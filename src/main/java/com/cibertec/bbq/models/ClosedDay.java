package com.cibertec.bbq.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "closed_days")
public class ClosedDay extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Se guarda siempre (si viene vacío = startDate). */
    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /** Instalación de la ubicación, solo como referencia. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;
}
