package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** Estado de los días visibles del calendario: ventana de reserva, días cerrados y días llenos. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDaysDTO {
    private LocalDate minDate;
    private LocalDate maxDate;
    private List<LocalDate> closedDates;
    private List<LocalDate> fullDates;
}
