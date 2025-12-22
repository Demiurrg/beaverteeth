package com.beaverteeth.appointment.service;

import com.beaverteeth.appointment.model.Appointment;
import com.beaverteeth.appointment.model.AppointmentAuditLog;
import com.beaverteeth.appointment.repository.AppointmentAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final AppointmentAuditLogRepository auditLogRepository;
    private final TimeSlotService timeSlotService;

    @Async
    public void logAppointmentChange(Long appointmentId, String action, String lastModifiedBy,
                                     String description, String oldStatus, String newStatus) {
        try {
            AppointmentAuditLog auditLog = AppointmentAuditLog.builder()
                    .appointmentId(appointmentId)
                    .action(action)
                    .lastModifiedBy(lastModifiedBy)
                    .description(description)
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .build();

            auditLogRepository.save(auditLog);

            log.info("Запись в журнал аудита записи: AppointmentId={}, Action={}, LastModifiedBy={}",
                    appointmentId, action, lastModifiedBy);

        } catch (Exception e) {
            log.error("Ошибка при записи в журнал аудита записи", e);
        }
    }

    @Async
    public void logAppointmentCreation(Appointment appointment, String createdBy) {
        String doctorName = timeSlotService.getDoctorName(appointment.getDoctorId());
        String patientName = timeSlotService.getPatientName(appointment.getPatientId());

        String description = String.format(
                "Создана новая запись: врач %s, пациент %s, время: %s - %s",
                doctorName, patientName,
                appointment.getStartTime(), appointment.getEndTime()
        );

        logAppointmentChange(
                appointment.getId(),
                "CREATE",
                createdBy,
                description,
                null,
                appointment.getStatus().name()
        );
    }

    @Async
    public void logStatusChange(Appointment appointment, String oldStatus,
                                String newStatus, String changedBy) {
        String doctorName = timeSlotService.getDoctorName(appointment.getDoctorId());
        String patientName = timeSlotService.getPatientName(appointment.getPatientId());

        String description = String.format(
                "Изменен статус записи: врач %s, пациент %s. Было: %s, Стало: %s",
                doctorName, patientName, oldStatus, newStatus
        );

        logAppointmentChange(
                appointment.getId(),
                "UPDATE_STATUS",
                changedBy,
                description,
                oldStatus,
                newStatus
        );
    }

    public List<AppointmentAuditLog> getAppointmentHistory(Long appointmentId) {
        return auditLogRepository.findByAppointmentIdOrderByChangedAtDesc(appointmentId);
    }
}