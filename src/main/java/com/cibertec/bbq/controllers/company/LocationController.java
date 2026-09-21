package com.cibertec.bbq.controllers.company;

import com.cibertec.bbq.models.DTO.LocationRequestDTO;
import com.cibertec.bbq.models.DTO.LocationResponseDTO;
import com.cibertec.bbq.services.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/facilities/{facilityId}/locations")
    public ResponseEntity<List<LocationResponseDTO>> findAll(@PathVariable Long facilityId) {
        return ResponseEntity.ok(locationService.findAll(facilityId));
    }

    /** Datos de una ubicación (cabecera de las páginas de reservas y días cerrados). */
    @GetMapping("/locations/{id}")
    public ResponseEntity<LocationResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.findById(id));
    }

    @PostMapping("/facilities/{facilityId}/locations")
    public ResponseEntity<LocationResponseDTO> create(@PathVariable Long facilityId,
                                                      @RequestBody @Valid LocationRequestDTO dto) {
        return ResponseEntity.status(201).body(locationService.create(facilityId, dto));
    }

    @PutMapping("/facilities/{facilityId}/locations/{id}")
    public ResponseEntity<LocationResponseDTO> update(@PathVariable Long facilityId, @PathVariable Long id,
                                                      @RequestBody @Valid LocationRequestDTO dto) {
        return ResponseEntity.ok(locationService.update(facilityId, id, dto));
    }

    @DeleteMapping("/facilities/{facilityId}/locations/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long facilityId, @PathVariable Long id) {
        locationService.delete(facilityId, id);
        return ResponseEntity.noContent().build();
    }
}
