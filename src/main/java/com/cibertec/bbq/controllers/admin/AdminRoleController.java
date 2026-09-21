package com.cibertec.bbq.controllers.admin;

import com.cibertec.bbq.models.DTO.RoleRequestDTO;
import com.cibertec.bbq.models.DTO.RoleResponseDTO;
import com.cibertec.bbq.services.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<List<RoleResponseDTO>> findAll() {
        return ResponseEntity.ok(roleService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RoleResponseDTO> create(@RequestBody @Valid RoleRequestDTO dto) {
        return ResponseEntity.status(201).body(roleService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponseDTO> update(@PathVariable Long id, @RequestBody @Valid RoleRequestDTO dto) {
        return ResponseEntity.ok(roleService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
