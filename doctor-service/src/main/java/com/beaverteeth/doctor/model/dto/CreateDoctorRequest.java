package com.beaverteeth.doctor.model.dto;

import com.beaverteeth.doctor.model.Specialty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDoctorRequest {

    @NotBlank(message = "ФИО не может быть пустым")
    private String fullName;

    @NotNull(message = "Общий стаж обязателен")
    @Min(value = 0, message = "Общий стаж не может быть отрицательным")
    private Integer totalExperience;

    @NotNull(message = "Стаж в клинике обязателен")
    @Min(value = 0, message = "Стаж в клинике не может быть отрицательным")
    private Integer clinicExperience;

    @NotNull(message = "Специализация обязательна")
    private Specialty specialty;

    @NotBlank(message = "Информация об образовании обязательна")
    private String education;

    private String certificates;

    private String createdBy;
}