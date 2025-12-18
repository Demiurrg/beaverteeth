package com.beaverteeth.doctor.model.dto;

import com.beaverteeth.doctor.model.Specialty;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorDTO {
    private Long id;
    private String fullName;
    private Integer totalExperience;
    private Integer clinicExperience;
    private Specialty specialty;
    private String education;
    private String certificates;
    private Boolean isActive;

    // Аудит-поля
    private String createdBy;
    private LocalDateTime createdAt;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedAt;
}