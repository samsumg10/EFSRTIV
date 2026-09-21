package com.cibertec.bbq.controllers.company;

import com.cibertec.bbq.models.DTO.ClosedDayRequestDTO;
import com.cibertec.bbq.models.DTO.ClosedDayResponseDTO;
import com.cibertec.bbq.services.ClosedDayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company/locations/{locationId}/closeddays")
@RequiredArgsConstructor
public class ClosedDayController {

    private final ClosedDayService closedDayService;

    @GetMapping
    public ResponseEntity<List<ClosedDayResponseDTO>> findAll(@PathVariable Long locationId) {
        return ResponseEntity.ok(closedDayService.findAll(locationId));
    }

    @PostMapping
    public ResponseEntity<ClosedDayResponseDTO> create(@PathVariable Long locationId,
                                                       @RequestBody @Valid ClosedDayRequestDTO dto) {
        return ResponseEntity.status(201).body(closedDayService.create(locationId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClosedDayResponseDTO> update(@PathVariable Long locationId, @PathVariable Long id,
                                                       @RequestBody @Valid ClosedDayRequestDTO dto) {
        return ResponseEntity.ok(closedDayService.update(locationId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long locationId, @PathVariable Long id) {
        closedDayService.delete(locationId, id);
        return ResponseEntity.noContent().build();
    }
}
