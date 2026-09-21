package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Long roleId;
    private String roleName;
    private Integer roleType;
    private List<Long> facilityIds;
    private List<String> facilityNames;
    private LocalDateTime createdAt;
    private boolean me;
}
