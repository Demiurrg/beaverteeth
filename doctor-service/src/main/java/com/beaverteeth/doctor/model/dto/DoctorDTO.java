package com.beaverteeth.doctor.model.dto;

import com.beaverteeth.doctor.model.Specialty;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DoctorDTO {
    private Long id;
    private String fullName;
    private Integer totalExperience;
    private Integer clinicExperience;

    @JsonUnwrapped
    private Specialty specialty;

    private String education;
    private String certificates;
    private Boolean isActive;
    private String createdBy;
    private LocalDateTime createdAt;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedAt;

    // Добавьте метод для получения specialty как строки
    @JsonGetter("specialty")
    public String getSpecialtyName() {
        return specialty != null ? specialty.toString() : null;
    }
}