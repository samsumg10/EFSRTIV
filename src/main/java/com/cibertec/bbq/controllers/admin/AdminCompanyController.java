package com.cibertec.bbq.controllers.admin;

import com.cibertec.bbq.models.DTO.CompanyRequestDTO;
import com.cibertec.bbq.models.DTO.CompanyResponseDTO;
import com.cibertec.bbq.services.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/companies")
@RequiredArgsConstructor
public class AdminCompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<List<CompanyResponseDTO>> findAll() {
        return ResponseEntity.ok(companyService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CompanyResponseDTO> create(@RequestBody @Valid CompanyRequestDTO dto) {
        return ResponseEntity.status(201).body(companyService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponseDTO> update(@PathVariable Long id, @RequestBody @Valid CompanyRequestDTO dto) {
        return ResponseEntity.ok(companyService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
