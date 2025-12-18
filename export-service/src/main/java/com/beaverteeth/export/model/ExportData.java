package com.beaverteeth.export.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportData {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime exportDate;

    private List<DoctorData> doctors;
    private List<PatientData> patients;
    private List<AppointmentData> appointments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoctorData {
        private Long id;
        private String fullName;
        private Integer totalExperience;
        private Integer clinicExperience;
        private String specialty;
        private String education;
        private String certificates;
        private Boolean isActive;
        private String createdBy;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        private String lastModifiedBy;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastModifiedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PatientData {
        private Long id;
        private String fullName;
        private Integer age;
        private String address;
        private String phone;
        private String email;
        private Boolean isActive;
        private String notes;
        private String createdBy;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        private String lastModifiedBy;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime lastModifiedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppointmentData {
        private Long id;
        private Long doctorId;
        private Long patientId;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;

        private String status;
        private String notes;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;
    }
}