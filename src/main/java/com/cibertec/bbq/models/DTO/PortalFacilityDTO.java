package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** /facility/{uuid}: instalación y sus ubicaciones para el cliente. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortalFacilityDTO {
    private String uuid;
    private String name;
    private String description;
    private String address;
    private String phone;
    private String email;
    private String companyName;
    private String companyUrl;
    private List<PortalLocationDTO> locations;
}
