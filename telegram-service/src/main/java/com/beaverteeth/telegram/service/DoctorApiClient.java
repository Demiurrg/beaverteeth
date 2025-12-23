package com.beaverteeth.telegram.service;

import com.beaverteeth.telegram.model.dto.DoctorDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorApiClient {

    private final RestTemplate restTemplate;

    @Value("${doctor.service.url:http://localhost:8081}")
    private String doctorServiceUrl;

    public List<DoctorDto> getAllActiveDoctors() {
        try {
            String url = doctorServiceUrl + "/api/doctors";
            log.info("Запрос всех врачей: {}", url);

            List<DoctorDto> allDoctors = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<DoctorDto>>() {}
            ).getBody();

            if (allDoctors != null) {
                return allDoctors.stream()
                        .filter(doctor -> doctor.getIsActive() != null && doctor.getIsActive())
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            log.error("Ошибка при получении списка врачей: {}", e.getMessage());
        }

        return List.of();
    }

    public List<DoctorDto> getDoctorsBySpecialty(String specialty) {
        try {
            String url = doctorServiceUrl + "/api/doctors/specialty/" + specialty;
            log.info("Запрос врачей по специализации {}: {}", specialty, url);

            List<DoctorDto> doctors = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<DoctorDto>>() {}
            ).getBody();

            if (doctors != null) {
                return doctors.stream()
                        .filter(doctor -> doctor.getIsActive() != null && doctor.getIsActive())
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            log.error("Ошибка при получении врачей по специализации {}: {}", specialty, e.getMessage());
        }

        return List.of();
    }

    public DoctorDto getDoctorById(Long doctorId) {
        try {
            String url = doctorServiceUrl + "/api/doctors/" + doctorId;
            log.info("Запрос врача по ID {}: {}", doctorId, url);

            return restTemplate.getForObject(url, DoctorDto.class);

        } catch (Exception e) {
            log.error("Ошибка при получении врача по ID {}: {}", doctorId, e.getMessage());
            return null;
        }
    }

    public Long findDoctorIdByLastName(String lastName) {
        try {
            String url = doctorServiceUrl + "/api/doctors/search?lastName=" + lastName;
            log.info("Поиск врача по фамилии через API: {}", lastName);

            List<DoctorDto> doctors = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<DoctorDto>>() {}
            ).getBody();

            if (doctors != null && !doctors.isEmpty()) {
                return doctors.stream()
                        .filter(doctor -> doctor.getIsActive() != null && doctor.getIsActive())
                        .findFirst()
                        .map(DoctorDto::getId)
                        .orElse(null);
            }

            log.warn("Врач с фамилией {} не найден", lastName);
            return null;

        } catch (Exception e) {
            log.error("Ошибка при поиске врача по фамилии {}: {}", lastName, e.getMessage());
            return null;
        }
    }

    public List<DoctorDto> searchDoctorsByLastName(String lastName) {
        try {
            String url = doctorServiceUrl + "/api/doctors/search?lastName=" + lastName;
            log.info("Поиск врачей по фамилии: {}", lastName);

            List<DoctorDto> doctors = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<DoctorDto>>() {}
            ).getBody();

            if (doctors != null) {
                return doctors.stream()
                        .filter(doctor -> doctor.getIsActive() != null && doctor.getIsActive())
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            log.error("Ошибка при поиске врачей по фамилии {}: {}", lastName, e.getMessage());
        }

        return List.of();
    }
}