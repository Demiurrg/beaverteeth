package com.beaverteeth.appointment.service;

import com.beaverteeth.appointment.model.Appointment;
import com.beaverteeth.appointment.model.dto.TimeSlotDTO;
import com.beaverteeth.appointment.model.dto.TimeSlotRequest;
import com.beaverteeth.appointment.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeSlotService {

    private final AppointmentRepository appointmentRepository;
    private final RestTemplate restTemplate;

    @Value("${doctor.service.url:http://localhost:8081}")
    private String doctorServiceUrl;

    @Value("${patient.service.url:http://localhost:8082}")
    private String patientServiceUrl;

    private static final int WORKING_HOUR_START = 9;
    private static final int WORKING_HOUR_END = 18;
    private static final int APPOINTMENT_DURATION_HOURS = 2;

    boolean isDoctorOnVacation(Long doctorId, LocalDate date) {
        try {
            String url = doctorServiceUrl + "/api/vacations/doctor/" + doctorId + "/check?date=" + date;
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            log.warn("Не удалось проверить отпуск врача {} на дату {}: {}",
                    doctorId, date, e.getMessage());
            return false; // Если сервис недоступен, считаем что врач не в отпуске
        }
    }

    public Long getPatientChatId(Long patientId) {
        try {
            String url = patientServiceUrl + "/api/patients/" + patientId + "/with-chat";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getBody() != null) {
                Object chatId = response.getBody().get("telegramChatId");
                if (chatId != null) {
                    if (chatId instanceof Integer) return ((Integer) chatId).longValue();
                    if (chatId instanceof Long) return (Long) chatId;
                    if (chatId instanceof Number) return ((Number) chatId).longValue();
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось получить telegramChatId для пациента {}: {}",
                    patientId, e.getMessage());
        }
        return null;
    }

    public List<TimeSlotDTO> getAvailableTimeSlots(TimeSlotRequest request) {
        // Ищем врача по фамилии
        Long doctorId = findDoctorIdByLastName(request.getDoctorLastName());

        if (doctorId == null) {
            return List.of();
        }

        if (isDoctorOnVacation(doctorId, request.getDate())) {
            log.info("Врач {} в отпуске на дату {}", doctorId, request.getDate());
            return List.of(); // Возвращаем пустой список слотов
        }

        // Получаем все записи врача на указанную дату
        LocalDateTime startOfDay = request.getDate().atStartOfDay();
        LocalDateTime endOfDay = request.getDate().atTime(LocalTime.MAX);

        List<Appointment> appointments =
                appointmentRepository.findByDoctorIdAndDateRange(doctorId, startOfDay, endOfDay);

        // Генерируем все возможные слоты времени
        List<TimeSlotDTO> allSlots = generateTimeSlots(request.getDate(), doctorId);

        // Фильтруем занятые слоты
        List<TimeSlotDTO> availableSlots = new ArrayList<>();

        for (TimeSlotDTO slot : allSlots) {
            boolean isAvailable = isTimeSlotAvailable(slot, appointments);
            if (isAvailable) {
                availableSlots.add(slot);
            }
        }

        return availableSlots;
    }

    private List<TimeSlotDTO> generateTimeSlots(LocalDate date, Long doctorId) {
        List<TimeSlotDTO> slots = new ArrayList<>();
        String doctorName = getDoctorName(doctorId);

        // Генерируем слоты с 9:00 до 18:00 с интервалом 2 часа
        for (int hour = WORKING_HOUR_START; hour <= WORKING_HOUR_END - APPOINTMENT_DURATION_HOURS; hour++) {
            LocalDateTime startTime = date.atTime(hour, 0);
            LocalDateTime endTime = startTime.plusHours(APPOINTMENT_DURATION_HOURS);

            // Проверяем что слот не выходит за рабочий день
            if (endTime.getHour() <= WORKING_HOUR_END ||
                    (endTime.getHour() == WORKING_HOUR_END && endTime.getMinute() == 0)) {

                TimeSlotDTO slot = TimeSlotDTO.builder()
                        .startTime(startTime)
                        .endTime(endTime)
                        .doctorId(doctorId)
                        .doctorName(doctorName)
                        .build();

                slots.add(slot);
            }
        }

        return slots;
    }

    private boolean isTimeSlotAvailable(TimeSlotDTO slot,
                                        List<com.beaverteeth.appointment.model.Appointment> appointments) {

        for (com.beaverteeth.appointment.model.Appointment appointment : appointments) {
            // Пропускаем отмененные записи
            if (appointment.getStatus() == com.beaverteeth.appointment.model.AppointmentStatus.CANCELLED) {
                continue;
            }

            boolean overlaps = slot.getStartTime().isBefore(appointment.getEndTime()) &&
                    slot.getEndTime().isAfter(appointment.getStartTime());

            if (overlaps) {
                return false;
            }
        }

        return true;
    }

    public Long findDoctorIdByLastName(String lastName) {
        try {
            String url = doctorServiceUrl + "/api/doctors/search?lastName=" + lastName;
            ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);

            if (response.getBody() != null && response.getBody().length > 0) {
                Map<String, Object> doctor = response.getBody()[0];
                Object id = doctor.get("id");

                if (id instanceof Integer) return ((Integer) id).longValue();
                if (id instanceof Long) return (Long) id;
                if (id instanceof Number) return ((Number) id).longValue();
            }

            return null;

        } catch (Exception e) {
            log.error("Ошибка при поиске врача в TimeSlotService: {}", e.getMessage());
            return null;
        }
    }

    public String getDoctorName(Long doctorId) {
        try {
            String url = doctorServiceUrl + "/api/doctors/" + doctorId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getBody() != null) {
                return (String) response.getBody().get("fullName");
            }
        } catch (Exception e) {
            log.error("Ошибка при получении имени врача: {}", e.getMessage());
        }

        return "Врач ID: " + doctorId;
    }

    public String getPatientName(Long patientId) {
        try {
            String url = patientServiceUrl + "/api/patients/" + patientId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getBody() != null) {
                return (String) response.getBody().get("fullName");
            }
        } catch (Exception e) {
            log.error("Ошибка при получении имени пациента: {}", e.getMessage());
        }

        return "Пациент ID: " + patientId;
    }

    public void validateDoctorExists(Long doctorId) {
        try {
            String url = doctorServiceUrl + "/api/doctors/" + doctorId;
            restTemplate.getForEntity(url, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Врач с ID " + doctorId + " не найден");
        }
    }

    public void validatePatientExists(Long patientId) {
        try {
            String url = patientServiceUrl + "/api/patients/" + patientId;
            restTemplate.getForEntity(url, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Пациент с ID " + patientId + " не найден");
        }
    }
}