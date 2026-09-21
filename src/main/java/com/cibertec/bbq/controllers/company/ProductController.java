package com.cibertec.bbq.controllers.company;

import com.cibertec.bbq.models.DTO.ProductRequestDTO;
import com.cibertec.bbq.models.DTO.ProductResponseDTO;
import com.cibertec.bbq.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company/facilities/{facilityId}/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> findAll(@PathVariable Long facilityId) {
        return ResponseEntity.ok(productService.findAll(facilityId));
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> create(@PathVariable Long facilityId,
                                                     @RequestBody @Valid ProductRequestDTO dto) {
        return ResponseEntity.status(201).body(productService.create(facilityId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> update(@PathVariable Long facilityId, @PathVariable Long id,
                                                     @RequestBody @Valid ProductRequestDTO dto) {
        return ResponseEntity.ok(productService.update(facilityId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long facilityId, @PathVariable Long id) {
        productService.delete(facilityId, id);
        return ResponseEntity.noContent().build();
    }
}
