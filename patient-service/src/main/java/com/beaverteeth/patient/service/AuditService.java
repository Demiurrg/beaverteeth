package com.beaverteeth.patient.service;

import com.beaverteeth.patient.model.Patient;
import com.beaverteeth.patient.model.PatientAuditLog;
import com.beaverteeth.patient.repository.PatientAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final PatientAuditLogRepository auditLogRepository;

    @Async
    public void logPatientChange(Long patientId, String action, String changedBy, String description) {
        try {
            PatientAuditLog auditLog = PatientAuditLog.builder()
                    .patientId(patientId)
                    .action(action)
                    .changedBy(changedBy)
                    .description(description)
                    .build();

            auditLogRepository.save(auditLog);

            log.info("Запись в журнал аудита пациента: PatientId={}, Action={}, ChangedBy={}",
                    patientId, action, changedBy);

        } catch (Exception e) {
            log.error("Ошибка при записи в журнал аудита пациента", e);
        }
    }

    public List<PatientAuditLog> getPatientHistory(Long patientId) {
        return auditLogRepository.findByPatientIdOrderByChangedAtDesc(patientId);
    }

    // Для обратной совместимости
    @Async
    public void logPatientChange(Patient patient, String action) {
        String changedBy = patient.getLastModifiedBy() != null ?
                patient.getLastModifiedBy() :
                patient.getCreatedBy();

        String description = getDefaultDescription(action, patient.getFullName(), patient.getPhone());

        logPatientChange(patient.getId(), action, changedBy, description);
    }

    private String getDefaultDescription(String action, String patientName, String phone) {
        switch (action) {
            case "CREATE":
                return "Создан новый пациент: " + patientName + " (тел.: " + phone + ")";
            case "UPDATE":
                return "Обновлены данные пациента: " + patientName;
            case "DELETE":
                return "Пациент отключен (мягкое удаление): " + patientName;
            default:
                return "Действие '" + action + "' над пациентом: " + patientName;
        }
    }
}