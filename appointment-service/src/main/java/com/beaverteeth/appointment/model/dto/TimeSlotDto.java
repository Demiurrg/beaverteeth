package com.beaverteeth.appointment.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotDto {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long doctorId;
    private String doctorName;
}