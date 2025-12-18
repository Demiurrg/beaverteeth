package com.beaverteeth.appointment.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotDTO {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long doctorId;
    private String doctorName;
}