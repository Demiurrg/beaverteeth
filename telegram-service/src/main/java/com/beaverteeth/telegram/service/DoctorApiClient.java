package com.beaverteeth.telegram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorApiClient {

    private final RestTemplate restTemplate;

    @Value("${doctor.service.url:http://localhost:8081}")
    private String doctorServiceUrl;

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllActiveDoctors() {
        try {
            String url = doctorServiceUrl + "/api/doctors";
            log.info("Запрос всех врачей: {}", url);

            ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);

            if (response.getBody() != null) {
                List<Map<String, Object>> activeDoctors = new ArrayList<>();

                for (Map<String, Object> doctor : response.getBody()) {
                    Boolean isActive = (Boolean) doctor.get("isActive");
                    if (isActive != null && isActive) {
                        activeDoctors.add(doctor);
                    }
                }

                return activeDoctors;
            }

        } catch (Exception e) {
            log.error("Ошибка при получении списка врачей: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDoctorsBySpecialty(String specialty) {
        try {
            String url = doctorServiceUrl + "/api/doctors/specialty/" + specialty;
            log.info("Запрос врачей по специализации {}: {}", specialty, url);

            ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);

            if (response.getBody() != null) {
                List<Map<String, Object>> activeDoctors = new ArrayList<>();

                for (Map<String, Object> doctor : response.getBody()) {
                    Boolean isActive = (Boolean) doctor.get("isActive");
                    if (isActive != null && isActive) {
                        activeDoctors.add(doctor);
                    }
                }

                return activeDoctors;
            }

        } catch (Exception e) {
            log.error("Ошибка при получении врачей по специализации {}: {}", specialty, e.getMessage());
        }

        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getDoctorById(Long doctorId) {
        try {
            String url = doctorServiceUrl + "/api/doctors/" + doctorId;
            log.info("Запрос врача по ID {}: {}", doctorId, url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();

        } catch (Exception e) {
            log.error("Ошибка при получении врача по ID {}: {}", doctorId, e.getMessage());
            return null;
        }
    }

    public Long findDoctorIdByLastName(String lastName) {
        try {
            // Используем новый эндпоинт для поиска
            String url = doctorServiceUrl + "/api/doctors/search?lastName=" + lastName;
            log.info("Поиск врача по фамилии через API: {}", lastName);

            ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);
            Map<String, Object>[] doctors = response.getBody();

            if (doctors != null && doctors.length > 0) {
                // Возвращаем ID первого найденного врача
                for (Map<String, Object> doctor : doctors) {
                    Boolean isActive = (Boolean) doctor.get("isActive");
                    if (isActive != null && isActive) {
                        Object id = doctor.get("id");
                        if (id instanceof Integer) {
                            return ((Integer) id).longValue();
                        } else if (id instanceof Long) {
                            return (Long) id;
                        } else if (id instanceof Number) {
                            return ((Number) id).longValue();
                        }
                    }
                }
            }

            log.warn("Врач с фамилией {} не найден", lastName);
            return null;

        } catch (Exception e) {
            log.error("Ошибка при поиске врача по фамилии {}: {}", lastName, e.getMessage());
            return null;
        }
    }

    // Дополнительный метод для получения списка врачей
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchDoctorsByLastName(String lastName) {
        try {
            String url = doctorServiceUrl + "/api/doctors/search?lastName=" + lastName;
            log.info("Поиск врачей по фамилии: {}", lastName);

            ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);

            if (response.getBody() != null) {
                List<Map<String, Object>> activeDoctors = new ArrayList<>();

                for (Map<String, Object> doctor : response.getBody()) {
                    Boolean isActive = (Boolean) doctor.get("isActive");
                    if (isActive != null && isActive) {
                        activeDoctors.add(doctor);
                    }
                }

                return activeDoctors;
            }

        } catch (Exception e) {
            log.error("Ошибка при поиске врачей по фамилии {}: {}", lastName, e.getMessage());
        }

        return Collections.emptyList();
    }
}