package com.cibertec.bbq.repositories;

import com.cibertec.bbq.models.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {

    /** Incluye productos ya eliminados: el detalle conserva su nombre. */
    @Query("SELECT d FROM BookingDetail d LEFT JOIN FETCH d.product " +
           "WHERE d.booking.id = :bookingId AND d.deletedAt IS NULL ORDER BY d.id")
    List<BookingDetail> findAllByBookingId(Long bookingId);
}
