package com.beaverteeth.patient.model.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePatientRequest {

    @NotBlank(message = "ФИО не может быть пустым")
    private String fullName;

    @NotNull(message = "Возраст обязателен")
    @Min(value = 0, message = "Возраст не может быть отрицательным")
    @Max(value = 150, message = "Возраст не может быть больше 150")
    private Integer age;

    @NotBlank(message = "Адрес не может быть пустым")
    private String address;

    @NotBlank(message = "Телефон не может быть пустым")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Некорректный формат телефона")
    private String phone;

    @Email(message = "Некорректный формат email")
    private String email;

    private String telegramUsername;

    private String notes;
    private String createdBy;
}