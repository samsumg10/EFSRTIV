package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Ubicación vista por el cliente. products/minDate/maxDate solo se llenan en /location/{uuid}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortalLocationDTO {
    private String uuid;
    private String name;
    private String description;
    private Integer maxPerson;
    private BigDecimal price;
    private String facilityUuid;
    private String facilityName;
    private String facilityAddress;
    private String facilityPhone;
    private String companyName;
    private List<ProductResponseDTO> products;
    private LocalDate minDate;
    private LocalDate maxDate;
}
