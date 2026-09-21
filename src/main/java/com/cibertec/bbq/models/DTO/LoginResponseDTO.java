package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private Long id;
    private String name;
    private String area;
    private Long companyId;
    private String companyName;
    private Integer roleType;
}
