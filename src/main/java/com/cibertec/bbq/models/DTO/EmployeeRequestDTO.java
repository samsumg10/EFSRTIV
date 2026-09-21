package com.cibertec.bbq.models.DTO;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

import static com.cibertec.bbq.models.DTO.CompanyRequestDTO.PHONE_MSG;
import static com.cibertec.bbq.models.DTO.CompanyRequestDTO.PHONE_REGEX;

@Data
public class EmployeeRequestDTO {
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String name;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "Correo no válido.")
    @Size(max = 140, message = "Máximo 140 caracteres.")
    private String email;

    @Pattern(regexp = PHONE_REGEX, message = PHONE_MSG)
    private String phone;

    /** Obligatoria al crear; al editar, vacía = no cambiarla. */
    @Size(min = 8, max = 100, message = "Mínimo 8 caracteres.")
    private String password;

    private String passwordConfirmation;

    @NotNull(message = "Elige un rol.")
    private Long roleId;

    /** Instalaciones asignadas (solo se usan si el rol es de tipo Empleado). */
    private List<Long> facilityIds;
}
