package com.cibertec.bbq.controllers.company;

import com.cibertec.bbq.models.DTO.EmployeeRequestDTO;
import com.cibertec.bbq.models.DTO.EmployeeResponseDTO;
import com.cibertec.bbq.models.DTO.RoleResponseDTO;
import com.cibertec.bbq.services.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class EmployeeController {

    private final EmployeeService employeeService;

    /** Roles disponibles para el select del formulario de empleados. */
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponseDTO>> roles() {
        return ResponseEntity.ok(employeeService.roles());
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeResponseDTO>> findAll() {
        return ResponseEntity.ok(employeeService.findAll());
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    @PostMapping("/employees")
    public ResponseEntity<EmployeeResponseDTO> create(@RequestBody @Valid EmployeeRequestDTO dto) {
        return ResponseEntity.status(201).body(employeeService.create(dto));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponseDTO> update(@PathVariable Long id, @RequestBody @Valid EmployeeRequestDTO dto) {
        return ResponseEntity.ok(employeeService.update(id, dto));
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
