package com.cibertec.bbq.services;

import com.cibertec.bbq.models.DTO.ProductRequestDTO;
import com.cibertec.bbq.models.DTO.ProductResponseDTO;
import com.cibertec.bbq.models.Facility;
import com.cibertec.bbq.models.Product;
import com.cibertec.bbq.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/** /company/facilities/{id}/products. */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final AccessService access;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> findAll(Long facilityId) {
        Facility f = access.facility(facilityId);
        return productRepository.findAllByFacilityIdAndDeletedAtIsNullOrderByIdDesc(f.getId())
            .stream().map(ProductService::toResponse).toList();
    }

    @Transactional
    public ProductResponseDTO create(Long facilityId, ProductRequestDTO dto) {
        Product p = new Product();
        p.setFacility(access.facility(facilityId));
        apply(p, dto);
        return toResponse(productRepository.save(p));
    }

    @Transactional
    public ProductResponseDTO update(Long facilityId, Long id, ProductRequestDTO dto) {
        Product p = getOrThrow(facilityId, id);
        apply(p, dto);
        return toResponse(productRepository.save(p));
    }

    /** Las reservas antiguas conservan el precio y el nombre del producto. */
    @Transactional
    public void delete(Long facilityId, Long id) {
        Product p = getOrThrow(facilityId, id);
        p.markDeleted();
        productRepository.save(p);
    }

    private Product getOrThrow(Long facilityId, Long id) {
        access.facility(facilityId);
        return productRepository.findByIdAndFacilityIdAndDeletedAtIsNull(id, facilityId)
            .orElseThrow(() -> new NoSuchElementException("Producto no encontrado."));
    }

    private void apply(Product p, ProductRequestDTO dto) {
        p.setName(dto.getName().trim());
        p.setPrice(dto.getPrice());
    }

    public static ProductResponseDTO toResponse(Product p) {
        return new ProductResponseDTO(p.getId(), p.getFacility().getId(), p.getName(), p.getPrice());
    }
}
