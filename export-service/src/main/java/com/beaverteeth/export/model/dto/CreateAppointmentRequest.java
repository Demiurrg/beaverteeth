package com.beaverteeth.export.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequest {
    private Long doctorId;
    private Long patientId;
    private LocalDateTime startTime;
    private String notes;
}