package com.cibertec.bbq.models.DTO;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LocationRequestDTO {
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String name;

    @Size(max = 2000, message = "Máximo 2000 caracteres.")
    private String description;

    /** Capacidad diaria en personas; vacío = ubicación exclusiva (1 reserva por día). */
    @Min(value = 1, message = "La capacidad mínima es 1.")
    @Max(value = 99999, message = "La capacidad máxima es 99999.")
    private Integer maxPerson;

    @NotNull(message = "El precio es obligatorio.")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo.")
    @Digits(integer = 8, fraction = 2, message = "Máximo 8 enteros y 2 decimales.")
    private BigDecimal price;
}
