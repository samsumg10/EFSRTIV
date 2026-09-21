package com.cibertec.bbq.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 1 = administrador de empresa, 2 = empleado (AppConstants.ROLE_TYPE_*) */
    @Column(name = "type", nullable = false)
    private Integer type;
}
