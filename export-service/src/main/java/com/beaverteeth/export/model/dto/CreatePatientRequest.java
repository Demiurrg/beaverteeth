package com.beaverteeth.export.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePatientRequest {
    private String fullName;
    private Integer age;
    private String address;
    private String phone;
    private String email;
    private String notes;
    private String createdBy;
}
