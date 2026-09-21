package com.cibertec.bbq.models.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

import static com.cibertec.bbq.models.DTO.CompanyRequestDTO.PHONE_MSG;
import static com.cibertec.bbq.models.DTO.CompanyRequestDTO.PHONE_REGEX;

/** Reserva (manual desde la empresa o desde el portal). También se usa para cotizar. */
@Data
public class BookingRequestDTO {
    @NotNull(message = "Elige la fecha de la reserva.")
    private LocalDate date;

    @NotNull(message = "Indica el número de personas.")
    @Min(value = 1, message = "Mínimo 1 persona.")
    @Max(value = 99999, message = "Número de personas no válido.")
    private Integer numberPersons;

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

    @Valid
    private List<ProductQuantityDTO> products;

    @Data
    public static class ProductQuantityDTO {
        @NotNull(message = "Producto no válido.")
        private Long productId;

        @NotNull(message = "Indica la cantidad.")
        @Min(value = 0, message = "La cantidad no puede ser negativa.")
        @Max(value = 999, message = "Máximo 999 unidades.")
        private Integer quantity;
    }
}
