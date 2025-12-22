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

    @JsonFormat(shape = JsonFormat.Shape.STRING)
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

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDateTime createdAt;

        private String lastModifiedBy;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
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

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDateTime createdAt;

        private String lastModifiedBy;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
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

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDateTime startTime;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDateTime endTime;

        private String status;
        private String notes;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDateTime createdAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDateTime updatedAt;
    }
}