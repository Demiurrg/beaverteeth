package com.beaverteeth.doctor.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "ФИО не может быть пустым")
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Min(value = 0, message = "Общий стаж не может быть отрицательным")
    @Column(name = "total_experience", nullable = false)
    private Integer totalExperience; // в годах

    @Min(value = 0, message = "Стаж в клинике не может быть отрицательным")
    @Column(name = "clinic_experience", nullable = false)
    private Integer clinicExperience; // в годах

    @Enumerated(EnumType.STRING)
    @Column(name = "specialty", nullable = false)
    private Specialty specialty;

    @NotBlank(message = "Информация об образовании обязательна")
    @Column(name = "education", columnDefinition = "TEXT")
    private String education;

    @Column(name = "certificates", columnDefinition = "TEXT")
    private String certificates;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public String getLastName() {
        if (fullName == null || fullName.isEmpty()) {
            return "";
        }

        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }
}