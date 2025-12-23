package com.beaverteeth.appointment.service;

import com.beaverteeth.appointment.model.Appointment;
import com.beaverteeth.appointment.model.AppointmentStatus;
import com.beaverteeth.appointment.model.dto.AppointmentConfirmationRequest;
import com.beaverteeth.appointment.model.dto.AppointmentDTO;
import com.beaverteeth.appointment.model.dto.CreateAppointmentRequest;
import com.beaverteeth.appointment.repository.AppointmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotService timeSlotService;
    private final AuditService auditService;
    private final TelegramNotificationService telegramNotificationService;

    private static final int APPOINTMENT_DURATION_HOURS = 2;

    public AppointmentDTO createAppointment(CreateAppointmentRequest request) {
        String createdBy = "system";

        // Проверяем существование врача и пациента
        timeSlotService.validateDoctorExists(request.getDoctorId());
        timeSlotService.validatePatientExists(request.getPatientId());

        // ПРОВЕРКА ОТПУСКА
        LocalDate appointmentDate = request.getStartTime().toLocalDate();
        if (timeSlotService.isDoctorOnVacation(request.getDoctorId(), appointmentDate)) {
            throw new IllegalArgumentException("Врач в отпуске на выбранную дату");
        }

        LocalDateTime endTime = request.getStartTime().plusHours(APPOINTMENT_DURATION_HOURS);

        // Проверяем доступность времени
        List<Appointment> conflictingAppointments = appointmentRepository
                .findConflictingAppointments(
                        request.getDoctorId(),
                        request.getStartTime(),
                        endTime
                );

        if (!conflictingAppointments.isEmpty()) {
            throw new IllegalArgumentException("Время уже занято");
        }

        // Используем patientChatId из запроса, если он есть
        Long patientChatId = request.getPatientChatId();

        // Если не передан в запросе, пробуем получить из профиля пациента
        if (patientChatId == null) {
            patientChatId = timeSlotService.getPatientChatId(request.getPatientId());
        }

        // Создаем запись со статусом PENDING
        Appointment appointment = Appointment.builder()
                .doctorId(request.getDoctorId())
                .patientId(request.getPatientId())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .status(AppointmentStatus.PENDING)
                .notes(request.getNotes())
                .patientChatId(patientChatId) // Сохраняем chatId
                .build();

        appointment.setCreatedBy(createdBy);
        appointment.setChangedBy(createdBy);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Создана запись на прием (ожидает подтверждения): врач={}, пациент={}, время={}, chatId={}",
                request.getDoctorId(), request.getPatientId(), request.getStartTime(), patientChatId);

        // Запись в журнал
        auditService.logAppointmentCreation(savedAppointment, createdBy);

        return convertToDTO(savedAppointment);
    }

    public AppointmentDTO confirmAppointment(Long id, AppointmentConfirmationRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запись не найдена с ID: " + id));

        // Сохраняем старый статус для аудита
        AppointmentStatus oldStatus = appointment.getStatus();

        if ("confirm".equalsIgnoreCase(request.getAction())) {
            appointment.setStatus(AppointmentStatus.CONFIRMED);
        } else if ("reject".equalsIgnoreCase(request.getAction())) {
            appointment.setStatus(AppointmentStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("Некорректное действие: используйте 'confirm' или 'reject'");
        }

        appointment.setConfirmedBy(request.getConfirmedBy());
        appointment.setConfirmationNotes(request.getNotes());
        appointment.setConfirmationDate(LocalDateTime.now());
        appointment.setChangedBy(request.getConfirmedBy());

        Appointment updatedAppointment = appointmentRepository.save(appointment);

        // Запись в журнал аудита
        auditService.logStatusChange(updatedAppointment, oldStatus.name(),
                updatedAppointment.getStatus().name(),
                request.getConfirmedBy());

        // Отправляем уведомление пациенту через Telegram
        telegramNotificationService.sendAppointmentConfirmation(
                updatedAppointment,
                oldStatus,
                updatedAppointment.getStatus()
        );

        log.info("Запись {} {} пользователем {}",
                id, request.getAction(), request.getConfirmedBy());

        return convertToDTO(updatedAppointment);
    }

    public List<AppointmentDTO> getPendingAppointments() {
        List<Appointment> appointments = appointmentRepository.findByStatus(
                AppointmentStatus.PENDING);
        return appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AppointmentDTO> getConfirmedAppointments() {
        List<Appointment> appointments = appointmentRepository.findByStatus(
                AppointmentStatus.CONFIRMED);
        return appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Обновить метод convertToDTO
    private AppointmentDTO convertToDTO(Appointment appointment) {
        AppointmentDTO dto = AppointmentDTO.builder()
                .id(appointment.getId())
                .doctorId(appointment.getDoctorId())
                .patientId(appointment.getPatientId())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())
                .notes(appointment.getNotes())
                .createdAt(appointment.getCreatedAt())
                .changedAt(appointment.getChangedAt())
                .confirmedBy(appointment.getConfirmedBy())
                .confirmationNotes(appointment.getConfirmationNotes())
                .confirmationDate(appointment.getConfirmationDate())
                .patientChatId(appointment.getPatientChatId())
                .build();

        // Получаем дополнительную информацию
        try {
            dto.setDoctorName(timeSlotService.getDoctorName(appointment.getDoctorId()));
            dto.setPatientName(timeSlotService.getPatientName(appointment.getPatientId()));
        } catch (Exception e) {
            log.warn("Не удалось получить информацию о враче/пациенте: {}", e.getMessage());
        }

        return dto;
    }

    public void cancelAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запись не найдена с ID: " + id));

        String oldStatus = appointment.getStatus().name();
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setChangedBy("system"); // Нужно получить из запроса

        appointmentRepository.save(appointment);
        log.info("Запись отменена: ID={}", id);

        // ЗАПИСЬ В ЖУРНАЛ
        auditService.logStatusChange(appointment, oldStatus, "CANCELLED", "system");
    }

    public void completeAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запись не найдена с ID: " + id));

        String oldStatus = appointment.getStatus().name();
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setChangedBy("system"); // Нужно получить из запроса

        appointmentRepository.save(appointment);
        log.info("Запись завершена: ID={}", id);

        // ЗАПИСЬ В ЖУРНАЛ
        auditService.logStatusChange(appointment, oldStatus, "COMPLETED", "system");
    }

    // ДОБАВИТЬ метод для изменения времени записи
    public AppointmentDTO rescheduleAppointment(Long id, LocalDateTime newStartTime, String modifiedBy) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запись не найдена с ID: " + id));

        LocalDateTime newEndTime = newStartTime.plusHours(APPOINTMENT_DURATION_HOURS);

        // Проверяем доступность нового времени
        List<Appointment> conflictingAppointments = appointmentRepository
                .findConflictingAppointments(
                        appointment.getDoctorId(),
                        newStartTime,
                        newEndTime
                )
                .stream()
                .filter(a -> !a.getId().equals(id)) // исключаем текущую запись
                .collect(Collectors.toList());

        if (!conflictingAppointments.isEmpty()) {
            throw new IllegalArgumentException("Новое время уже занято");
        }

        // Сохраняем старое время для аудита
        LocalDateTime oldStartTime = appointment.getStartTime();
        LocalDateTime oldEndTime = appointment.getEndTime();

        appointment.setStartTime(newStartTime);
        appointment.setEndTime(newEndTime);
        appointment.setChangedBy(modifiedBy);

        Appointment updatedAppointment = appointmentRepository.save(appointment);

        // ЗАПИСЬ В ЖУРНАЛ для переноса
        String doctorName = timeSlotService.getDoctorName(appointment.getDoctorId());
        String patientName = timeSlotService.getPatientName(appointment.getPatientId());

        String description = String.format(
                "Перенос записи: врач %s, пациент %s. Было: %s - %s, Стало: %s - %s",
                doctorName, patientName, oldStartTime, oldEndTime, newStartTime, newEndTime
        );

        auditService.logAppointmentChange(
                id, "RESCHEDULE", modifiedBy, description,
                appointment.getStatus().name(), appointment.getStatus().name()
        );

        return convertToDTO(updatedAppointment);
    }

    // Остальные методы остаются БЕЗ изменений или с минимальными правками
    public AppointmentDTO getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запись не найдена с ID: " + id));

        return convertToDTO(appointment);
    }

    public List<AppointmentDTO> getAppointmentsByDoctor(Long doctorId) {
        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctorId);

        return appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AppointmentDTO> getDoctorSchedule(Long doctorId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndDateRange(doctorId, startOfDay, endOfDay);

        return appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AppointmentDTO> getUpcomingAppointmentsForPatient(Long patientId) {
        LocalDateTime now = LocalDateTime.now();

        List<Appointment> appointments = appointmentRepository
                .findByPatientId(patientId).stream()
                .filter(a -> a.getStartTime().isAfter(now)
                        && a.getStatus() == AppointmentStatus.SCHEDULED)
                .collect(Collectors.toList());

        return appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}