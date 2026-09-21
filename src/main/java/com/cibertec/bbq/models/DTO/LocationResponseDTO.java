package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponseDTO {
    private Long id;
    private Long facilityId;
    private String facilityName;
    private String name;
    private String description;
    private String uuid;
    private Integer maxPerson;
    private BigDecimal price;
}
