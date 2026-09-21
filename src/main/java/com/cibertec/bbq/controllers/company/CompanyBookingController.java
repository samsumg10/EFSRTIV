package com.cibertec.bbq.controllers.company;

import com.cibertec.bbq.models.DTO.*;
import com.cibertec.bbq.services.CompanyBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyBookingController {

    private final CompanyBookingService service;

    // ---- Calendario de la ubicación (/company/locations/{id}/bookings) ----

    @GetMapping("/locations/{locationId}/calendar-info")
    public ResponseEntity<CalendarInfoDTO> calendarInfo(@PathVariable Long locationId) {
        return ResponseEntity.ok(service.calendarInfo(locationId));
    }

    @GetMapping("/locations/{locationId}/calendar-days")
    public ResponseEntity<CalendarDaysDTO> calendarDays(@PathVariable Long locationId,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(service.calendarDays(locationId, start, end));
    }

    @GetMapping("/locations/{locationId}/availability")
    public ResponseEntity<AvailabilityDTO> availability(@PathVariable Long locationId,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.availability(locationId, date));
    }

    @GetMapping("/locations/{locationId}/bookings")
    public ResponseEntity<List<CalendarEventDTO>> events(@PathVariable Long locationId,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(service.events(locationId, start, end));
    }

    @PostMapping("/locations/{locationId}/bookings")
    public ResponseEntity<BookingResponseDTO> create(@PathVariable Long locationId,
                                                     @RequestBody @Valid BookingRequestDTO dto) {
        return ResponseEntity.status(201).body(service.create(locationId, dto));
    }

    // ---- Reserva individual ----

    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<BookingResponseDTO> detail(@PathVariable Long bookingId) {
        return ResponseEntity.ok(service.detail(bookingId));
    }

    @PatchMapping("/bookings/{bookingId}/status")
    public ResponseEntity<BookingResponseDTO> changeStatus(@PathVariable Long bookingId,
                                                           @RequestBody @Valid BookingStatusDTO dto) {
        return ResponseEntity.ok(service.changeStatus(bookingId, dto));
    }

    // ---- Lista por instalación (/company/facilities/{id}/booking/getBookingsByFacility) ----

    @GetMapping("/facilities/{facilityId}/bookings")
    public ResponseEntity<BookingListDTO> list(@PathVariable Long facilityId,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate resStart,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate resEnd,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate regStart,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate regEnd,
            @RequestParam(required = false) String uuid,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long locationId,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(service.list(facilityId, resStart, resEnd, regStart, regEnd, uuid, status, locationId, page));
    }
}
