package com.beaverteeth.appointment.test;

import com.beaverteeth.appointment.model.Appointment;
import com.beaverteeth.appointment.model.AppointmentStatus;
import com.beaverteeth.appointment.model.dto.TimeSlotDto;
import com.beaverteeth.appointment.model.dto.TimeSlotRequest;
import com.beaverteeth.appointment.repository.AppointmentRepository;
import com.beaverteeth.appointment.service.TimeSlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TimeSlotService timeSlotService;

    private TimeSlotRequest timeSlotRequest;

    @BeforeEach
    void setUp() {
        // Set URL через рефлексию, так как они с @Value
        setField(timeSlotService, "doctorServiceUrl", "http://doctor-service");
        setField(timeSlotService, "patientServiceUrl", "http://patient-service");

        timeSlotRequest = TimeSlotRequest.builder()
                .date(LocalDate.of(2024, 12, 25))
                .doctorLastName("Иванов")
                .build();
    }

    private void setField(Object object, String fieldName, Object value) {
        try {
            var field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void isDoctorOnVacation_ShouldReturnTrueWhenDoctorOnVacation() {
        // Arrange
        Long doctorId = 100L;
        LocalDate date = LocalDate.of(2024, 12, 25);
        String url = "http://doctor-service/api/vacations/doctor/100/check?date=2024-12-25";

        when(restTemplate.getForEntity(url, Boolean.class))
                .thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        // Act
        boolean result = timeSlotService.isDoctorOnVacation(doctorId, date);

        // Assert
        assertTrue(result);
        verify(restTemplate, times(1)).getForEntity(url, Boolean.class);
    }

    @Test
    void isDoctorOnVacation_ShouldReturnFalseWhenDoctorNotOnVacation() {
        // Arrange
        Long doctorId = 100L;
        LocalDate date = LocalDate.of(2024, 12, 25);
        String url = "http://doctor-service/api/vacations/doctor/100/check?date=2024-12-25";

        when(restTemplate.getForEntity(url, Boolean.class))
                .thenReturn(new ResponseEntity<>(false, HttpStatus.OK));

        // Act
        boolean result = timeSlotService.isDoctorOnVacation(doctorId, date);

        // Assert
        assertFalse(result);
    }

    @Test
    void isDoctorOnVacation_ServiceUnavailable_ShouldReturnFalse() {
        // Arrange
        Long doctorId = 100L;
        LocalDate date = LocalDate.of(2024, 12, 25);
        String url = "http://doctor-service/api/vacations/doctor/100/check?date=2024-12-25";

        when(restTemplate.getForEntity(url, Boolean.class))
                .thenThrow(new RestClientException("Service unavailable"));

        // Act
        boolean result = timeSlotService.isDoctorOnVacation(doctorId, date);

        // Assert
        assertFalse(result); // При недоступности сервиса считаем, что врач не в отпуске
        verify(restTemplate, times(1)).getForEntity(url, Boolean.class);
    }

    @Test
    void getPatientChatId_ShouldReturnChatId() {
        // Arrange
        Long patientId = 200L;
        String url = "http://patient-service/api/patients/200/with-chat";
        Map<String, Object> responseBody = Map.of("telegramChatId", 123456789L);

        when(restTemplate.getForEntity(url, Map.class))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        // Act
        Long result = timeSlotService.getPatientChatId(patientId);

        // Assert
        assertEquals(123456789L, result);
        verify(restTemplate, times(1)).getForEntity(url, Map.class);
    }

    @Test
    void getPatientChatId_NoChatId_ShouldReturnNull() {
        // Arrange
        Long patientId = 200L;
        String url = "http://patient-service/api/patients/200/with-chat";
        Map<String, Object> responseBody = Map.of("fullName", "Иванов Иван");

        when(restTemplate.getForEntity(url, Map.class))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        // Act
        Long result = timeSlotService.getPatientChatId(patientId);

        // Assert
        assertNull(result);
    }

    @Test
    void getPatientChatId_ServiceUnavailable_ShouldReturnNull() {
        // Arrange
        Long patientId = 200L;
        String url = "http://patient-service/api/patients/200/with-chat";

        when(restTemplate.getForEntity(url, Map.class))
                .thenThrow(new RestClientException("Service unavailable"));

        // Act
        Long result = timeSlotService.getPatientChatId(patientId);

        // Assert
        assertNull(result);
    }

    @Test
    void getAvailableTimeSlots_DoctorOnVacation_ShouldReturnEmptyList() {
        // Arrange
        String searchUrl = "http://doctor-service/api/doctors/search?lastName=Иванов";
        Map<String, Object>[] doctors = new Map[]{Map.of("id", 100L, "fullName", "Иванов Иван Иванович")};

        when(restTemplate.getForEntity(searchUrl, Map[].class))
                .thenReturn(new ResponseEntity<>(doctors, HttpStatus.OK));

        String vacationUrl = "http://doctor-service/api/vacations/doctor/100/check?date=2024-12-25";
        when(restTemplate.getForEntity(vacationUrl, Boolean.class))
                .thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        // Act
        List<TimeSlotDto> result = timeSlotService.getAvailableTimeSlots(timeSlotRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Должен вернуть пустой список
        verify(appointmentRepository, never()).findByDoctorIdAndDateRange(any(), any(), any());
    }

    @Test
    void getAvailableTimeSlots_DoctorNotFound_ShouldReturnEmptyList() {
        // Arrange
        String searchUrl = "http://doctor-service/api/doctors/search?lastName=Иванов";
        when(restTemplate.getForEntity(searchUrl, Map[].class))
                .thenReturn(new ResponseEntity<>(new Map[0], HttpStatus.OK));

        // Act
        List<TimeSlotDto> result = timeSlotService.getAvailableTimeSlots(timeSlotRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(restTemplate, times(1)).getForEntity(searchUrl, Map[].class);
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    void findDoctorIdByLastName_ShouldReturnDoctorId() {
        // Arrange
        String searchUrl = "http://doctor-service/api/doctors/search?lastName=Иванов";
        Map<String, Object>[] doctors = new Map[]{
                Map.of("id", 100L, "fullName", "Иванов Иван Иванович"),
                Map.of("id", 101L, "fullName", "Иванов Петр Петрович")
        };

        when(restTemplate.getForEntity(searchUrl, Map[].class))
                .thenReturn(new ResponseEntity<>(doctors, HttpStatus.OK));

        // Act
        Long result = timeSlotService.findDoctorIdByLastName("Иванов");

        // Assert
        assertEquals(100L, result); // Возвращает первого врача
        verify(restTemplate, times(1)).getForEntity(searchUrl, Map[].class);
    }

    @Test
    void findDoctorIdByLastName_NoDoctorsFound_ShouldReturnNull() {
        // Arrange
        String searchUrl = "http://doctor-service/api/doctors/search?lastName=Иванов";
        when(restTemplate.getForEntity(searchUrl, Map[].class))
                .thenReturn(new ResponseEntity<>(new Map[0], HttpStatus.OK));

        // Act
        Long result = timeSlotService.findDoctorIdByLastName("Иванов");

        // Assert
        assertNull(result);
    }

    @Test
    void getDoctorName_ShouldReturnDoctorName() {
        // Arrange
        Long doctorId = 100L;
        String url = "http://doctor-service/api/doctors/100";
        Map<String, Object> responseBody = Map.of("fullName", "Иванов Иван Иванович");

        when(restTemplate.getForEntity(url, Map.class))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        // Act
        String result = timeSlotService.getDoctorName(doctorId);

        // Assert
        assertEquals("Иванов Иван Иванович", result);
    }

    @Test
    void getDoctorName_ServiceUnavailable_ShouldReturnDefaultName() {
        // Arrange
        Long doctorId = 100L;
        String url = "http://doctor-service/api/doctors/100";

        when(restTemplate.getForEntity(url, Map.class))
                .thenThrow(new RestClientException("Service unavailable"));

        // Act
        String result = timeSlotService.getDoctorName(doctorId);

        // Assert
        assertEquals("Врач ID: 100", result);
    }

    @Test
    void getPatientName_ShouldReturnPatientName() {
        // Arrange
        Long patientId = 200L;
        String url = "http://patient-service/api/patients/200";
        Map<String, Object> responseBody = Map.of("fullName", "Петров Петр Петрович");

        when(restTemplate.getForEntity(url, Map.class))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        // Act
        String result = timeSlotService.getPatientName(patientId);

        // Assert
        assertEquals("Петров Петр Петрович", result);
    }

    @Test
    void validateDoctorExists_DoctorExists_ShouldNotThrowException() {
        // Arrange
        Long doctorId = 100L;
        String url = "http://doctor-service/api/doctors/100";

        when(restTemplate.getForEntity(url, Map.class))
                .thenReturn(new ResponseEntity<>(Map.of("id", 100L), HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> timeSlotService.validateDoctorExists(doctorId));
        verify(restTemplate, times(1)).getForEntity(url, Map.class);
    }

    @Test
    void validateDoctorExists_DoctorNotFound_ShouldThrowException() {
        // Arrange
        Long doctorId = 999L;
        String url = "http://doctor-service/api/doctors/999";

        when(restTemplate.getForEntity(url, Map.class))
                .thenThrow(new RestClientException("Doctor not found"));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            timeSlotService.validateDoctorExists(doctorId);
        });

        assertEquals("Врач с ID 999 не найден", exception.getMessage());
    }

    @Test
    void validatePatientExists_PatientExists_ShouldNotThrowException() {
        // Arrange
        Long patientId = 200L;
        String url = "http://patient-service/api/patients/200";

        when(restTemplate.getForEntity(url, Map.class))
                .thenReturn(new ResponseEntity<>(Map.of("id", 200L), HttpStatus.OK));

        // Act & Assert
        assertDoesNotThrow(() -> timeSlotService.validatePatientExists(patientId));
        verify(restTemplate, times(1)).getForEntity(url, Map.class);
    }

    @Test
    void validatePatientExists_PatientNotFound_ShouldThrowException() {
        // Arrange
        Long patientId = 999L;
        String url = "http://patient-service/api/patients/999";

        when(restTemplate.getForEntity(url, Map.class))
                .thenThrow(new RestClientException("Patient not found"));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            timeSlotService.validatePatientExists(patientId);
        });

        assertEquals("Пациент с ID 999 не найден", exception.getMessage());
    }
}