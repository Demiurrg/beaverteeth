package com.beaverteeth.telegram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientApiClient {

    private final RestTemplate restTemplate;

    @Value("${patient.service.url:http://localhost:8082}")
    private String patientServiceUrl;

    // 1. Проверка существования пациента по Telegram username
    public Map<String, Object> getPatientByTelegramUsername(String username) {
        try {
            String url = patientServiceUrl + "/api/patients/telegram/" + username;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.info("Пациент с username {} не найден: {}", username, e.getMessage());
            return null;
        }
    }

    // 2. Создание нового пациента
    public Map<String, Object> createPatient(Map<String, Object> patientData) {
        try {
            String url = patientServiceUrl + "/api/patients";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, patientData, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка при создании пациента: {}", e.getMessage());
            throw new RuntimeException("Не удалось создать пациента");
        }
    }

    // 3. Проверка занятости телефона (опционально)
    public Map<String, Object> getPatientByPhone(String phone) {
        try {
            String url = patientServiceUrl + "/api/patients/phone/" + phone;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.info("Пациент с телефоном {} не найден: {}", phone, e.getMessage());
            return null;
        }
    }
}