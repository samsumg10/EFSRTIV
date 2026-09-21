package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Reserva validada y con precios calculados en el servidor, todavía sin guardar (equivale al carrito). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingQuoteDTO {
    private Long locationId;
    private String locationUuid;
    private String locationName;
    private String facilityName;
    private LocalDate date;
    private Integer numberPersons;
    private String name;
    private String email;
    private String phone;
    private BigDecimal locationPrice;
    private List<BookingLineDTO> lines;
    private BigDecimal productTotal;
    private BigDecimal total;
}
