package com.beaverteeth.appointment.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAppointmentRequest {

    @NotNull(message = "ID врача обязателен")
    private Long doctorId;

    @NotNull(message = "ID пациента обязателен")
    private Long patientId;

    @NotNull(message = "Дата и время начала обязательны")
    private LocalDateTime startTime;

    private String notes;

    private Long patientChatId;
}