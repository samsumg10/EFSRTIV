package com.cibertec.bbq.repositories;

import com.cibertec.bbq.models.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** RN-03: personas y cantidad de reservas activas de un día. Fila: [personas, reservas]. */
    @Query("SELECT COALESCE(SUM(b.numberPersons), 0), COUNT(b) FROM Sale s JOIN s.booking b " +
           "WHERE b.location.id = :locationId AND b.date = :date AND b.deletedAt IS NULL " +
           "AND s.status IN :statuses")
    List<Object[]> occupancy(Long locationId, LocalDate date, Collection<Integer> statuses);

    /** RN-04: ocupación agrupada por día. Fila: [fecha, personas, reservas]. */
    @Query("SELECT b.date, SUM(b.numberPersons), COUNT(b) FROM Sale s JOIN s.booking b " +
           "WHERE b.location.id = :locationId AND b.date BETWEEN :from AND :to AND b.deletedAt IS NULL " +
           "AND s.status IN :statuses GROUP BY b.date")
    List<Object[]> occupancyByDate(Long locationId, LocalDate from, LocalDate to, Collection<Integer> statuses);
}
