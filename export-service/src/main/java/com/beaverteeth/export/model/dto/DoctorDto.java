package com.beaverteeth.export.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDto {
    private Long id;
    private String fullName;
    private String specialty;
    private Integer totalExperience;
    private Integer clinicExperience;
    private String education;
    private String certificates;
    private Boolean isActive;
}