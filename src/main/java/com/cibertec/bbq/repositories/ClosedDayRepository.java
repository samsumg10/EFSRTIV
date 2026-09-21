package com.cibertec.bbq.repositories;

import com.cibertec.bbq.models.ClosedDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClosedDayRepository extends JpaRepository<ClosedDay, Long> {

    List<ClosedDay> findAllByLocationIdAndDeletedAtIsNullOrderByStartDateDesc(Long locationId);
    Optional<ClosedDay> findByIdAndLocationIdAndDeletedAtIsNull(Long id, Long locationId);

    /** RN-11: ¿algún día cerrado de la ubicación se cruza con [start, end]? */
    @Query("SELECT COUNT(c) > 0 FROM ClosedDay c WHERE c.location.id = :locationId AND c.deletedAt IS NULL " +
           "AND c.startDate <= :end AND COALESCE(c.endDate, c.startDate) >= :start " +
           "AND (:excludeId IS NULL OR c.id <> :excludeId)")
    boolean existsOverlap(Long locationId, LocalDate start, LocalDate end, Long excludeId);

    @Query("SELECT c FROM ClosedDay c WHERE c.location.id = :locationId AND c.deletedAt IS NULL " +
           "AND c.startDate <= :to AND COALESCE(c.endDate, c.startDate) >= :from")
    List<ClosedDay> findInRange(Long locationId, LocalDate from, LocalDate to);
}
