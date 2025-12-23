package com.beaverteeth.telegram.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DoctorDto {
    private Long id;
    private String fullName;
    private Integer totalExperience;
    private Integer clinicExperience;
    private String specialty;
    private String education;
    private String certificates;
    private Boolean isActive;
    private String createdBy;
    private LocalDateTime createdAt;
    private String changedBy;
    private LocalDateTime changedAt;
}