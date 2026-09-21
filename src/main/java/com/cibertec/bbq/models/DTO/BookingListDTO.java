package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Lista paginada de reservas de una instalación + estadísticas (getBookingsByFacility). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingListDTO {
    private List<BookingResponseDTO> items;
    private int page;
    private int totalPages;
    private long totalElements;
    private Stats stats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private long totalBookings;
        private BigDecimal totalAmount;
        private long paidCount;
        private BigDecimal paidAmount;
        private long pendingCount;
        private BigDecimal pendingAmount;
    }
}
