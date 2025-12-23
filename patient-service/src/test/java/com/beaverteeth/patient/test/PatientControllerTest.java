package com.beaverteeth.patient.test;

import com.beaverteeth.patient.model.dto.CreatePatientRequest;
import com.beaverteeth.patient.model.dto.PatientDto;
import com.beaverteeth.patient.service.PatientService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    @Mock
    private PatientService patientService;

    @InjectMocks
    private PatientController patientController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private PatientDto patientDto;
    private CreatePatientRequest createRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(patientController).build();
        objectMapper = new ObjectMapper();

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
    void createPatient_ShouldReturnCreatedPatient() throws Exception {
        when(patientService.createPatient(any(CreatePatientRequest.class))).thenReturn(patientDto);

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Иванов Иван Иванович"))
                .andExpect(jsonPath("$.phone").value("+79123456789"));

        verify(patientService, times(1)).createPatient(any(CreatePatientRequest.class));
    }

    @Test
    void getPatient_ShouldReturnPatient() throws Exception {
        when(patientService.getPatientById(1L)).thenReturn(patientDto);

        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Иванов Иван Иванович"));

        verify(patientService, times(1)).getPatientById(1L);
    }

    @Test
    void updatePatientChatId_ShouldUpdateChatId() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("telegramChatId", 123456789L);

        doNothing().when(patientService).updatePatientChatId(eq(1L), eq(123456789L), eq("user123"));

        mockMvc.perform(put("/api/patients/1/chat-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk());

        verify(patientService, times(1)).updatePatientChatId(1L, 123456789L, "user123");
    }

    @Test
    void getPatientWithChatId_ShouldReturnPatientWithChatId() throws Exception {
        when(patientService.getPatientWithChatId(1L)).thenReturn(patientDto);

        mockMvc.perform(get("/api/patients/1/with-chat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramChatId").value(123456789L));

        verify(patientService, times(1)).getPatientWithChatId(1L);
    }

    @Test
    void getPatientByPhone_ShouldReturnPatient() throws Exception {
        when(patientService.getPatientByPhone("+79123456789")).thenReturn(patientDto);

        mockMvc.perform(get("/api/patients/phone/{phone}", "+79123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+79123456789"));

        verify(patientService, times(1)).getPatientByPhone("+79123456789");
    }

    @Test
    void getPatientByTelegramUsername_ShouldReturnPatient() throws Exception {
        when(patientService.getPatientByTelegramUsername("@ivanov")).thenReturn(patientDto);

        mockMvc.perform(get("/api/patients/telegram/{username}", "@ivanov"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramUsername").value("@ivanov"));

        verify(patientService, times(1)).getPatientByTelegramUsername("@ivanov");
    }

    @Test
    void getAllPatients_ShouldReturnAllPatients() throws Exception {
        List<PatientDto> patients = Arrays.asList(patientDto);
        when(patientService.getAllPatients()).thenReturn(patients);

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Иванов Иван Иванович"));

        verify(patientService, times(1)).getAllPatients();
    }

    @Test
    void searchPatients_ShouldReturnMatchingPatients() throws Exception {
        List<PatientDto> patients = Arrays.asList(patientDto);
        when(patientService.searchPatientsByName("Иванов")).thenReturn(patients);

        mockMvc.perform(get("/api/patients/search")
                        .param("name", "Иванов"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Иванов Иван Иванович"));

        verify(patientService, times(1)).searchPatientsByName("Иванов");
    }

    @Test
    void updatePatient_ShouldReturnUpdatedPatient() throws Exception {
        when(patientService.updatePatient(eq(1L), any(PatientDto.class), eq("user123")))
                .thenReturn(patientDto);

        mockMvc.perform(put("/api/patients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientDto))
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(patientService, times(1)).updatePatient(eq(1L), any(PatientDto.class), eq("user123"));
    }

    @Test
    void deletePatient_ShouldReturnNoContent() throws Exception {
        doNothing().when(patientService).deletePatient(1L, "user123");

        mockMvc.perform(delete("/api/patients/1")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isNoContent());

        verify(patientService, times(1)).deletePatient(1L, "user123");
    }

    @Test
    void createPatient_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        CreatePatientRequest invalidRequest = CreatePatientRequest.builder()
                .fullName("") // Пустое имя
                .age(-5) // Отрицательный возраст
                .phone("invalid") // Невалидный телефон
                .build();

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}