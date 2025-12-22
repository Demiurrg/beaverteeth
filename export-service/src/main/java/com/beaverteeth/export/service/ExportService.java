package com.beaverteeth.export.service;

import com.beaverteeth.export.model.ExportData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final RestTemplate restTemplate;

    @Value("${export.directory:./exports}")
    private String exportDirectory;

    @Value("${doctor.service.url:http://doctor-service:8080}")
    private String doctorServiceUrl;

    @Value("${patient.service.url:http://patient-service:8080}")
    private String patientServiceUrl;

    @Value("${appointment.service.url:http://appointment-service:8080}")
    private String appointmentServiceUrl;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public String exportAllData() throws IOException {
        log.info("Начинаем экспорт всех данных");

        // Собираем данные из всех сервисов
        ExportData exportData = collectAllData();

        // Создаем директорию для экспорта если не существует
        createExportDirectory();

        // Генерируем имя файла с датой
        String fileName = generateExportFileName();
        String filePath = exportDirectory + "/" + fileName;

        // Записываем данные в JSON файл
        writeDataToFile(exportData, filePath);

        log.info("Экспорт завершен. Файл: {}", filePath);
        return filePath;
    }

    public void restoreFromFile(String filePath) throws IOException {
        log.info("Начинаем восстановление данных из файла: {}", filePath);

        // Читаем данные из файла
        ExportData exportData = readDataFromFile(filePath);

        // Восстанавливаем данные в системе
        restoreDoctors(exportData.getDoctors());
        restorePatients(exportData.getPatients());
        restoreAppointments(exportData.getAppointments());

        log.info("Восстановление данных завершено");
    }

    public List<String> getExportFiles() throws IOException {
        Path exportPath = Paths.get(exportDirectory);

        if (!Files.exists(exportPath)) {
            return List.of();
        }

        return Files.list(exportPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .map(Path::getFileName)
                .map(Path::toString)
                .sorted()
                .toList();
    }

    public byte[] downloadExportFile(String fileName) throws IOException {
        String filePath = exportDirectory + "/" + fileName;
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("Файл не найден: " + fileName);
        }

        return Files.readAllBytes(path);
    }

    private ExportData collectAllData() {
        ExportData exportData = ExportData.builder()
                .exportDate(LocalDateTime.now())
                .doctors(fetchDoctors())
                .patients(fetchPatients())
                .appointments(fetchAppointments())
                .build();

        return exportData;
    }

    private List<ExportData.DoctorData> fetchDoctors() {
        try {
            String url = doctorServiceUrl + "/api/doctors";
            ResponseEntity<ExportData.DoctorData[]> response = restTemplate
                    .getForEntity(url, ExportData.DoctorData[].class);

            if (response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (Exception e) {
            log.error("Ошибка при получении данных врачей: {}", e.getMessage());
        }

        return List.of();
    }

    private List<ExportData.PatientData> fetchPatients() {
        try {
            String url = patientServiceUrl + "/api/patients";
            ResponseEntity<ExportData.PatientData[]> response = restTemplate
                    .getForEntity(url, ExportData.PatientData[].class);

            if (response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (Exception e) {
            log.error("Ошибка при получении данных пациентов: {}", e.getMessage());
        }

        return List.of();
    }

    private List<ExportData.AppointmentData> fetchAppointments() {
        try {
            // Получаем всех врачей и их записи
            List<ExportData.DoctorData> doctors = fetchDoctors();
            List<ExportData.AppointmentData> allAppointments = new java.util.ArrayList<>();

            for (ExportData.DoctorData doctor : doctors) {
                String url = appointmentServiceUrl + "/api/appointments/doctor/" + doctor.getId();
                ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);

                if (response.getBody() != null) {
                    for (Map appointmentMap : response.getBody()) {
                        ExportData.AppointmentData appointment = convertMapToAppointmentData(appointmentMap);
                        allAppointments.add(appointment);
                    }
                }
            }

            return allAppointments;
        } catch (Exception e) {
            log.error("Ошибка при получении данных записей: {}", e.getMessage());
        }

        return List.of();
    }

    private ExportData.AppointmentData convertMapToAppointmentData(Map<String, Object> map) {
        return ExportData.AppointmentData.builder()
                .id(((Integer) map.get("id")).longValue())
                .doctorId(((Integer) map.get("doctorId")).longValue())
                .patientId(((Integer) map.get("patientId")).longValue())
                .startTime(LocalDateTime.parse((String) map.get("startTime")))
                .endTime(LocalDateTime.parse((String) map.get("endTime")))
                .status((String) map.get("status"))
                .notes((String) map.get("notes"))
                .createdAt(LocalDateTime.parse((String) map.get("createdAt")))
                .updatedAt(LocalDateTime.parse((String) map.get("updatedAt")))
                .build();
    }

    private void restoreDoctors(List<ExportData.DoctorData> doctors) {
        log.info("Восстановление {} врачей", doctors.size());

        for (ExportData.DoctorData doctor : doctors) {
            try {
                // Проверяем, существует ли уже врач
                String checkUrl = doctorServiceUrl + "/api/doctors/" + doctor.getId();
                try {
                    restTemplate.getForEntity(checkUrl, Map.class);
                    log.info("Врач ID {} уже существует, пропускаем", doctor.getId());
                    continue;
                } catch (Exception e) {
                    // Врач не существует, создаем нового
                }

                // Создаем врача
                String createUrl = doctorServiceUrl + "/api/doctors";
                Map<String, Object> request = Map.of(
                        "fullName", doctor.getFullName(),
                        "totalExperience", doctor.getTotalExperience(),
                        "clinicExperience", doctor.getClinicExperience(),
                        "specialty", doctor.getSpecialty(),
                        "education", doctor.getEducation(),
                        "certificates", doctor.getCertificates(),
                        "createdBy", "system-restore"
                );

                restTemplate.postForEntity(createUrl, request, Map.class);
                log.info("Восстановлен врач: {}", doctor.getFullName());

            } catch (Exception e) {
                log.error("Ошибка при восстановлении врача {}: {}", doctor.getId(), e.getMessage());
            }
        }
    }

    private void restorePatients(List<ExportData.PatientData> patients) {
        log.info("Восстановление {} пациентов", patients.size());

        for (ExportData.PatientData patient : patients) {
            try {
                // Проверяем, существует ли уже пациент
                String checkUrl = patientServiceUrl + "/api/patients/" + patient.getId();
                try {
                    restTemplate.getForEntity(checkUrl, Map.class);
                    log.info("Пациент ID {} уже существует, пропускаем", patient.getId());
                    continue;
                } catch (Exception e) {
                    // Пациент не существует, создаем нового
                }

                // Создаем пациента
                String createUrl = patientServiceUrl + "/api/patients";
                Map<String, Object> request = Map.of(
                        "fullName", patient.getFullName(),
                        "age", patient.getAge(),
                        "address", patient.getAddress(),
                        "phone", patient.getPhone(),
                        "email", patient.getEmail(),
                        "notes", patient.getNotes(),
                        "createdBy", "system-restore"
                );

                restTemplate.postForEntity(createUrl, request, Map.class);
                log.info("Восстановлен пациент: {}", patient.getFullName());

            } catch (Exception e) {
                log.error("Ошибка при восстановлении пациента {}: {}", patient.getId(), e.getMessage());
            }
        }
    }

    private void restoreAppointments(List<ExportData.AppointmentData> appointments) {
        log.info("Восстановление {} записей", appointments.size());

        for (ExportData.AppointmentData appointment : appointments) {
            try {
                // Создаем запись
                String createUrl = appointmentServiceUrl + "/api/appointments";
                Map<String, Object> request = Map.of(
                        "doctorId", appointment.getDoctorId(),
                        "patientId", appointment.getPatientId(),
                        "startTime", appointment.getStartTime().toString(),
                        "notes", appointment.getNotes() != null ? appointment.getNotes() : ""
                );

                restTemplate.postForEntity(createUrl, request, Map.class);
                log.info("Восстановлена запись: врач={}, пациент={}, время={}",
                        appointment.getDoctorId(), appointment.getPatientId(), appointment.getStartTime());

            } catch (Exception e) {
                log.error("Ошибка при восстановлении записи {}: {}", appointment.getId(), e.getMessage());
            }
        }
    }

    private void createExportDirectory() throws IOException {
        Path path = Paths.get(exportDirectory);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("Создана директория для экспорта: {}", exportDirectory);
        }
    }

    private String generateExportFileName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        return "export_" + timestamp + ".json";
    }

    private void writeDataToFile(ExportData exportData, String filePath) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(filePath), exportData);
    }

    private ExportData readDataFromFile(String filePath) throws IOException {
        return objectMapper.readValue(new File(filePath), ExportData.class);
    }
}