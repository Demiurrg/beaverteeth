package com.beaverteeth.telegram.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TimeSlotDto {
    private Long id;
    private Long doctorId;
    private String doctorName;
    private String doctorLastName;
    private String specialty;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean available;
}