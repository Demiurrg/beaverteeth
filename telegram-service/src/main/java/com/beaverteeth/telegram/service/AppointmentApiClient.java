package com.beaverteeth.telegram.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${appointment.service.url:http://localhost:8083}")
    private String appointmentServiceUrl;

    @Value("${patient.service.url:http://localhost:8082}")
    private String patientServiceUrl;

    public List<Map<String, Object>> getAvailableTimeSlots(String doctorLastName, LocalDate date) {
        try {
            String formattedDate = date.format(DateTimeFormatter.ISO_DATE);
            String url = appointmentServiceUrl + "/api/appointments/available-slots" +
                    "?date=" + formattedDate + "&doctorLastName=" + doctorLastName;

            log.info("Запрос свободных слотов: {}", url);

            ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);

            if (response.getBody() != null && response.getBody().length > 0) {
                return Arrays.asList(response.getBody());
            }

        } catch (Exception e) {
            log.error("Ошибка при получении свободных слотов для врача {} на дату {}: {}",
                    doctorLastName, date, e.getMessage());
        }

        return List.of();
    }

    public Map<String, Object> createAppointment(Long doctorId, Long patientId, LocalDateTime startTime, String notes) {
        try {
            String url = appointmentServiceUrl + "/api/appointments";

            Map<String, Object> request = Map.of(
                    "doctorId", doctorId,
                    "patientId", patientId,
                    "startTime", startTime.format(DateTimeFormatter.ISO_DATE_TIME),
                    "notes", notes != null ? notes : "Запись через Telegram бот"
            );

            log.info("Создание записи: doctorId={}, patientId={}, time={}",
                    doctorId, patientId, startTime);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            return response.getBody();

        } catch (Exception e) {
            log.error("Ошибка при создании записи: {}", e.getMessage());
            return Map.of("error", "Ошибка при создании записи: " + e.getMessage());
        }
    }

    public Long getPatientIdByPhone(String phone) {
        try {
            // Реальный запрос к patient-service для поиска пациента по телефону
            String url = patientServiceUrl + "/api/patients/phone/" + phone;
            log.info("Поиск пациента по телефону: {}", phone);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> patient = response.getBody();

            if (patient != null && patient.containsKey("id")) {
                Object id = patient.get("id");
                if (id instanceof Integer) {
                    return ((Integer) id).longValue();
                } else if (id instanceof Long) {
                    return (Long) id;
                } else if (id instanceof Number) {
                    return ((Number) id).longValue();
                }
            }

            log.warn("Пациент с телефоном {} не найден", phone);
            return null;

        } catch (Exception e) {
            log.error("Ошибка при поиске пациента по телефону {}: {}", phone, e.getMessage());
            return null;
        }
    }

    // Дополнительный метод для поиска пациента по Telegram username
    public Long getPatientIdByTelegramUsername(String username) {
        try {
            String url = patientServiceUrl + "/api/patients/telegram/" + username;
            log.info("Поиск пациента по Telegram username: {}", username);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> patient = response.getBody();

            if (patient != null && patient.containsKey("id")) {
                Object id = patient.get("id");
                if (id instanceof Number) {
                    return ((Number) id).longValue();
                }
            }

            log.warn("Пациент с Telegram username {} не найден", username);
            return null;

        } catch (Exception e) {
            log.info("Пациент с Telegram username {} не найден: {}", username, e.getMessage());
            return null;
        }
    }
}