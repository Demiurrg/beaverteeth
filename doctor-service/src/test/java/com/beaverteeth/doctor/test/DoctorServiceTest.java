package com.beaverteeth.doctor.test;

import com.beaverteeth.doctor.model.Doctor;
import com.beaverteeth.doctor.model.dto.CreateDoctorRequest;
import com.beaverteeth.doctor.model.dto.DoctorDto;
import com.beaverteeth.doctor.model.Specialty;
import com.beaverteeth.doctor.repository.DoctorRepository;
import com.beaverteeth.doctor.service.AuditService;
import com.beaverteeth.doctor.service.DoctorService;
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
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DoctorService doctorService;

    private Doctor doctor;
    private DoctorDto doctorDto;
    private CreateDoctorRequest createRequest;

    @BeforeEach
    void setUp() {
        doctor = Doctor.builder()
                .id(1L)
                .fullName("Иванов Иван Иванович")
                .totalExperience(10)
                .clinicExperience(5)
                .specialty(Specialty.THERAPIST)
                .education("Медицинский университет")
                .certificates("Сертификат кардиолога")
                .isActive(true)
                .build();

        doctorDto = DoctorDto.builder()
                .id(1L)
                .fullName("Иванов Иван Иванович")
                .totalExperience(10)
                .clinicExperience(5)
                .specialty(Specialty.THERAPIST)
                .education("Медицинский университет")
                .certificates("Сертификат кардиолога")
                .build();

        createRequest = CreateDoctorRequest.builder()
                .fullName("Иванов Иван Иванович")
                .totalExperience(10)
                .clinicExperience(5)
                .specialty(Specialty.THERAPIST)
                .education("Медицинский университет")
                .certificates("Сертификат кардиолога")
                .createdBy("admin")
                .build();
    }

    @Test
    void createDoctor_ShouldCreateAndReturnDoctor() {
        // Arrange
        when(modelMapper.map(createRequest, Doctor.class)).thenReturn(doctor);
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);
        when(modelMapper.map(doctor, DoctorDto.class)).thenReturn(doctorDto);

        // Act
        DoctorDto result = doctorService.createDoctor(createRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Иванов Иван Иванович", result.getFullName());
        assertEquals(Specialty.THERAPIST, result.getSpecialty());

        verify(doctorRepository, times(1)).save(any(Doctor.class));
        verify(auditService, times(1)).logDoctorChange(
                eq(1L), eq("CREATE"), eq("admin"), anyString());
    }

    @Test
    void getDoctorById_ShouldReturnDoctorWhenFound() {
        // Arrange
        when(doctorRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(doctor));
        when(modelMapper.map(doctor, DoctorDto.class)).thenReturn(doctorDto);

        // Act
        DoctorDto result = doctorService.getDoctorById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(doctorRepository, times(1)).findByIdAndIsActiveTrue(1L);
    }

    @Test
    void getDoctorById_ShouldThrowExceptionWhenNotFound() {
        // Arrange
        when(doctorRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            doctorService.getDoctorById(1L);
        });
        verify(doctorRepository, times(1)).findByIdAndIsActiveTrue(1L);
    }

    @Test
    void getAllDoctors_ShouldReturnActiveDoctors() {
        // Arrange
        List<Doctor> doctors = Arrays.asList(doctor);
        when(doctorRepository.findByIsActiveTrue()).thenReturn(doctors);
        when(modelMapper.map(doctor, DoctorDto.class)).thenReturn(doctorDto);

        // Act
        List<DoctorDto> result = doctorService.getAllDoctors();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(doctorRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    void getDoctorsBySpecialty_ShouldReturnDoctors() {
        // Arrange
        List<Doctor> doctors = Arrays.asList(doctor);
        when(doctorRepository.findBySpecialtyAndIsActiveTrue(Specialty.THERAPIST))
                .thenReturn(doctors);
        when(modelMapper.map(doctor, DoctorDto.class)).thenReturn(doctorDto);

        // Act
        List<DoctorDto> result = doctorService.getDoctorsBySpecialty(Specialty.THERAPIST);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Specialty.THERAPIST, result.get(0).getSpecialty());
        verify(doctorRepository, times(1)).findBySpecialtyAndIsActiveTrue(Specialty.THERAPIST);
    }

    @Test
    void searchDoctorsByLastName_ShouldReturnMatchingDoctors() {
        // Arrange
        List<Doctor> doctors = Arrays.asList(doctor);
        when(doctorRepository.findByLastNameContainingIgnoreCaseAndIsActiveTrue("Иванов"))
                .thenReturn(doctors);
        when(modelMapper.map(doctor, DoctorDto.class)).thenReturn(doctorDto);

        // Act
        List<DoctorDto> result = doctorService.searchDoctorsByLastName("Иванов");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Иванов Иван Иванович", result.get(0).getFullName());
        verify(doctorRepository, times(1))
                .findByLastNameContainingIgnoreCaseAndIsActiveTrue("Иванов");
    }

    @Test
    void deleteDoctor_ShouldSetDoctorInactive() {
        // Arrange
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);

        // Act
        doctorService.deleteDoctor(1L, "user123");

        // Assert
        assertFalse(doctor.getIsActive());
        assertEquals("user123", doctor.getChangedBy());
        verify(doctorRepository, times(1)).findById(1L);
        verify(doctorRepository, times(1)).save(doctor);
        verify(auditService, times(1)).logDoctorChange(
                eq(1L), eq("DELETE"), eq("user123"), anyString());
    }

    @Test
    void convertToDTO_ShouldConvertDoctorToDTO() {
        // Arrange
        when(modelMapper.map(doctor, DoctorDto.class)).thenReturn(doctorDto);

        // Act
        DoctorDto result = doctorService.convertToDTO(doctor);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Иванов Иван Иванович", result.getFullName());
        verify(modelMapper, times(1)).map(doctor, DoctorDto.class);
    }
}