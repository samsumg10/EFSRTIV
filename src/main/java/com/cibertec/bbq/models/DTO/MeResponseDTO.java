package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MeResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Integer roleType;
    private String roleName;
    private Long companyId;
    private String companyName;
    private String companyUuid;
}
