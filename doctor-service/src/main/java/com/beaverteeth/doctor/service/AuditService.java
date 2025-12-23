package com.beaverteeth.doctor.service;

import com.beaverteeth.doctor.model.Doctor;
import com.beaverteeth.doctor.model.DoctorAuditLog;
import com.beaverteeth.doctor.repository.DoctorAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final DoctorAuditLogRepository auditLogRepository;

    @Async
    public void logDoctorChange(Long doctorId, String action, String changedBy, String description) {
        try {
            DoctorAuditLog auditLog = DoctorAuditLog.builder()
                    .doctorId(doctorId)
                    .action(action)
                    .changedBy(changedBy)
                    .description(description)
                    .build();

            auditLogRepository.save(auditLog);

            // Лог для отладки
            log.info("Запись в журнал аудита: DoctorId={}, Action={}, ChangedBy={}",
                    doctorId, action, changedBy);

        } catch (Exception e) {
            log.error("Ошибка при записи в журнал аудита", e);
        }
    }

    public List<DoctorAuditLog> getDoctorHistory(Long doctorId) {
        return auditLogRepository.findByDoctorIdOrderByChangedAtDesc(doctorId);
    }

    // Для обратной совместимости с существующим кодом
    @Async
    public void logDoctorChange(Doctor doctor, String action) {
        String changedBy = doctor.getChangedBy() != null ?
                doctor.getChangedBy() :
                doctor.getCreatedBy();

        String description = getDefaultDescription(action, doctor.getFullName());

        logDoctorChange(doctor.getId(), action, changedBy, description);
    }

    private String getDefaultDescription(String action, String doctorName) {
        switch (action) {
            case "CREATE":
                return "Создан новый врач: " + doctorName;
            case "UPDATE":
                return "Обновлены данные врача: " + doctorName;
            case "DELETE":
                return "Врач отключен (мягкое удаление): " + doctorName;
            default:
                return "Действие '" + action + "' над врачом: " + doctorName;
        }
    }
}