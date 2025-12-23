package com.beaverteeth.telegram.model.dto;

import lombok.Data;

@Data
public class PatientDto {
    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String telegramUsername;
    private Long telegramChatId;
}