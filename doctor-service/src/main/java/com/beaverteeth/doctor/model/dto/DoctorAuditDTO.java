package com.beaverteeth.doctor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorAuditDTO {
    private Long id;
    private Long doctorId;
    private String action;
    private String changedBy;
    private LocalDateTime changedAt;
    private String description;
}