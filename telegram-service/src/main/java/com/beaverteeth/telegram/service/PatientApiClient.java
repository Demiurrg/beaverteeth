package com.beaverteeth.telegram.service;

import com.beaverteeth.telegram.model.dto.PatientDto;
import com.beaverteeth.telegram.model.dto.CreatePatientRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientApiClient {

    private final RestTemplate restTemplate;

    @Value("${patient.service.url:http://localhost:8082}")
    private String patientServiceUrl;

    // 1. Проверка существования пациента по Telegram username
    public PatientDto getPatientByTelegramUsername(String username) {
        try {
            String url = patientServiceUrl + "/api/patients/telegram/" + username;
            log.info("Поиск пациента по Telegram username: {}", username);

            return restTemplate.getForObject(url, PatientDto.class);

        } catch (Exception e) {
            log.info("Пациент с username {} не найден: {}", username, e.getMessage());
            return null;
        }
    }

    // 2. Создание нового пациента
    public PatientDto createPatient(CreatePatientRequest patientRequest) {
        try {
            String url = patientServiceUrl + "/api/patients";
            log.info("Создание нового пациента: {}", patientRequest.getFullName());

            return restTemplate.postForObject(url, patientRequest, PatientDto.class);

        } catch (Exception e) {
            log.error("Ошибка при создании пациента: {}", e.getMessage());
            throw new RuntimeException("Не удалось создать пациента: " + e.getMessage());
        }
    }

    // 3. Проверка занятости телефона
    public PatientDto getPatientByPhone(String phone) {
        try {
            String url = patientServiceUrl + "/api/patients/phone/" + phone;
            log.info("Поиск пациента по телефону: {}", phone);

            return restTemplate.getForObject(url, PatientDto.class);

        } catch (Exception e) {
            log.info("Пациент с телефоном {} не найден: {}", phone, e.getMessage());
            return null;
        }
    }
}