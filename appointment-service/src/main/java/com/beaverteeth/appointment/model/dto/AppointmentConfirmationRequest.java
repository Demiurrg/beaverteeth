package com.beaverteeth.appointment.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentConfirmationRequest {

    @NotNull(message = "Статус подтверждения обязателен")
    private String action;

    private String notes;

    @NotBlank(message = "Имя подтверждающего обязательно")
    private String confirmedBy;
}