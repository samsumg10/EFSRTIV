package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Evento en el formato que espera FullCalendar (los campos extra van a extendedProps). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventDTO {
    private Long id;
    private String title;
    private LocalDate start;
    private boolean allDay;
    private Integer status;
    private Integer numberPersons;
}
