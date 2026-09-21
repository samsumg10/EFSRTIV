package com.cibertec.bbq.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** Instalaciones asignadas a un empleado (solo aplica a roles de tipo 2). La tabla no tiene updated_at. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "employee_facility")
@EntityListeners(AuditingEntityListener.class)
public class EmployeeFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public EmployeeFacility(Employee employee, Facility facility) {
        this.employee = employee;
        this.facility = facility;
    }
}
