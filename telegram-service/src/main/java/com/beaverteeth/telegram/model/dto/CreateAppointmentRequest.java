package com.beaverteeth.telegram.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateAppointmentRequest {
    private Long doctorId;
    private Long patientId;
    private LocalDateTime startTime;
    private String notes;
    private Long patientChatId;
}