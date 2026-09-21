package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Línea de producto de una reserva o cotización. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingLineDTO {
    private Long productId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal total;
}
