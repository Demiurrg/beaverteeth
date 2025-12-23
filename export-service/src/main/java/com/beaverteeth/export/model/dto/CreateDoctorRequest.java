package com.beaverteeth.export.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDoctorRequest {
    private String fullName;
    private Integer totalExperience;
    private Integer clinicExperience;
    private String specialty;
    private String education;
    private String certificates;
    private String createdBy;
}