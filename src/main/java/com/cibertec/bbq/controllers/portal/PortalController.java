package com.cibertec.bbq.controllers.portal;

import com.cibertec.bbq.models.DTO.*;
import com.cibertec.bbq.services.PortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** API pública del portal del cliente (sin JWT). */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PortalController {

    private final PortalService portalService;

    @GetMapping("/facilities/{uuid}")
    public ResponseEntity<PortalFacilityDTO> facility(@PathVariable String uuid) {
        return ResponseEntity.ok(portalService.facility(uuid));
    }

    @GetMapping("/locations/{uuid}")
    public ResponseEntity<PortalLocationDTO> location(@PathVariable String uuid) {
        return ResponseEntity.ok(portalService.location(uuid));
    }

    @GetMapping("/locations/{uuid}/calendar-days")
    public ResponseEntity<CalendarDaysDTO> calendarDays(@PathVariable String uuid,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(portalService.calendarDays(uuid, start, end));
    }

    @GetMapping("/locations/{uuid}/availability")
    public ResponseEntity<AvailabilityDTO> availability(@PathVariable String uuid,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(portalService.availability(uuid, date));
    }

    @PostMapping("/locations/{uuid}/quote")
    public ResponseEntity<BookingQuoteDTO> quote(@PathVariable String uuid, @RequestBody @Valid BookingRequestDTO dto) {
        return ResponseEntity.ok(portalService.quote(uuid, dto));
    }

    @PostMapping("/locations/{uuid}/bookings")
    public ResponseEntity<BookingResponseDTO> book(@PathVariable String uuid, @RequestBody @Valid BookingRequestDTO dto) {
        return ResponseEntity.status(201).body(portalService.book(uuid, dto));
    }

    @GetMapping("/bookings/{uuid}")
    public ResponseEntity<BookingResponseDTO> booking(@PathVariable String uuid) {
        return ResponseEntity.ok(portalService.booking(uuid));
    }
}
