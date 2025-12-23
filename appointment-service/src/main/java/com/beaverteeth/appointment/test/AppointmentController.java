package com.beaverteeth.appointment.test;

import com.beaverteeth.appointment.model.dto.*;
import com.beaverteeth.appointment.service.AppointmentService;
import com.beaverteeth.appointment.service.TimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment Controller", description = "API для управления записями на прием")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final TimeSlotService timeSlotService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать новую запись на прием")
    public AppointmentDto createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        return appointmentService.createAppointment(request);
    }

    @GetMapping
    @Operation(summary = "Получить все записи на прием")
    public List<AppointmentDto> getAllAppointments() {
        return appointmentService.getAllAppointments();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить запись по ID")
    public AppointmentDto getAppointment(@PathVariable Long id) {
        return appointmentService.getAppointmentById(id);
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Получить все записи врача")
    public List<AppointmentDto> getDoctorAppointments(@PathVariable Long doctorId) {
        return appointmentService.getAppointmentsByDoctor(doctorId);
    }

    @GetMapping("/doctor/{doctorId}/schedule")
    @Operation(summary = "Получить расписание врача на день")
    public List<AppointmentDto> getDoctorSchedule(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointmentService.getDoctorSchedule(doctorId, date);
    }

    @GetMapping("/available-slots")
    @Operation(summary = "Получить свободные слоты времени для записи")
    public List<TimeSlotDto> getAvailableTimeSlots(@Valid TimeSlotRequest request) {
        return timeSlotService.getAvailableTimeSlots(request);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Отменить запись")
    public void cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Завершить запись")
    public void completeAppointment(@PathVariable Long id) {
        appointmentService.completeAppointment(id);
    }

    @GetMapping("/patient/{patientId}/upcoming")
    @Operation(summary = "Получить предстоящие записи пациента")
    public List<AppointmentDto> getPatientUpcomingAppointments(@PathVariable Long patientId) {
        return appointmentService.getUpcomingAppointmentsForPatient(patientId);
    }

    @GetMapping("/pending")
    @Operation(summary = "Получить все записи, ожидающие подтверждения")
    public List<AppointmentDto> getPendingAppointments() {
        return appointmentService.getPendingAppointments();
    }

    @GetMapping("/confirmed")
    @Operation(summary = "Получить все подтвержденные записи")
    public List<AppointmentDto> getConfirmedAppointments() {
        return appointmentService.getConfirmedAppointments();
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Подтвердить или отклонить запись")
    public AppointmentDto confirmAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentConfirmationRequest request) {
        return appointmentService.confirmAppointment(id, request);
    }
}