package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String url;
    private String uuid;
    private Long accountId;
    private String accountName;
    private String accountEmail;
    private String accountPhone;
    private String accountRoleName;
}
