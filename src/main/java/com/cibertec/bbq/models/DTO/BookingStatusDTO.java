package com.cibertec.bbq.models.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Cambio de estado de la venta: 2 = pagado, 3 = cancelado. */
@Data
public class BookingStatusDTO {
    @NotNull(message = "Indica el nuevo estado.")
    private Integer status;
}
