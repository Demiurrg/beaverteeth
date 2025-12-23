package com.beaverteeth.doctor.model.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacationRequest {

    @NotNull(message = "ID врача обязателен")
    private Long doctorId;

    @NotNull(message = "Дата начала отпуска обязательна")
    @FutureOrPresent(message = "Дата начала отпуска должна быть сегодня или в будущем")
    private LocalDate startDate;

    @NotNull(message = "Дата окончания отпуска обязательна")
    @FutureOrPresent(message = "Дата окончания отпуска должна быть сегодня или в будущем")
    private LocalDate endDate;

    private String reason;
}