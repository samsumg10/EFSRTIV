package com.cibertec.bbq.models.DTO;

import jakarta.validation.constraints.*;
import lombok.Data;

import static com.cibertec.bbq.models.DTO.CompanyRequestDTO.PHONE_MSG;
import static com.cibertec.bbq.models.DTO.CompanyRequestDTO.PHONE_REGEX;

@Data
public class FacilityRequestDTO {
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String name;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "Correo no válido.")
    @Size(max = 150, message = "Máximo 150 caracteres.")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio.")
    @Pattern(regexp = PHONE_REGEX, message = PHONE_MSG)
    private String phone;

    @Size(max = 255, message = "Máximo 255 caracteres.")
    private String address;

    @Size(max = 2000, message = "Máximo 2000 caracteres.")
    private String description;
}
