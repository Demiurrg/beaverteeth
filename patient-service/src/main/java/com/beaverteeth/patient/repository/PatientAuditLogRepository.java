package com.beaverteeth.patient.repository;

import com.beaverteeth.patient.model.PatientAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientAuditLogRepository extends JpaRepository<PatientAuditLog, Long> {

    List<PatientAuditLog> findByPatientIdOrderByChangedAtDesc(Long patientId);

    List<PatientAuditLog> findByPatientIdAndActionOrderByChangedAtDesc(Long patientId, String action);
}