package com.cibertec.bbq.models.DTO;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Alta/edición de empresa desde el admin: datos de la empresa + su representante. */
@Data
public class CompanyRequestDTO {
    public static final String PHONE_REGEX = "^[0-9+\\-\\s]{6,20}$";
    public static final String PHONE_MSG = "Solo números, espacios, + y - (6 a 20 caracteres).";
    public static final String URL_REGEX = "^$|^https?://\\S+$";
    public static final String URL_MSG = "Debe empezar con http:// o https://";

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

    @NotNull(message = "Elige el estado.")
    @Min(value = 0, message = "Estado no válido.")
    @Max(value = 1, message = "Estado no válido.")
    private Integer status;

    @NotBlank(message = "El nombre del representante es obligatorio.")
    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String representativeName;

    @NotBlank(message = "El correo del representante es obligatorio.")
    @Email(message = "Correo no válido.")
    @Size(max = 140, message = "Máximo 140 caracteres.")
    private String representativeEmail;

    @Pattern(regexp = PHONE_REGEX, message = PHONE_MSG)
    private String representativePhone;

    /** Obligatoria al crear; al editar, vacía = no cambiarla. */
    @Size(min = 8, max = 100, message = "Mínimo 8 caracteres.")
    private String representativePassword;
}
