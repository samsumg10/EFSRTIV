package com.cibertec.bbq.services;

import com.cibertec.bbq.exceptions.BusinessException;
import com.cibertec.bbq.models.AppConstants;
import com.cibertec.bbq.models.ClosedDay;
import com.cibertec.bbq.models.DTO.AvailabilityDTO;
import com.cibertec.bbq.models.DTO.CalendarDaysDTO;
import com.cibertec.bbq.models.Location;
import com.cibertec.bbq.repositories.BookingRepository;
import com.cibertec.bbq.repositories.ClosedDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Disponibilidad de una ubicación (una sola implementación para empresa y portal).
 * Sustituye a PlanHelper, DaysFullyBookedHelper y MaxPeopleHelper del sistema Laravel.
 * Una reserva ocupa el día completo; "activa" = no eliminada y venta pendiente o pagada.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_RANGE_DAYS = 100;

    private final ClosedDayRepository closedDayRepository;
    private final BookingRepository bookingRepository;

    // ---- RN-01: ventana de reserva ----

    public LocalDate minDate() { return LocalDate.now().plusDays(AppConstants.BOOKING_MIN_DAYS_AHEAD); }

    public LocalDate maxDate() { return LocalDate.now().plusDays(AppConstants.BOOKING_MAX_DAYS_AHEAD); }

    public boolean isInWindow(LocalDate d) {
        return !d.isBefore(minDate()) && !d.isAfter(maxDate());
    }

    public String windowMessage(LocalDate d) {
        if (d.isBefore(minDate())) {
            return "Las reservas se hacen con al menos " + AppConstants.BOOKING_MIN_DAYS_AHEAD
                + " días de anticipación (desde el " + minDate().format(DMY) + ").";
        }
        return "Solo se puede reservar hasta el " + maxDate().format(DMY) + ".";
    }

    // ---- RN-02: días cerrados ----

    public boolean isClosed(Location l, LocalDate d) {
        return closedDayRepository.existsOverlap(l.getId(), d, d, null);
    }

    public List<LocalDate> closedDates(Location l, LocalDate from, LocalDate to) {
        TreeSet<LocalDate> dates = new TreeSet<>();
        for (ClosedDay cd : closedDayRepository.findInRange(l.getId(), from, to)) {
            LocalDate end = cd.getEndDate() != null ? cd.getEndDate() : cd.getStartDate();
            LocalDate d = cd.getStartDate().isBefore(from) ? from : cd.getStartDate();
            LocalDate last = end.isAfter(to) ? to : end;
            for (; !d.isAfter(last); d = d.plusDays(1)) dates.add(d);
        }
        return new ArrayList<>(dates);
    }

    // ---- RN-03 / RN-04: capacidad ----

    /** [personas, reservas] activas del día. */
    public long[] occupancy(Location l, LocalDate d) {
        Object[] row = bookingRepository.occupancy(l.getId(), d, AppConstants.ACTIVE_SALE_STATUSES).get(0);
        return new long[] { ((Number) row[0]).longValue(), ((Number) row[1]).longValue() };
    }

    public boolean isFull(Location l, long persons, long bookings) {
        return l.getMaxPerson() == null ? bookings > 0 : persons >= l.getMaxPerson();
    }

    public List<LocalDate> fullDates(Location l, LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        for (Object[] row : bookingRepository.occupancyByDate(l.getId(), from, to, AppConstants.ACTIVE_SALE_STATUSES)) {
            if (isFull(l, ((Number) row[1]).longValue(), ((Number) row[2]).longValue())) {
                dates.add((LocalDate) row[0]);
            }
        }
        dates.sort(null);
        return dates;
    }

    /** Resumen de un día para mostrar en pantalla (no lanza excepciones). */
    public AvailabilityDTO check(Location l, LocalDate d) {
        boolean exclusive = l.getMaxPerson() == null;
        long[] occ = occupancy(l, d);
        Integer remaining = exclusive ? null : (int) Math.max(0, l.getMaxPerson() - occ[0]);
        String message = null;
        if (!isInWindow(d)) message = windowMessage(d);
        else if (isClosed(l, d)) message = "La ubicación está cerrada ese día.";
        else if (isFull(l, occ[0], occ[1])) message = exclusive ? "Ese día ya está reservado." : "Ese día ya no tiene cupos.";
        return new AvailabilityDTO(d, message == null, message, exclusive, l.getMaxPerson(), occ[0], remaining);
    }

    /** Días visibles del calendario: [start, end) como los envía FullCalendar. */
    public CalendarDaysDTO calendarDays(Location l, LocalDate start, LocalDate end) {
        if (!end.isAfter(start) || ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            throw new BusinessException("Rango de fechas no válido.");
        }
        LocalDate to = end.minusDays(1);
        return new CalendarDaysDTO(minDate(), maxDate(), closedDates(l, start, to), fullDates(l, start, to));
    }
}
