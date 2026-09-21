package com.cibertec.bbq.models.DTO;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequestDTO {
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String name;

    @NotNull(message = "El precio es obligatorio.")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo.")
    @Digits(integer = 8, fraction = 2, message = "Máximo 8 enteros y 2 decimales.")
    private BigDecimal price;
}
