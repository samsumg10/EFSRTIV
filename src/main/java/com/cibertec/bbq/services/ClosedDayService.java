package com.cibertec.bbq.services;

import com.cibertec.bbq.exceptions.BusinessException;
import com.cibertec.bbq.models.AppConstants;
import com.cibertec.bbq.models.ClosedDay;
import com.cibertec.bbq.models.DTO.ClosedDayRequestDTO;
import com.cibertec.bbq.models.DTO.ClosedDayResponseDTO;
import com.cibertec.bbq.models.Location;
import com.cibertec.bbq.repositories.BookingRepository;
import com.cibertec.bbq.repositories.ClosedDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/** /company/locations/{id}/closeddays (ClosedDayController del sistema Laravel). */
@Service
@RequiredArgsConstructor
public class ClosedDayService {

    private final AccessService access;
    private final ClosedDayRepository closedDayRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<ClosedDayResponseDTO> findAll(Long locationId) {
        Location l = access.location(locationId);
        return closedDayRepository.findAllByLocationIdAndDeletedAtIsNullOrderByStartDateDesc(l.getId())
            .stream().map(ClosedDayService::toResponse).toList();
    }

    @Transactional
    public ClosedDayResponseDTO create(Long locationId, ClosedDayRequestDTO dto) {
        Location l = access.location(locationId);
        ClosedDay cd = new ClosedDay();
        cd.setLocation(l);
        cd.setFacility(l.getFacility());
        apply(cd, l, dto, null);
        return toResponse(closedDayRepository.save(cd));
    }

    @Transactional
    public ClosedDayResponseDTO update(Long locationId, Long id, ClosedDayRequestDTO dto) {
        Location l = access.location(locationId);
        ClosedDay cd = getOrThrow(l, id);
        apply(cd, l, dto, id);
        return toResponse(closedDayRepository.save(cd));
    }

    @Transactional
    public void delete(Long locationId, Long id) {
        Location l = access.location(locationId);
        ClosedDay cd = getOrThrow(l, id);
        cd.markDeleted();
        closedDayRepository.save(cd);
    }

    /** RN-11 + RN-15 (no cerrar días que ya tienen reservas activas). */
    private void apply(ClosedDay cd, Location l, ClosedDayRequestDTO dto, Long excludeId) {
        LocalDate start = dto.getStartDate();
        LocalDate end = dto.getEndDate() != null ? dto.getEndDate() : start;
        if (end.isBefore(start)) {
            throw new BusinessException("endDate", "La fecha de fin no puede ser anterior a la de inicio.");
        }
        if (closedDayRepository.existsOverlap(l.getId(), start, end, excludeId)) {
            throw new BusinessException("startDate", start.equals(end)
                ? "Ese día ya está registrado como cerrado."
                : "Las fechas se cruzan con otro periodo cerrado.");
        }
        long activeBookings = bookingRepository.occupancyByDate(l.getId(), start, end, AppConstants.ACTIVE_SALE_STATUSES)
            .stream().mapToLong(row -> ((Number) row[2]).longValue()).sum();
        if (activeBookings > 0) {
            throw new BusinessException("startDate", "Hay " + activeBookings
                + " reserva(s) activa(s) en esas fechas. Cancélalas antes de cerrar la ubicación.");
        }
        cd.setStartDate(start);
        cd.setEndDate(end);
        cd.setReason(dto.getReason() == null || dto.getReason().isBlank() ? null : dto.getReason().trim());
    }

    private ClosedDay getOrThrow(Location l, Long id) {
        return closedDayRepository.findByIdAndLocationIdAndDeletedAtIsNull(id, l.getId())
            .orElseThrow(() -> new NoSuchElementException("Día cerrado no encontrado."));
    }

    private static ClosedDayResponseDTO toResponse(ClosedDay cd) {
        return new ClosedDayResponseDTO(cd.getId(), cd.getStartDate(), cd.getEndDate(), cd.getReason());
    }
}
