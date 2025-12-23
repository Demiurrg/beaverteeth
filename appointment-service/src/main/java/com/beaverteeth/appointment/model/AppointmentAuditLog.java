package com.beaverteeth.appointment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "action", nullable = false, length = 20)
    private String action; // CREATE, UPDATE_STATUS, CANCELLED, COMPLETED, RESCHEDULE

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "old_status", length = 20)
    private String oldStatus; // предыдущий статус

    @Column(name = "new_status", length = 20)
    private String newStatus; // новый статус
}