package com.beaverteeth.appointment.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditEntity {

    @Column(name = "created_by")
    protected String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    protected LocalDateTime createdAt;

    @Column(name = "changed_by")
    protected String changedBy;

    @UpdateTimestamp
    @Column(name = "changed_at")
    protected LocalDateTime changedAt;
}