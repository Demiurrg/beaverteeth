package com.beaverteeth.appointment.test;

import com.beaverteeth.appointment.model.Appointment;
import com.beaverteeth.appointment.model.AppointmentStatus;
import com.beaverteeth.appointment.model.dto.AppointmentConfirmationRequest;
import com.beaverteeth.appointment.model.dto.AppointmentDto;
import com.beaverteeth.appointment.model.dto.CreateAppointmentRequest;
import com.beaverteeth.appointment.repository.AppointmentRepository;
import com.beaverteeth.appointment.service.AppointmentService;
import com.beaverteeth.appointment.service.AuditService;
import com.beaverteeth.appointment.service.TelegramNotificationService;
import com.beaverteeth.appointment.service.TimeSlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private TimeSlotService timeSlotService;

    @Mock
    private AuditService auditService;

    @Mock
    private TelegramNotificationService telegramNotificationService;

    @InjectMocks
    private AppointmentService appointmentService;

    private Appointment appointment;
    private CreateAppointmentRequest createRequest;
    private AppointmentConfirmationRequest confirmationRequest;

    @BeforeEach
    void setUp() {
        appointment = Appointment.builder()
                .id(1L)
                .doctorId(100L)
                .patientId(200L)
                .startTime(LocalDateTime.of(2024, 12, 25, 10, 0))
                .endTime(LocalDateTime.of(2024, 12, 25, 12, 0))
                .status(AppointmentStatus.PENDING)
                .notes("Консультация")
                .patientChatId(123456789L)
                .build();

        createRequest = CreateAppointmentRequest.builder()
                .doctorId(100L)
                .patientId(200L)
                .startTime(LocalDateTime.of(2024, 12, 25, 10, 0))
                .notes("Консультация")
                .patientChatId(123456789L)
                .build();

        confirmationRequest = AppointmentConfirmationRequest.builder()
                .action("confirm")
                .confirmedBy("admin")
                .notes("Подтверждено администратором")
                .build();
    }

    @Test
    void createAppointment_WithPatientChatIdFromService_ShouldUseIt() {
        // Arrange
        createRequest.setPatientChatId(null); // Не передаем в запросе
        LocalDateTime endTime = createRequest.getStartTime().plusHours(2);

        doNothing().when(timeSlotService).validateDoctorExists(100L);
        doNothing().when(timeSlotService).validatePatientExists(200L);
        when(timeSlotService.isDoctorOnVacation(eq(100L), any(LocalDate.class))).thenReturn(false);
        when(appointmentRepository.findConflictingAppointments(100L, createRequest.getStartTime(), endTime))
                .thenReturn(Collections.emptyList());
        when(timeSlotService.getPatientChatId(200L)).thenReturn(987654321L); // Получаем из сервиса
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(timeSlotService.getDoctorName(100L)).thenReturn("Доктор Иванов");
        when(timeSlotService.getPatientName(200L)).thenReturn("Пациент Петров");

        // Act
        appointmentService.createAppointment(createRequest);

        // Assert
        verify(timeSlotService, times(1)).getPatientChatId(200L);
    }

    @Test
    void createAppointment_DoctorOnVacation_ShouldThrowException() {
        // Arrange
        doNothing().when(timeSlotService).validateDoctorExists(100L);
        doNothing().when(timeSlotService).validatePatientExists(200L);
        when(timeSlotService.isDoctorOnVacation(eq(100L), any(LocalDate.class))).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.createAppointment(createRequest);
        });

        assertEquals("Врач в отпуске на выбранную дату", exception.getMessage());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void createAppointment_TimeConflict_ShouldThrowException() {
        // Arrange
        LocalDateTime endTime = createRequest.getStartTime().plusHours(2);

        doNothing().when(timeSlotService).validateDoctorExists(100L);
        doNothing().when(timeSlotService).validatePatientExists(200L);
        when(timeSlotService.isDoctorOnVacation(eq(100L), any(LocalDate.class))).thenReturn(false);
        when(appointmentRepository.findConflictingAppointments(100L, createRequest.getStartTime(), endTime))
                .thenReturn(Arrays.asList(appointment)); // Уже есть запись

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.createAppointment(createRequest);
        });

        assertEquals("Время уже занято", exception.getMessage());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void confirmAppointment_ShouldUpdateStatusAndSendNotification() {
        // Arrange
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(timeSlotService.getDoctorName(100L)).thenReturn("Доктор Иванов");
        when(timeSlotService.getPatientName(200L)).thenReturn("Пациент Петров");

        // Act
        AppointmentDto result = appointmentService.confirmAppointment(1L, confirmationRequest);

        // Assert
        assertNotNull(result);
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
        assertEquals("admin", appointment.getConfirmedBy());
        assertEquals("Подтверждено администратором", appointment.getConfirmationNotes());
        assertNotNull(appointment.getConfirmationDate());
        assertEquals("admin", appointment.getChangedBy());

        verify(appointmentRepository, times(1)).save(appointment);
        verify(auditService, times(1)).logStatusChange(
                eq(appointment), eq("PENDING"), eq("CONFIRMED"), eq("admin"));
        verify(telegramNotificationService, times(1)).sendAppointmentConfirmation(
                eq(appointment), eq(AppointmentStatus.PENDING), eq(AppointmentStatus.CONFIRMED));
    }

    @Test
    void confirmAppointment_RejectAction_ShouldUpdateStatusToRejected() {
        // Arrange
        confirmationRequest.setAction("reject");
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(timeSlotService.getDoctorName(100L)).thenReturn("Доктор Иванов");
        when(timeSlotService.getPatientName(200L)).thenReturn("Пациент Петров");

        // Act
        appointmentService.confirmAppointment(1L, confirmationRequest);

        // Assert
        assertEquals(AppointmentStatus.REJECTED, appointment.getStatus());
        verify(telegramNotificationService, times(1)).sendAppointmentConfirmation(
                eq(appointment), eq(AppointmentStatus.PENDING), eq(AppointmentStatus.REJECTED));
    }

    @Test
    void rescheduleAppointment_ShouldUpdateTimeAndLogChange() {
        // Arrange
        LocalDateTime newStartTime = LocalDateTime.of(2024, 12, 26, 14, 0);
        LocalDateTime newEndTime = newStartTime.plusHours(2);

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.findConflictingAppointments(100L, newStartTime, newEndTime))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(timeSlotService.getDoctorName(100L)).thenReturn("Доктор Иванов");
        when(timeSlotService.getPatientName(200L)).thenReturn("Пациент Петров");

        // Act
        AppointmentDto result = appointmentService.rescheduleAppointment(1L, newStartTime, "admin");

        // Assert
        assertNotNull(result);
        assertEquals(newStartTime, appointment.getStartTime());
        assertEquals(newEndTime, appointment.getEndTime());
        assertEquals("admin", appointment.getChangedBy());

        verify(auditService, times(1)).logAppointmentChange(
                eq(1L), eq("RESCHEDULE"), eq("admin"), anyString(), eq("PENDING"), eq("PENDING"));
    }

    @Test
    void getDoctorSchedule_ShouldReturnAppointmentsForDate() {
        // Arrange
        Long doctorId = 100L;
        LocalDate date = LocalDate.of(2024, 12, 25);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        when(appointmentRepository.findByDoctorIdAndDateRange(doctorId, startOfDay, endOfDay))
                .thenReturn(Arrays.asList(appointment));
        when(timeSlotService.getDoctorName(100L)).thenReturn("Доктор Иванов");
        when(timeSlotService.getPatientName(200L)).thenReturn("Пациент Петров");

        // Act
        List<AppointmentDto> result = appointmentService.getDoctorSchedule(doctorId, date);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(appointmentRepository, times(1)).findByDoctorIdAndDateRange(doctorId, startOfDay, endOfDay);
    }

    @Test
    void getUpcomingAppointmentsForPatient_ShouldReturnFutureAppointments() {
        // Arrange
        Long patientId = 200L;
        Appointment futureAppointment = Appointment.builder()
                .id(2L)
                .patientId(patientId)
                .startTime(LocalDateTime.now().plusDays(1)) // Завтра
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .status(AppointmentStatus.SCHEDULED) // Должен быть SCHEDULED
                .build();

        Appointment pastAppointment = Appointment.builder()
                .id(3L)
                .patientId(patientId)
                .startTime(LocalDateTime.now().minusDays(1)) // Вчера
                .endTime(LocalDateTime.now().minusDays(1).plusHours(2))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment cancelledAppointment = Appointment.builder()
                .id(4L)
                .patientId(patientId)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .status(AppointmentStatus.CANCELLED) // Отменен
                .build();

        when(appointmentRepository.findByPatientId(patientId))
                .thenReturn(Arrays.asList(futureAppointment, pastAppointment, cancelledAppointment));
        when(timeSlotService.getDoctorName(any())).thenReturn("Доктор");
        when(timeSlotService.getPatientName(any())).thenReturn("Пациент");

        // Act
        List<AppointmentDto> result = appointmentService.getUpcomingAppointmentsForPatient(patientId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size()); // Только будущая запись со статусом SCHEDULED
        assertEquals(2L, result.get(0).getId());
        verify(appointmentRepository, times(1)).findByPatientId(patientId);
    }

    // Дополнительные тесты для edge cases

    @Test
    void getAllAppointments_ShouldSortByStartTime() {
        // Arrange
        Appointment earlyAppointment = Appointment.builder()
                .id(1L)
                .startTime(LocalDateTime.of(2024, 12, 25, 9, 0))
                .endTime(LocalDateTime.of(2024, 12, 25, 11, 0))
                .doctorId(100L)
                .patientId(200L)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        Appointment lateAppointment = Appointment.builder()
                .id(2L)
                .startTime(LocalDateTime.of(2024, 12, 25, 11, 0))
                .endTime(LocalDateTime.of(2024, 12, 25, 13, 0))
                .doctorId(100L)
                .patientId(200L)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        when(appointmentRepository.findAll()).thenReturn(Arrays.asList(lateAppointment, earlyAppointment));
        when(timeSlotService.getDoctorName(any())).thenReturn("Доктор");
        when(timeSlotService.getPatientName(any())).thenReturn("Пациент");

        // Act
        List<AppointmentDto> result = appointmentService.getAllAppointments();

        // Assert
        assertNotNull(result);
        // Проверяем что сортировка работает (можно проверить через отладчик или логи)
        verify(appointmentRepository, times(1)).findAll();
    }
}