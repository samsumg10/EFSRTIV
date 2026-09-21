package com.cibertec.bbq.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "locations")
public class Location extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    /** Capacidad diaria en personas. NULL = ubicación exclusiva (1 reserva por día). */
    @Column(name = "max_person")
    private Integer maxPerson;

    /** Precio por reserva (sales.location_price). */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;
}
