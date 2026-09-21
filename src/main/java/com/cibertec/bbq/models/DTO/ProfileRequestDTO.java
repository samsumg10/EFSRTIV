package com.cibertec.bbq.models.DTO;

import jakarta.validation.constraints.*;
import lombok.Data;

import static com.cibertec.bbq.models.DTO.CompanyRequestDTO.*;

/** /company/profile: datos de la empresa + la cuenta del usuario que inició sesión. */
@Data
public class ProfileRequestDTO {
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String name;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "Correo no válido.")
    @Size(max = 140, message = "Máximo 140 caracteres.")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio.")
    @Pattern(regexp = PHONE_REGEX, message = PHONE_MSG)
    private String phone;

    @Size(max = 255, message = "Máximo 255 caracteres.")
    private String address;

    @Size(max = 255, message = "Máximo 255 caracteres.")
    @Pattern(regexp = URL_REGEX, message = URL_MSG)
    private String url;

    @NotBlank(message = "Tu nombre es obligatorio.")
    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String accountName;

    @NotBlank(message = "Tu correo es obligatorio.")
    @Email(message = "Correo no válido.")
    @Size(max = 140, message = "Máximo 140 caracteres.")
    private String accountEmail;

    @Pattern(regexp = PHONE_REGEX, message = PHONE_MSG)
    private String accountPhone;

    /** Vacía = no cambiarla. */
    @Size(min = 8, max = 100, message = "Mínimo 8 caracteres.")
    private String accountPassword;

    private String accountPasswordConfirmation;
}
