package com.cibertec.bbq.controllers.company;

import com.cibertec.bbq.models.DTO.FacilityRequestDTO;
import com.cibertec.bbq.models.DTO.FacilityResponseDTO;
import com.cibertec.bbq.services.FacilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;

    @GetMapping
    public ResponseEntity<List<FacilityResponseDTO>> findAll() {
        return ResponseEntity.ok(facilityService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacilityResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(facilityService.findById(id));
    }

    @PostMapping
    public ResponseEntity<FacilityResponseDTO> create(@RequestBody @Valid FacilityRequestDTO dto) {
        return ResponseEntity.status(201).body(facilityService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacilityResponseDTO> update(@PathVariable Long id, @RequestBody @Valid FacilityRequestDTO dto) {
        return ResponseEntity.ok(facilityService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        facilityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
