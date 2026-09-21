package com.cibertec.bbq.services;

import com.cibertec.bbq.exceptions.ForbiddenException;
import com.cibertec.bbq.models.*;
import com.cibertec.bbq.models.DTO.*;
import com.cibertec.bbq.repositories.FacilityRepository;
import com.cibertec.bbq.repositories.LocationRepository;
import com.cibertec.bbq.repositories.ProductRepository;
import com.cibertec.bbq.repositories.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/** Portal público del cliente (PortalController del sistema Laravel). Sin login. */
@Service
@RequiredArgsConstructor
public class PortalService {

    private final FacilityRepository facilityRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final AvailabilityService availability;
    private final BookingService bookingService;

    @Transactional(readOnly = true)
    public PortalFacilityDTO facility(String uuid) {
        Facility f = facilityRepository.findActiveByUuid(uuid)
            .orElseThrow(() -> new NoSuchElementException("Instalación no encontrada."));
        ensureCompanyActive(f.getCompany());
        List<PortalLocationDTO> locations = locationRepository.findAllByFacilityIdAndDeletedAtIsNullOrderByIdAsc(f.getId())
            .stream().map(l -> toLocation(l, false)).toList();
        return new PortalFacilityDTO(f.getUuid(), f.getName(), f.getDescription(), f.getAddress(), f.getPhone(),
            f.getEmail(), f.getCompany().getName(), f.getCompany().getUrl(), locations);
    }

    @Transactional(readOnly = true)
    public PortalLocationDTO location(String uuid) {
        return toLocation(activeLocation(uuid), true);
    }

    @Transactional(readOnly = true)
    public CalendarDaysDTO calendarDays(String uuid, LocalDate start, LocalDate end) {
        return availability.calendarDays(activeLocation(uuid), start, end);
    }

    @Transactional(readOnly = true)
    public AvailabilityDTO availability(String uuid, LocalDate date) {
        return availability.check(activeLocation(uuid), date);
    }

    /** Valida y cotiza (equivale a addToCart); el carrito vive en sessionStorage del navegador. */
    @Transactional(readOnly = true)
    public BookingQuoteDTO quote(String uuid, BookingRequestDTO dto) {
        return bookingService.quote(activeLocation(uuid), dto);
    }

    /** Crea la reserva con venta PENDIENTE (pago por transferencia). READ_COMMITTED: ver BookingService.create. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponseDTO book(String uuid, BookingRequestDTO dto) {
        Location l = activeLocation(uuid);
        return bookingService.toResponse(bookingService.create(l.getId(), dto, null), true);
    }

    @Transactional(readOnly = true)
    public BookingResponseDTO booking(String bookingUuid) {
        Sale s = saleRepository.findByBookingUuid(bookingUuid)
            .orElseThrow(() -> new NoSuchElementException("Reserva no encontrada."));
        return bookingService.toResponse(s, true);
    }

    private Location activeLocation(String uuid) {
        Location l = locationRepository.findActiveByUuid(uuid)
            .orElseThrow(() -> new NoSuchElementException("Ubicación no encontrada."));
        ensureCompanyActive(l.getFacility().getCompany());
        return l;
    }

    /** RN-10 */
    private static void ensureCompanyActive(Company c) {
        if (c.getDeletedAt() != null || !Objects.equals(c.getStatus(), AppConstants.COMPANY_ACTIVE)) {
            throw new ForbiddenException("Este establecimiento no está recibiendo reservas por el momento.");
        }
    }

    private PortalLocationDTO toLocation(Location l, boolean withDetail) {
        Facility f = l.getFacility();
        List<ProductResponseDTO> products = !withDetail ? null
            : productRepository.findAllByFacilityIdAndDeletedAtIsNullOrderByNameAsc(f.getId())
                .stream().map(ProductService::toResponse).toList();
        return new PortalLocationDTO(l.getUuid(), l.getName(), l.getDescription(), l.getMaxPerson(), l.getPrice(),
            f.getUuid(), f.getName(), f.getAddress(), f.getPhone(), f.getCompany().getName(), products,
            withDetail ? availability.minDate() : null, withDetail ? availability.maxDate() : null);
    }
}
