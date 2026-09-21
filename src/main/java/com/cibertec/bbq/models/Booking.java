package com.cibertec.bbq.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Una reserva ocupa un día completo de una ubicación. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    /** Empleado que la registró; NULL = reserva hecha desde el portal. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "number_persons", nullable = false)
    private Integer numberPersons;
}
