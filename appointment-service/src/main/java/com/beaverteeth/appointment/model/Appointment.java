package com.beaverteeth.appointment.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "ID врача обязателен")
    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @NotNull(message = "ID пациента обязателен")
    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @NotNull(message = "Дата и время начала обязательны")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "Дата и время окончания обязательны")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING; // ИЗМЕНИТЬ дефолтное значение

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // НОВЫЕ ПОЛЯ для подтверждения
    @Column(name = "confirmed_by")
    private String confirmedBy; // кто подтвердил/отклонил

    @Column(name = "confirmation_notes", columnDefinition = "TEXT")
    private String confirmationNotes; // причина отклонения

    @Column(name = "confirmation_date")
    private LocalDateTime confirmationDate; // когда подтвердили/отклонили

    @Column(name = "patient_chat_id")
    private Long patientChatId; // ID чата Telegram пациента

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        changedAt = LocalDateTime.now();
    }
}