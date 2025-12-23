package com.beaverteeth.patient.test;

import com.beaverteeth.patient.model.Patient;
import com.beaverteeth.patient.model.dto.CreatePatientRequest;
import com.beaverteeth.patient.model.dto.PatientDto;
import com.beaverteeth.patient.repository.PatientRepository;
import com.beaverteeth.patient.service.AuditService;
import com.beaverteeth.patient.service.PatientService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PatientService patientService;

    private Patient patient;
    private PatientDto patientDto;
    private CreatePatientRequest createRequest;

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .id(1L)
                .fullName("Иванов Иван Иванович")
                .age(30)
                .address("Москва, ул. Ленина, д. 1")
                .phone("+79123456789")
                .email("ivanov@example.com")
                .telegramUsername("@ivanov")
                .telegramChatId(123456789L)
                .notes("Аллергия на пенициллин")
                .isActive(true)
                .build();

        patientDto = PatientDto.builder()
                .id(1L)
                .fullName("Иванов Иван Иванович")
                .age(30)
                .address("Москва, ул. Ленина, д. 1")
                .phone("+79123456789")
                .email("ivanov@example.com")
                .telegramUsername("@ivanov")
                .telegramChatId(123456789L)
                .notes("Аллергия на пенициллин")
                .build();

        createRequest = CreatePatientRequest.builder()
                .fullName("Иванов Иван Иванович")
                .age(30)
                .address("Москва, ул. Ленина, д. 1")
                .phone("+79123456789")
                .email("ivanov@example.com")
                .telegramUsername("@ivanov")
                .notes("Аллергия на пенициллин")
                .createdBy("admin")
                .build();
    }

    @Test
    void createPatient_ShouldCreateAndReturnPatient() {
        // Arrange
        when(patientRepository.existsByPhone("+79123456789")).thenReturn(false);
        when(patientRepository.existsByEmail("ivanov@example.com")).thenReturn(false);
        when(patientRepository.existsByTelegramUsername("@ivanov")).thenReturn(false);
        when(modelMapper.map(createRequest, Patient.class)).thenReturn(patient);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(modelMapper.map(patient, PatientDto.class)).thenReturn(patientDto);

        // Act
        PatientDto result = patientService.createPatient(createRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Иванов Иван Иванович", result.getFullName());
        assertEquals("+79123456789", result.getPhone());

        verify(patientRepository, times(1)).existsByPhone("+79123456789");
        verify(patientRepository, times(1)).existsByEmail("ivanov@example.com");
        verify(patientRepository, times(1)).existsByTelegramUsername("@ivanov");
        verify(patientRepository, times(1)).save(any(Patient.class));
        verify(auditService, times(1)).logPatientChange(
                eq(1L), eq("CREATE"), eq("admin"), anyString());
    }

    @Test
    void createPatient_PhoneAlreadyExists_ShouldThrowException() {
        // Arrange
        when(patientRepository.existsByPhone("+79123456789")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            patientService.createPatient(createRequest);
        });

        assertEquals("Пациент с таким телефоном уже существует", exception.getMessage());
        verify(patientRepository, times(1)).existsByPhone("+79123456789");
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void updatePatientChatId_ShouldUpdateChatId() {
        // Arrange
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        // Act
        patientService.updatePatientChatId(1L, 987654321L, "user123");

        // Assert
        assertEquals(987654321L, patient.getTelegramChatId());
        assertEquals("user123", patient.getChangedBy());
        verify(patientRepository, times(1)).findById(1L);
        verify(patientRepository, times(1)).save(patient);
    }

    @Test
    void updatePatientChatId_PatientNotFound_ShouldThrowException() {
        // Arrange
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            patientService.updatePatientChatId(1L, 987654321L, "user123");
        });

        assertEquals("Пациент не найден с ID: 1", exception.getMessage());
        verify(patientRepository, times(1)).findById(1L);
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void getPatientWithChatId_ShouldReturnPatient() {
        // Arrange
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(modelMapper.map(patient, PatientDto.class)).thenReturn(patientDto);

        // Act
        PatientDto result = patientService.getPatientWithChatId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(123456789L, result.getTelegramChatId());
        verify(patientRepository, times(1)).findById(1L);
    }

    @Test
    void updatePatient_PhoneAlreadyExists_ShouldThrowException() {
        // Arrange
        PatientDto updatedDto = PatientDto.builder()
                .phone("+79999999999") // Другой телефон
                .build();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientRepository.existsByPhone("+79999999999")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            patientService.updatePatient(1L, updatedDto, "user123");
        });

        assertEquals("Пациент с таким телефоном уже существует", exception.getMessage());
        verify(patientRepository, times(1)).findById(1L);
        verify(patientRepository, times(1)).existsByPhone("+79999999999");
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void deletePatient_ShouldSetPatientInactive() {
        // Arrange
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        // Act
        patientService.deletePatient(1L, "user123");

        // Assert
        assertFalse(patient.getIsActive());
        assertEquals("user123", patient.getChangedBy());
        verify(patientRepository, times(1)).findById(1L);
        verify(patientRepository, times(1)).save(patient);
        verify(auditService, times(1)).logPatientChange(
                eq(1L), eq("DELETE"), eq("user123"), anyString());
    }

    @Test
    void getPatientById_ShouldReturnActivePatient() {
        // Arrange
        when(patientRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(patient));
        when(modelMapper.map(patient, PatientDto.class)).thenReturn(patientDto);

        // Act
        PatientDto result = patientService.getPatientById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(patientRepository, times(1)).findByIdAndIsActiveTrue(1L);
    }

    @Test
    void getPatientById_PatientNotFound_ShouldThrowException() {
        // Arrange
        when(patientRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            patientService.getPatientById(1L);
        });

        assertEquals("Пациент не найден с ID: 1", exception.getMessage());
        verify(patientRepository, times(1)).findByIdAndIsActiveTrue(1L);
    }

    @Test
    void getPatientByPhone_ShouldReturnPatient() {
        // Arrange
        when(patientRepository.findByPhone("+79123456789")).thenReturn(Optional.of(patient));
        when(modelMapper.map(patient, PatientDto.class)).thenReturn(patientDto);

        // Act
        PatientDto result = patientService.getPatientByPhone("+79123456789");

        // Assert
        assertNotNull(result);
        assertEquals("+79123456789", result.getPhone());
        verify(patientRepository, times(1)).findByPhone("+79123456789");
    }

    @Test
    void getPatientByPhone_PatientInactive_ShouldThrowException() {
        // Arrange
        patient.setIsActive(false);
        when(patientRepository.findByPhone("+79123456789")).thenReturn(Optional.of(patient));

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            patientService.getPatientByPhone("+79123456789");
        });

        assertEquals("Пациент не активен", exception.getMessage());
        verify(patientRepository, times(1)).findByPhone("+79123456789");
    }

    @Test
    void getAllPatients_ShouldReturnActivePatients() {
        // Arrange
        List<Patient> patients = Arrays.asList(patient);
        when(patientRepository.findByIsActiveTrue()).thenReturn(patients);
        when(modelMapper.map(patient, PatientDto.class)).thenReturn(patientDto);

        // Act
        List<PatientDto> result = patientService.getAllPatients();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(patientRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    void searchPatientsByName_ShouldReturnMatchingPatients() {
        // Arrange
        List<Patient> patients = Arrays.asList(patient);
        when(patientRepository.findByFullNameContainingIgnoreCase("Иванов")).thenReturn(patients);
        when(modelMapper.map(patient, PatientDto.class)).thenReturn(patientDto);

        // Act
        List<PatientDto> result = patientService.searchPatientsByName("Иванов");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Иванов Иван Иванович", result.get(0).getFullName());
        verify(patientRepository, times(1)).findByFullNameContainingIgnoreCase("Иванов");
    }

    @Test
    void convertToDTO_ShouldConvertPatientToDTO() {
        // Arrange
        when(modelMapper.map(patient, PatientDto.class)).thenReturn(patientDto);

        // Act
        PatientDto result = patientService.convertToDTO(patient);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Иванов Иван Иванович", result.getFullName());
        verify(modelMapper, times(1)).map(patient, PatientDto.class);
    }
}