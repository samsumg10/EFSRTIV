package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** Datos fijos de la página de reservas de una ubicación. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarInfoDTO {
    private LocationResponseDTO location;
    private List<ProductResponseDTO> products;
    private LocalDate minDate;
    private LocalDate maxDate;
}
