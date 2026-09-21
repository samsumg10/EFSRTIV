package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String url;
    private Integer status;
    private String uuid;
    private LocalDateTime createdAt;
    private Long representativeId;
    private String representativeName;
    private String representativeEmail;
    private String representativePhone;
}
