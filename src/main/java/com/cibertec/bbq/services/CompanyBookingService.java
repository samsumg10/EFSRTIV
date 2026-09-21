package com.cibertec.bbq.services;

import com.cibertec.bbq.exceptions.BusinessException;
import com.cibertec.bbq.models.*;
import com.cibertec.bbq.models.DTO.*;
import com.cibertec.bbq.repositories.ProductRepository;
import com.cibertec.bbq.repositories.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Reservas en el panel de empresa: calendario por ubicación y lista por instalación. */
@Service
@RequiredArgsConstructor
public class CompanyBookingService {

    private static final int PAGE_SIZE = 10;

    private final AccessService access;
    private final AvailabilityService availability;
    private final BookingService bookingService;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;

    @Transactional(readOnly = true)
    public CalendarInfoDTO calendarInfo(Long locationId) {
        Location l = access.location(locationId);
        List<ProductResponseDTO> products = productRepository
            .findAllByFacilityIdAndDeletedAtIsNullOrderByNameAsc(l.getFacility().getId())
            .stream().map(ProductService::toResponse).toList();
        return new CalendarInfoDTO(LocationService.toResponse(l), products, availability.minDate(), availability.maxDate());
    }

    @Transactional(readOnly = true)
    public CalendarDaysDTO calendarDays(Long locationId, LocalDate start, LocalDate end) {
        return availability.calendarDays(access.location(locationId), start, end);
    }

    @Transactional(readOnly = true)
    public AvailabilityDTO availability(Long locationId, LocalDate date) {
        return availability.check(access.location(locationId), date);
    }

    /** Eventos de FullCalendar en [start, end). */
    @Transactional(readOnly = true)
    public List<CalendarEventDTO> events(Long locationId, LocalDate start, LocalDate end) {
        Location l = access.location(locationId);
        if (!end.isAfter(start) || ChronoUnit.DAYS.between(start, end) > 100) {
            throw new BusinessException("Rango de fechas no válido.");
        }
        return saleRepository.findForCalendar(l.getId(), start, end).stream().map(s -> {
            Booking b = s.getBooking();
            return new CalendarEventDTO(b.getId(), b.getName() + " (" + b.getNumberPersons() + " pers.)",
                b.getDate(), true, s.getStatus(), b.getNumberPersons());
        }).toList();
    }

    /** Reserva manual (RN-08: queda PAGADA y registra al empleado). READ_COMMITTED: ver BookingService.create. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponseDTO create(Long locationId, BookingRequestDTO dto) {
        Employee me = access.currentEmployee();
        access.location(locationId);
        return bookingService.toResponse(bookingService.create(locationId, dto, me), true);
    }

    @Transactional(readOnly = true)
    public BookingResponseDTO detail(Long bookingId) {
        return bookingService.toResponse(access.booking(bookingId), true);
    }

    @Transactional
    public BookingResponseDTO changeStatus(Long bookingId, BookingStatusDTO dto) {
        Sale s = access.booking(bookingId);
        bookingService.changeStatus(s, dto.getStatus());
        return bookingService.toResponse(s, true);
    }

    /** Lista filtrada + estadísticas (BookingController@getBookingsByFacility). */
    @Transactional(readOnly = true)
    public BookingListDTO list(Long facilityId, LocalDate resStart, LocalDate resEnd, LocalDate regStart,
                               LocalDate regEnd, String uuid, Integer status, Long locationId, int page) {
        Facility f = access.facility(facilityId);
        LocalDateTime regFrom = regStart != null ? regStart.atStartOfDay() : null;
        LocalDateTime regTo = regEnd != null ? regEnd.plusDays(1).atStartOfDay() : null;
        String code = uuid == null || uuid.isBlank() ? null : uuid.trim();

        Page<Sale> result = saleRepository.search(f.getId(), resStart, resEnd, regFrom, regTo, code, status,
            locationId, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
        SaleRepository.StatsView st = saleRepository.stats(f.getId(), resStart, resEnd, regFrom, regTo, code, status, locationId);

        BookingListDTO.Stats stats = new BookingListDTO.Stats(
            toLong(st.getTotalBookings()), toMoney(st.getTotalAmount()),
            toLong(st.getPaidCount()), toMoney(st.getPaidAmount()),
            toLong(st.getPendingCount()), toMoney(st.getPendingAmount()));
        List<BookingResponseDTO> items = result.getContent().stream()
            .map(s -> bookingService.toResponse(s, false)).toList();
        return new BookingListDTO(items, result.getNumber(), result.getTotalPages(), result.getTotalElements(), stats);
    }

    private static long toLong(Number n) { return n == null ? 0 : n.longValue(); }

    private static BigDecimal toMoney(Number n) {
        return n == null ? BigDecimal.ZERO : new BigDecimal(n.toString()).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
