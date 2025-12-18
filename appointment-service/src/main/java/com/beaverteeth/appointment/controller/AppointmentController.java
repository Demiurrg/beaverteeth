package com.beaverteeth.appointment.controller;

import com.beaverteeth.appointment.model.dto.*;
import com.beaverteeth.appointment.service.AppointmentService;
import com.beaverteeth.appointment.service.TimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "Создать новую запись на прием")
    public ResponseEntity<AppointmentDTO> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {

        AppointmentDTO appointment = appointmentService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить запись по ID")
    public ResponseEntity<AppointmentDTO> getAppointment(@PathVariable Long id) {
        AppointmentDTO appointment = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(appointment);
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Получить все записи врача")
    public ResponseEntity<List<AppointmentDTO>> getDoctorAppointments(
            @PathVariable Long doctorId) {

        List<AppointmentDTO> appointments = appointmentService.getAppointmentsByDoctor(doctorId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/doctor/{doctorId}/schedule")
    @Operation(summary = "Получить расписание врача на день")
    public ResponseEntity<List<AppointmentDTO>> getDoctorSchedule(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<AppointmentDTO> appointments = appointmentService.getDoctorSchedule(doctorId, date);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/available-slots")
    @Operation(summary = "Получить свободные слоты времени для записи")
    public ResponseEntity<List<TimeSlotDTO>> getAvailableTimeSlots(
            @Valid TimeSlotRequest request) {

        List<TimeSlotDTO> slots = timeSlotService.getAvailableTimeSlots(request);
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Отменить запись")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Завершить запись")
    public ResponseEntity<Void> completeAppointment(@PathVariable Long id) {
        appointmentService.completeAppointment(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/patient/{patientId}/upcoming")
    @Operation(summary = "Получить предстоящие записи пациента")
    public ResponseEntity<List<AppointmentDTO>> getPatientUpcomingAppointments(
            @PathVariable Long patientId) {

        List<AppointmentDTO> appointments = appointmentService
                .getUpcomingAppointmentsForPatient(patientId);
        return ResponseEntity.ok(appointments);
    }
}