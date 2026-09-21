package com.cibertec.bbq.repositories;

import com.cibertec.bbq.models.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findAllByDeletedAtIsNullOrderByIdDesc();
    Optional<Company> findByIdAndDeletedAtIsNull(Long id);
    /** Incluye eliminadas: el UNIQUE de la BD también las cuenta. */
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
}
