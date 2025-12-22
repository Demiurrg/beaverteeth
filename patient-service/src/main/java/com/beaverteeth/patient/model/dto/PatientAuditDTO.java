package com.beaverteeth.patient.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientAuditDTO {
    private Long id;
    private Long patientId;
    private String action;
    private String changedBy;
    private LocalDateTime changedAt;
    private String description;
}