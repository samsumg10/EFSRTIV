package com.cibertec.bbq.models.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ClosedDayRequestDTO {
    @NotNull(message = "La fecha de inicio es obligatoria.")
    private LocalDate startDate;

    /** Vacía = solo el día de inicio. */
    private LocalDate endDate;

    @Size(max = 1000, message = "Máximo 1000 caracteres.")
    private String reason;
}
