package com.cibertec.bbq.services;

import com.cibertec.bbq.exceptions.BusinessException;
import com.cibertec.bbq.models.*;
import com.cibertec.bbq.models.DTO.BookingLineDTO;
import com.cibertec.bbq.models.DTO.BookingQuoteDTO;
import com.cibertec.bbq.models.DTO.BookingRequestDTO;
import com.cibertec.bbq.models.DTO.BookingResponseDTO;
import com.cibertec.bbq.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Motor de reservas compartido por el panel de empresa y el portal:
 * valida (RN-01…03, RN-07), calcula precios en el servidor (RN-06) y guarda (RN-05, RN-08).
 * En el sistema Laravel esta lógica estaba duplicada en BookingController y PortalController.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final AvailabilityService availability;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final SaleRepository saleRepository;

    /** Valida y cotiza sin guardar (equivale a addToCart). */
    @Transactional(readOnly = true)
    public BookingQuoteDTO quote(Location l, BookingRequestDTO dto) {
        LocalDate d = dto.getDate();
        if (!availability.isInWindow(d)) throw new BusinessException("date", availability.windowMessage(d));
        if (availability.isClosed(l, d)) throw new BusinessException("date", "La ubicación está cerrada ese día.");

        long[] occ = availability.occupancy(l, d);
        if (l.getMaxPerson() == null) {
            if (occ[1] > 0) throw new BusinessException("date", "Ese día ya está reservado.");
        } else {
            long remaining = l.getMaxPerson() - occ[0];
            if (remaining <= 0) throw new BusinessException("date", "Ese día ya no tiene cupos.");
            if (dto.getNumberPersons() > remaining) {
                throw new BusinessException("numberPersons", "Solo quedan " + remaining + " lugar(es) para ese día.");
            }
        }

        // Productos con cantidad > 0 (se suman si vienen repetidos)
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        if (dto.getProducts() != null) {
            for (BookingRequestDTO.ProductQuantityDTO p : dto.getProducts()) {
                if (p.getQuantity() != null && p.getQuantity() > 0) quantities.merge(p.getProductId(), p.getQuantity(), Integer::sum);
            }
        }
        Map<Long, Product> products = new HashMap<>();
        if (!quantities.isEmpty()) {
            for (Product p : productRepository.findAllByIdInAndFacilityIdAndDeletedAtIsNull(quantities.keySet(), l.getFacility().getId())) {
                products.put(p.getId(), p);
            }
            if (products.size() != quantities.size()) {
                throw new BusinessException("products", "Algún producto ya no está disponible. Actualiza la página.");
            }
        }

        List<BookingLineDTO> lines = new ArrayList<>();
        BigDecimal productTotal = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : quantities.entrySet()) {
            Product p = products.get(e.getKey());
            BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(e.getValue())).setScale(2, RoundingMode.HALF_UP);
            lines.add(new BookingLineDTO(p.getId(), p.getName(), p.getPrice(), e.getValue(), lineTotal));
            productTotal = productTotal.add(lineTotal);
        }
        BigDecimal locationPrice = l.getPrice().setScale(2, RoundingMode.HALF_UP);
        productTotal = productTotal.setScale(2, RoundingMode.HALF_UP);

        return new BookingQuoteDTO(l.getId(), l.getUuid(), l.getName(), l.getFacility().getName(), d,
            dto.getNumberPersons(), dto.getName().trim(), dto.getEmail().trim().toLowerCase(), dto.getPhone().trim(),
            locationPrice, lines, productTotal, locationPrice.add(productTotal));
    }

    /**
     * Guarda la reserva, sus productos y la venta.
     * createdBy != null → reserva manual de la empresa (venta PAGADA); null → portal (venta PENDIENTE).
     * Si se llama dentro de otra transacción, esa también debe ser READ_COMMITTED
     * (al unirse a una transacción existente se usa el aislamiento de la externa).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Sale create(Long locationId, BookingRequestDTO dto, Employee createdBy) {
        // RN-05: bloquea la ubicación hasta el commit para que dos reservas no tomen el último cupo.
        // Con REPEATABLE READ (default de MySQL) la suma de ocupación leería la foto tomada antes
        // del bloqueo y no vería la reserva que otra transacción acaba de confirmar; por eso READ_COMMITTED.
        Location l = locationRepository.lockById(locationId)
            .orElseThrow(() -> new NoSuchElementException("Ubicación no encontrada."));
        BookingQuoteDTO q = quote(l, dto);

        Booking b = new Booking();
        b.setCompany(l.getFacility().getCompany());
        b.setLocation(l);
        b.setName(q.getName());
        b.setEmail(q.getEmail());
        b.setPhone(q.getPhone());
        b.setDate(q.getDate());
        b.setNumberPersons(q.getNumberPersons());
        b.setEmployee(createdBy);
        b.setUuid(UUID.randomUUID().toString());
        bookingRepository.save(b);

        for (BookingLineDTO line : q.getLines()) {
            BookingDetail d = new BookingDetail();
            d.setBooking(b);
            d.setProduct(productRepository.getReferenceById(line.getProductId()));
            d.setPrice(line.getPrice());
            d.setQuantity(line.getQuantity());
            d.setTotal(line.getTotal());
            bookingDetailRepository.save(d);
        }

        Sale s = new Sale();
        s.setBooking(b);
        s.setLocationPrice(q.getLocationPrice());
        s.setProductTotal(q.getProductTotal());
        s.setTotal(q.getTotal());
        if (createdBy != null) {
            s.setStatus(AppConstants.SALE_PAID);
            s.setPaidAt(LocalDateTime.now());
        } else {
            s.setStatus(AppConstants.SALE_PENDING);
            LocalDate deadline = LocalDate.now().plusDays(AppConstants.PAYMENT_DEADLINE_DAYS);
            LocalDate dayBefore = q.getDate().minusDays(1);
            s.setPaymentDeadlineDate(deadline.isAfter(dayBefore) ? dayBefore : deadline);
        }
        return saleRepository.save(s);
    }

    /** RN-09: pendiente → pagado; pendiente/pagado → cancelado. */
    @Transactional
    public void changeStatus(Sale s, Integer newStatus) {
        int current = s.getStatus();
        if (Objects.equals(newStatus, AppConstants.SALE_PAID)) {
            if (current != AppConstants.SALE_PENDING) {
                throw new BusinessException("Solo se puede marcar como pagada una reserva pendiente.");
            }
            s.setStatus(AppConstants.SALE_PAID);
            s.setPaidAt(LocalDateTime.now());
        } else if (Objects.equals(newStatus, AppConstants.SALE_CANCELLED)) {
            if (current != AppConstants.SALE_PENDING && current != AppConstants.SALE_PAID) {
                throw new BusinessException("Esta reserva ya está " + (current == AppConstants.SALE_CANCELLED ? "cancelada." : "expirada."));
            }
            s.setStatus(AppConstants.SALE_CANCELLED);
        } else {
            throw new BusinessException("status", "Estado no válido.");
        }
        saleRepository.save(s);
    }

    /** Detalle de la reserva (debe llamarse dentro de una transacción). */
    public BookingResponseDTO toResponse(Sale s, boolean withLines) {
        Booking b = s.getBooking();
        Location l = b.getLocation();
        Facility f = l.getFacility();
        List<BookingLineDTO> lines = !withLines ? List.of() : bookingDetailRepository.findAllByBookingId(b.getId()).stream()
            .map(d -> new BookingLineDTO(d.getProduct() != null ? d.getProduct().getId() : null,
                d.getProduct() != null ? d.getProduct().getName() : "Producto eliminado",
                d.getPrice(), d.getQuantity(), d.getTotal()))
            .toList();
        return new BookingResponseDTO(b.getId(), b.getUuid(), b.getName(), b.getEmail(), b.getPhone(), b.getDate(),
            b.getNumberPersons(), l.getId(), l.getName(), f.getId(), f.getName(), f.getCompany().getName(),
            b.getEmployee() == null ? "Portal" : b.getEmployee().getName(), b.getCreatedAt(),
            s.getStatus(), s.getLocationPrice(), s.getProductTotal(), s.getTotal(), s.getPaidAt(),
            s.getPaymentDeadlineDate(), lines);
    }
}
