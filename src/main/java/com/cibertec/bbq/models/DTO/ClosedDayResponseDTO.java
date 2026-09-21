package com.cibertec.bbq.models.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClosedDayResponseDTO {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}
