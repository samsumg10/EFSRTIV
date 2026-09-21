package com.cibertec.bbq.repositories;

import com.cibertec.bbq.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByFacilityIdAndDeletedAtIsNullOrderByIdDesc(Long facilityId);
    List<Product> findAllByFacilityIdAndDeletedAtIsNullOrderByNameAsc(Long facilityId);
    Optional<Product> findByIdAndFacilityIdAndDeletedAtIsNull(Long id, Long facilityId);
    List<Product> findAllByIdInAndFacilityIdAndDeletedAtIsNull(Collection<Long> ids, Long facilityId);
}
