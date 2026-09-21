package com.cibertec.bbq.models.DTO;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RoleRequestDTO {
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String name;

    @NotNull(message = "Elige el tipo de rol.")
    @Min(value = 1, message = "Tipo no válido.")
    @Max(value = 2, message = "Tipo no válido.")
    private Integer type;
}
