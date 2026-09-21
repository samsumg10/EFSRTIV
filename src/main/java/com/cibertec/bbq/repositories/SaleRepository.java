package com.cibertec.bbq.repositories;

import com.cibertec.bbq.models.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Las reservas se consultan desde la venta (1:1) para traer ambas en una sola consulta. */
@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("SELECT s FROM Sale s JOIN FETCH s.booking b JOIN FETCH b.location l JOIN FETCH l.facility f " +
           "WHERE b.id = :bookingId AND b.deletedAt IS NULL")
    Optional<Sale> findByBookingId(Long bookingId);

    @Query("SELECT s FROM Sale s JOIN FETCH s.booking b JOIN FETCH b.location l JOIN FETCH l.facility f " +
           "JOIN FETCH f.company WHERE b.uuid = :uuid AND b.deletedAt IS NULL")
    Optional<Sale> findByBookingUuid(String uuid);

    /** Eventos del calendario: fechas en [start, end). */
    @Query("SELECT s FROM Sale s JOIN FETCH s.booking b WHERE b.location.id = :locationId " +
           "AND b.deletedAt IS NULL AND b.date >= :start AND b.date < :end ORDER BY b.date, b.id")
    List<Sale> findForCalendar(Long locationId, LocalDate start, LocalDate end);

    // ---- Lista de reservas por instalación (Parte 8) ----

    String SEARCH_WHERE =
        " WHERE l.facility.id = :facilityId AND b.deletedAt IS NULL" +
        " AND (:resStart IS NULL OR b.date >= :resStart)" +
        " AND (:resEnd IS NULL OR b.date <= :resEnd)" +
        " AND (:regStart IS NULL OR b.createdAt >= :regStart)" +
        " AND (:regEnd IS NULL OR b.createdAt < :regEnd)" +
        " AND (:uuid IS NULL OR b.uuid = :uuid)" +
        " AND (:status IS NULL OR s.status = :status)" +
        " AND (:locationId IS NULL OR l.id = :locationId)";

    @Query(value = "SELECT s FROM Sale s JOIN FETCH s.booking b JOIN FETCH b.location l" + SEARCH_WHERE +
                   " ORDER BY b.date ASC, b.id ASC",
           countQuery = "SELECT COUNT(s) FROM Sale s JOIN s.booking b JOIN b.location l" + SEARCH_WHERE)
    Page<Sale> search(Long facilityId, LocalDate resStart, LocalDate resEnd,
                      LocalDateTime regStart, LocalDateTime regEnd, String uuid,
                      Integer status, Long locationId, Pageable pageable);

    @Query("SELECT COUNT(s) AS totalBookings, COALESCE(SUM(s.total), 0) AS totalAmount," +
           " COALESCE(SUM(CASE WHEN s.status = 2 THEN 1 ELSE 0 END), 0) AS paidCount," +
           " COALESCE(SUM(CASE WHEN s.status = 2 THEN s.total ELSE 0 END), 0) AS paidAmount," +
           " COALESCE(SUM(CASE WHEN s.status = 1 THEN 1 ELSE 0 END), 0) AS pendingCount," +
           " COALESCE(SUM(CASE WHEN s.status = 1 THEN s.total ELSE 0 END), 0) AS pendingAmount" +
           " FROM Sale s JOIN s.booking b JOIN b.location l" + SEARCH_WHERE)
    StatsView stats(Long facilityId, LocalDate resStart, LocalDate resEnd,
                    LocalDateTime regStart, LocalDateTime regEnd, String uuid,
                    Integer status, Long locationId);

    interface StatsView {
        Number getTotalBookings();
        Number getTotalAmount();
        Number getPaidCount();
        Number getPaidAmount();
        Number getPendingCount();
        Number getPendingAmount();
    }

    /** RN-09: ventas pendientes con el plazo vencido pasan a EXPIRADO. */
    @Modifying
    @Query("UPDATE Sale s SET s.status = 4, s.updatedAt = :now " +
           "WHERE s.status = 1 AND s.paymentDeadlineDate < :today AND s.deletedAt IS NULL")
    int expireOverdue(LocalDate today, LocalDateTime now);
}
