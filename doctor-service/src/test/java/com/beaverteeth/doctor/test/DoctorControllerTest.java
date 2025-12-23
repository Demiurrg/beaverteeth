package com.beaverteeth.doctor.test;

import com.beaverteeth.doctor.model.dto.CreateDoctorRequest;
import com.beaverteeth.doctor.model.dto.DoctorDto;
import com.beaverteeth.doctor.model.Specialty;
import com.beaverteeth.doctor.service.DoctorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DoctorControllerTest {

    @Mock
    private DoctorService doctorService;

    @InjectMocks
    private DoctorController doctorController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private DoctorDto doctorDto;
    private CreateDoctorRequest createRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(doctorController).build();
        objectMapper = new ObjectMapper();

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
    void createDoctor_ShouldReturnCreatedDoctor() throws Exception {
        when(doctorService.createDoctor(any(CreateDoctorRequest.class))).thenReturn(doctorDto);

        mockMvc.perform(post("/api/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Иванов Иван Иванович"))
                .andExpect(jsonPath("$.specialty").value("THERAPIST"));

        verify(doctorService, times(1)).createDoctor(any(CreateDoctorRequest.class));
    }

    @Test
    void getDoctor_ShouldReturnDoctor() throws Exception {
        when(doctorService.getDoctorById(1L)).thenReturn(doctorDto);

        mockMvc.perform(get("/api/doctors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Иванов Иван Иванович"));

        verify(doctorService, times(1)).getDoctorById(1L);
    }

    @Test
    void getAllDoctors_ShouldReturnAllDoctors() throws Exception {
        List<DoctorDto> doctors = Arrays.asList(doctorDto);
        when(doctorService.getAllDoctors()).thenReturn(doctors);

        mockMvc.perform(get("/api/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Иванов Иван Иванович"));

        verify(doctorService, times(1)).getAllDoctors();
    }

    @Test
    void searchDoctors_ShouldReturnMatchingDoctors() throws Exception {
        List<DoctorDto> doctors = Arrays.asList(doctorDto);
        when(doctorService.searchDoctorsByLastName("Иванов")).thenReturn(doctors);

        mockMvc.perform(get("/api/doctors/search")
                        .param("lastName", "Иванов"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Иванов Иван Иванович"));

        verify(doctorService, times(1)).searchDoctorsByLastName("Иванов");
    }

    @Test
    void getDoctorsBySpecialty_ShouldReturnSpecialtyDoctors() throws Exception {
        List<DoctorDto> doctors = Arrays.asList(doctorDto);
        when(doctorService.getDoctorsBySpecialty(Specialty.THERAPIST)).thenReturn(doctors);

        mockMvc.perform(get("/api/doctors/specialty/THERAPIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].specialty").value("THERAPIST"));

        verify(doctorService, times(1)).getDoctorsBySpecialty(Specialty.THERAPIST);
    }

    @Test
    void updateDoctor_ShouldReturnUpdatedDoctor() throws Exception {
        when(doctorService.updateDoctor(eq(1L), any(DoctorDto.class), eq("user123")))
                .thenReturn(doctorDto);

        mockMvc.perform(put("/api/doctors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doctorDto))
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(doctorService, times(1)).updateDoctor(eq(1L), any(DoctorDto.class), eq("user123"));
    }

    @Test
    void deleteDoctor_ShouldReturnNoContent() throws Exception {
        doNothing().when(doctorService).deleteDoctor(1L, "user123");

        mockMvc.perform(delete("/api/doctors/1")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isNoContent());

        verify(doctorService, times(1)).deleteDoctor(1L, "user123");
    }
}