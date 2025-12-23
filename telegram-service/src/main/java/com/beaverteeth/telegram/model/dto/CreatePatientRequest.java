package com.beaverteeth.telegram.model.dto;

import lombok.Data;

@Data
public class CreatePatientRequest {
    private String fullName;
    private Integer age;
    private String address;
    private String phone;
    private String email;
    private String telegramUsername;
    private Long telegramChatId;
    private String notes;
}