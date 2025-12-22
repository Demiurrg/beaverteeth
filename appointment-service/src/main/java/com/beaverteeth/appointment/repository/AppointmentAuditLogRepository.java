package com.beaverteeth.appointment.repository;

import com.beaverteeth.appointment.model.AppointmentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentAuditLogRepository extends JpaRepository<AppointmentAuditLog, Long> {

    List<AppointmentAuditLog> findByAppointmentIdOrderByChangedAtDesc(Long appointmentId);

    List<AppointmentAuditLog> findByAppointmentIdAndActionOrderByChangedAtDesc(Long appointmentId, String action);
}