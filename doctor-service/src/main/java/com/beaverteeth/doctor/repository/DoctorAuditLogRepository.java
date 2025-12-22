package com.beaverteeth.doctor.repository;

import com.beaverteeth.doctor.model.DoctorAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorAuditLogRepository extends JpaRepository<DoctorAuditLog, Long> {

    List<DoctorAuditLog> findByDoctorIdOrderByChangedAtDesc(Long doctorId);

    List<DoctorAuditLog> findByDoctorIdAndActionOrderByChangedAtDesc(Long doctorId, String action);
}