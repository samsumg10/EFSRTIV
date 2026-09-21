package com.cibertec.bbq.repositories;

import com.cibertec.bbq.models.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    List<Facility> findAllByCompanyIdAndDeletedAtIsNullOrderByIdDesc(Long companyId);
    List<Facility> findAllByIdInAndDeletedAtIsNullOrderByIdDesc(Collection<Long> ids);

    @Query("SELECT f FROM Facility f JOIN FETCH f.company WHERE f.id = :id AND f.deletedAt IS NULL")
    Optional<Facility> findActiveById(Long id);

    @Query("SELECT f FROM Facility f JOIN FETCH f.company WHERE f.uuid = :uuid AND f.deletedAt IS NULL")
    Optional<Facility> findActiveByUuid(String uuid);
}
