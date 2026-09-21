package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Disponibilidad de una ubicación en un día. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDTO {
    private LocalDate date;
    private boolean available;
    /** Motivo si no está disponible (fuera de plazo, cerrado, lleno). */
    private String message;
    /** true = ubicación exclusiva (max_person NULL): una reserva por día. */
    private boolean exclusive;
    private Integer maxPerson;
    private long occupiedPersons;
    /** null si es exclusiva. */
    private Integer remainingPersons;
}
