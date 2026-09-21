package com.cibertec.bbq.models.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "Ingresa tu correo.")
    @Email(message = "Correo no válido.")
    private String email;

    @NotBlank(message = "Ingresa tu contraseña.")
    private String password;
}
