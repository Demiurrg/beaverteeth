package com.beaverteeth.telegram.service;

import com.beaverteeth.telegram.model.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${appointment.service.url:http://localhost:8083}")
    private String appointmentServiceUrl;

    @Value("${patient.service.url:http://localhost:8082}")
    private String patientServiceUrl;

    public List<TimeSlotDto> getAvailableTimeSlots(String doctorLastName, LocalDate date) {
        try {
            String formattedDate = date.format(DateTimeFormatter.ISO_DATE);
            String url = appointmentServiceUrl + "/api/appointments/available-slots" +
                    "?date=" + formattedDate + "&doctorLastName=" + doctorLastName;

            log.info("Запрос свободных слотов: {}", url);

            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<TimeSlotDto>>() {}
            ).getBody();

        } catch (Exception e) {
            log.error("Ошибка при получении свободных слотов для врача {} на дату {}: {}",
                    doctorLastName, date, e.getMessage());
            return List.of();
        }
    }

    public AppointmentDto createAppointment(Long doctorId, Long patientId,
                                            LocalDateTime startTime, String notes,
                                            Long patientChatId) {
        try {
            String url = appointmentServiceUrl + "/api/appointments";

            CreateAppointmentRequest request = new CreateAppointmentRequest();
            request.setDoctorId(doctorId);
            request.setPatientId(patientId);
            request.setStartTime(startTime);
            request.setNotes(notes != null ? notes : "Запись через Telegram бот");
            request.setPatientChatId(patientChatId);

            log.info("Создание записи: doctorId={}, patientId={}, time={}, chatId={}",
                    doctorId, patientId, startTime, patientChatId);

            return restTemplate.postForEntity(url, request, AppointmentDto.class).getBody();

        } catch (Exception e) {
            log.error("Ошибка при создании записи: {}", e.getMessage());
            return null;
        }
    }

    public PatientDto getPatientByPhone(String phone) {
        try {
            String url = patientServiceUrl + "/api/patients/phone/" + phone;
            log.info("Поиск пациента по телефону: {}", phone);

            return restTemplate.getForObject(url, PatientDto.class);

        } catch (Exception e) {
            log.error("Ошибка при поиске пациента по телефону {}: {}", phone, e.getMessage());
            return null;
        }
    }

    public PatientDto getPatientByTelegramUsername(String username) {
        try {
            String url = patientServiceUrl + "/api/patients/telegram/" + username;
            log.info("Поиск пациента по Telegram username: {}", username);

            return restTemplate.getForObject(url, PatientDto.class);

        } catch (Exception e) {
            log.info("Пациент с Telegram username {} не найден: {}", username, e.getMessage());
            return null;
        }
    }

    public Long getPatientIdByPhone(String phone) {
        PatientDto patient = getPatientByPhone(phone);
        return patient != null ? patient.getId() : null;
    }

    public Long getPatientIdByTelegramUsername(String username) {
        PatientDto patient = getPatientByTelegramUsername(username);
        return patient != null ? patient.getId() : null;
    }
}