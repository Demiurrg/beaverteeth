package com.beaverteeth.appointment.model.dto;

import com.beaverteeth.appointment.model.AppointmentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentDTO {
    private Long id;
    private Long doctorId;
    private Long patientId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime changedAt;

    // Информация для отображения (заполняется сервисом)
    private String doctorName;
    private String patientName;
}