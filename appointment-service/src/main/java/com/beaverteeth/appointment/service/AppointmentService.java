package com.beaverteeth.appointment.service;

import com.beaverteeth.appointment.model.Appointment;
import com.beaverteeth.appointment.model.AppointmentStatus;
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
    private static final int APPOINTMENT_DURATION_HOURS = 2;

    public AppointmentDTO createAppointment(CreateAppointmentRequest request) {
        // Проверяем, что врач и пациент существуют
        timeSlotService.validateDoctorExists(request.getDoctorId());
        timeSlotService.validatePatientExists(request.getPatientId());

        LocalDateTime endTime = request.getStartTime().plusHours(APPOINTMENT_DURATION_HOURS);

        // Проверяем доступность времени - ДОЛЖНА ИСПОЛЬЗОВАТЬ ИСПРАВЛЕННЫЙ МЕТОД
        List<Appointment> conflictingAppointments = appointmentRepository
                .findConflictingAppointments(
                        request.getDoctorId(),
                        request.getStartTime(),
                        endTime
                );

        log.info("Проверка конфликтов для врача {} с {} до {}. Найдено конфликтов: {}",
                request.getDoctorId(), request.getStartTime(), endTime, conflictingAppointments.size());

        if (!conflictingAppointments.isEmpty()) {
            throw new IllegalArgumentException("Время уже занято");
        }

        // Создаем запись
        Appointment appointment = Appointment.builder()
                .doctorId(request.getDoctorId())
                .patientId(request.getPatientId())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .status(AppointmentStatus.SCHEDULED)
                .notes(request.getNotes())
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Создана запись на прием: врач={}, пациент={}, время={}",
                request.getDoctorId(), request.getPatientId(), request.getStartTime());

        return convertToDTO(savedAppointment);
    }

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

    public void cancelAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запись не найдена с ID: " + id));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
        log.info("Запись отменена: ID={}", id);
    }

    public void completeAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запись не найдена с ID: " + id));

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
        log.info("Запись завершена: ID={}", id);
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
                .updatedAt(appointment.getUpdatedAt())
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
}