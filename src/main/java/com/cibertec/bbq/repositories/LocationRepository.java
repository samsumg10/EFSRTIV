package com.cibertec.bbq.repositories;

import com.cibertec.bbq.models.Location;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findAllByFacilityIdAndDeletedAtIsNullOrderByIdAsc(Long facilityId);
    long countByFacilityIdAndDeletedAtIsNull(Long facilityId);

    @Query("SELECT l FROM Location l JOIN FETCH l.facility f JOIN FETCH f.company " +
           "WHERE l.id = :id AND l.deletedAt IS NULL AND f.deletedAt IS NULL")
    Optional<Location> findActiveById(Long id);

    @Query("SELECT l FROM Location l JOIN FETCH l.facility f JOIN FETCH f.company " +
           "WHERE l.uuid = :uuid AND l.deletedAt IS NULL AND f.deletedAt IS NULL")
    Optional<Location> findActiveByUuid(String uuid);

    /** RN-05: bloquea la fila mientras se crea una reserva (SELECT ... FOR UPDATE). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Location l WHERE l.id = :id")
    Optional<Location> lockById(Long id);
}
