package com.cibertec.bbq.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sales")
public class Sale extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "location_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal locationPrice;

    @Column(name = "product_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal productTotal;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    /** 1 pendiente, 2 pagado, 3 cancelado, 4 expirado (AppConstants.SALE_*) */
    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_deadline_date")
    private LocalDate paymentDeadlineDate;
}
