package com.beaverteeth.appointment.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotRequest {

    @NotNull(message = "Дата обязательна")
    private LocalDate date;

    @NotNull(message = "Фамилия врача обязательна")
    private String doctorLastName;
}