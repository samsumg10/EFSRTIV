package com.cibertec.bbq.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "companies")
public class Company extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    /** 1 = activa, 0 = inactiva (portal 403 y sin login) */
    @Column(name = "status", nullable = false)
    private Integer status = AppConstants.COMPANY_ACTIVE;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "url", length = 255)
    private String url;
}
