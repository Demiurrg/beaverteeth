package com.beaverteeth.patient.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientDto {
    private Long id;
    private String fullName;
    private Integer age;
    private String address;
    private String phone;
    private String email;
    private String telegramUsername;
    private Long telegramChatId;
    private Boolean isActive;
    private String notes;

    // Аудит-поля
    private String createdBy;
    private LocalDateTime createdAt;
    private String changedBy;
    private LocalDateTime changedAt;
}