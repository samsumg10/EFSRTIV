package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Detalle de una reserva con su venta y productos. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDTO {
    private Long id;
    private String uuid;
    private String name;
    private String email;
    private String phone;
    private LocalDate date;
    private Integer numberPersons;
    private Long locationId;
    private String locationName;
    private Long facilityId;
    private String facilityName;
    private String companyName;
    /** "Portal" o el nombre del empleado que la registró. */
    private String origin;
    private LocalDateTime createdAt;
    private Integer status;
    private BigDecimal locationPrice;
    private BigDecimal productTotal;
    private BigDecimal total;
    private LocalDateTime paidAt;
    private LocalDate paymentDeadlineDate;
    private List<BookingLineDTO> lines;
}
