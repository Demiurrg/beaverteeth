package com.beaverteeth.appointment.test;

import com.beaverteeth.appointment.model.dto.*;
import com.beaverteeth.appointment.service.AppointmentService;
import com.beaverteeth.appointment.service.TimeSlotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private TimeSlotService timeSlotService;

    @InjectMocks
    private AppointmentController appointmentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(appointmentController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void getAvailableTimeSlots_ShouldReturnSlots() throws Exception {
        // Arrange
        TimeSlotDto timeSlotDto = TimeSlotDto.builder()
                .startTime(LocalDateTime.of(2024, 12, 25, 9, 0))
                .endTime(LocalDateTime.of(2024, 12, 25, 11, 0))
                .doctorId(100L)
                .doctorName("Доктор Иванов")
                .build();

        List<TimeSlotDto> slots = Arrays.asList(timeSlotDto);
        when(timeSlotService.getAvailableTimeSlots(any(TimeSlotRequest.class)))
                .thenReturn(slots);

        // Act & Assert
        mockMvc.perform(get("/api/appointments/available-slots")
                        .param("date", "2024-12-25")
                        .param("doctorLastName", "Иванов"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].doctorId").value(100))
                .andExpect(jsonPath("$[0].doctorName").value("Доктор Иванов"));

        verify(timeSlotService, times(1)).getAvailableTimeSlots(any(TimeSlotRequest.class));
    }

    @Test
    void getAvailableTimeSlots_MissingParameters_ShouldReturnBadRequest() throws Exception {
        // Missing date parameter
        mockMvc.perform(get("/api/appointments/available-slots")
                        .param("doctorLastName", "Иванов"))
                .andExpect(status().isBadRequest());

        // Missing doctorLastName parameter
        mockMvc.perform(get("/api/appointments/available-slots")
                        .param("date", "2024-12-25"))
                .andExpect(status().isBadRequest());
    }
}