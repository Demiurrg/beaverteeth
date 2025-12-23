package com.beaverteeth.export.service;

import com.beaverteeth.export.model.ExportData;
import com.beaverteeth.export.model.dto.AppointmentDto;
import com.beaverteeth.export.model.dto.DoctorDto;
import com.beaverteeth.export.model.dto.PatientDto;
import com.beaverteeth.export.model.dto.CreateAppointmentRequest;
import com.beaverteeth.export.model.dto.CreateDoctorRequest;
import com.beaverteeth.export.model.dto.CreatePatientRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

        ExportData exportData = collectAllData();
        createExportDirectory();

        String fileName = generateExportFileName();
        String filePath = exportDirectory + "/" + fileName;

        writeDataToFile(exportData, filePath);

        log.info("Экспорт завершен. Файл: {}", filePath);
        return filePath;
    }

    public void restoreFromFile(String filePath) throws IOException {
        log.info("Начинаем восстановление данных из файла: {}", filePath);

        ExportData exportData = readDataFromFile(filePath);

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
        return ExportData.builder()
                .exportDate(LocalDateTime.now())
                .doctors(fetchDoctors())
                .patients(fetchPatients())
                .appointments(fetchAppointments())
                .build();
    }

    private List<ExportData.DoctorData> fetchDoctors() {
        try {
            List<DoctorDto> doctorDtos = restTemplate.exchange(
                    doctorServiceUrl + "/api/doctors",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<DoctorDto>>() {}
            ).getBody();

            if (doctorDtos != null) {
                return doctorDtos.stream()
                        .map(this::convertToDoctorData)
                        .toList();
            }
        } catch (Exception e) {
            log.error("Ошибка при получении данных врачей: {}", e.getMessage());
        }

        return List.of();
    }

    private ExportData.DoctorData convertToDoctorData(DoctorDto dto) {
        return ExportData.DoctorData.builder()
                .id(dto.getId())
                .fullName(dto.getFullName())
                .specialty(dto.getSpecialty())
                .totalExperience(dto.getTotalExperience())
                .clinicExperience(dto.getClinicExperience())
                .education(dto.getEducation())
                .certificates(dto.getCertificates())
                .build();
    }

    private List<ExportData.PatientData> fetchPatients() {
        try {
            List<PatientDto> patientDtos = restTemplate.exchange(
                    patientServiceUrl + "/api/patients",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<PatientDto>>() {}
            ).getBody();

            if (patientDtos != null) {
                return patientDtos.stream()
                        .map(this::convertToPatientData)
                        .toList();
            }
        } catch (Exception e) {
            log.error("Ошибка при получении данных пациентов: {}", e.getMessage());
        }

        return List.of();
    }

    private ExportData.PatientData convertToPatientData(PatientDto dto) {
        return ExportData.PatientData.builder()
                .id(dto.getId())
                .fullName(dto.getFullName())
                .age(dto.getAge())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .notes(dto.getNotes())
                .build();
    }

    private List<ExportData.AppointmentData> fetchAppointments() {
        try {
            List<AppointmentDto> appointmentDtos = restTemplate.exchange(
                    appointmentServiceUrl + "/api/appointments",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<AppointmentDto>>() {}
            ).getBody();

            if (appointmentDtos != null) {
                return appointmentDtos.stream()
                        .map(this::convertToAppointmentData)
                        .toList();
            }
        } catch (Exception e) {
            log.error("Ошибка при получении данных записей: {}", e.getMessage());
        }

        return List.of();
    }

    private ExportData.AppointmentData convertToAppointmentData(AppointmentDto dto) {
        return ExportData.AppointmentData.builder()
                .id(dto.getId())
                .doctorId(dto.getDoctorId())
                .patientId(dto.getPatientId())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(dto.getStatus())
                .notes(dto.getNotes())
                .createdAt(dto.getCreatedAt())
                .changedAt(dto.getChangedAt())
                .build();
    }

    private void restoreDoctors(List<ExportData.DoctorData> doctors) {
        log.info("Восстановление {} врачей", doctors.size());

        for (ExportData.DoctorData doctor : doctors) {
            try {
                // Проверяем существование врача
                if (doctorExists(doctor.getId())) {
                    log.info("Врач ID {} уже существует, пропускаем", doctor.getId());
                    continue;
                }

                // Создаем врача
                CreateDoctorRequest request = CreateDoctorRequest.builder()
                        .fullName(doctor.getFullName())
                        .totalExperience(doctor.getTotalExperience())
                        .clinicExperience(doctor.getClinicExperience())
                        .specialty(doctor.getSpecialty())
                        .education(doctor.getEducation())
                        .certificates(doctor.getCertificates())
                        .createdBy("system-restore")
                        .build();

                restTemplate.postForObject(
                        doctorServiceUrl + "/api/doctors",
                        request,
                        DoctorDto.class
                );
                log.info("Восстановлен врач: {}", doctor.getFullName());

            } catch (Exception e) {
                log.error("Ошибка при восстановлении врача {}: {}", doctor.getId(), e.getMessage());
            }
        }
    }

    private boolean doctorExists(Long doctorId) {
        try {
            restTemplate.getForObject(
                    doctorServiceUrl + "/api/doctors/" + doctorId,
                    DoctorDto.class
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void restorePatients(List<ExportData.PatientData> patients) {
        log.info("Восстановление {} пациентов", patients.size());

        for (ExportData.PatientData patient : patients) {
            try {
                // Проверяем существование пациента
                if (patientExists(patient.getId())) {
                    log.info("Пациент ID {} уже существует, пропускаем", patient.getId());
                    continue;
                }

                // Создаем пациента
                CreatePatientRequest request = CreatePatientRequest.builder()
                        .fullName(patient.getFullName())
                        .age(patient.getAge())
                        .address(patient.getAddress())
                        .phone(patient.getPhone())
                        .email(patient.getEmail())
                        .notes(patient.getNotes())
                        .createdBy("system-restore")
                        .build();

                restTemplate.postForObject(
                        patientServiceUrl + "/api/patients",
                        request,
                        PatientDto.class
                );
                log.info("Восстановлен пациент: {}", patient.getFullName());

            } catch (Exception e) {
                log.error("Ошибка при восстановлении пациента {}: {}", patient.getId(), e.getMessage());
            }
        }
    }

    private boolean patientExists(Long patientId) {
        try {
            restTemplate.getForObject(
                    patientServiceUrl + "/api/patients/" + patientId,
                    PatientDto.class
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void restoreAppointments(List<ExportData.AppointmentData> appointments) {
        log.info("Восстановление {} записей", appointments.size());

        for (ExportData.AppointmentData appointment : appointments) {
            try {
                // Проверяем, существуют ли врач и пациент
                if (!doctorExists(appointment.getDoctorId())) {
                    log.warn("Врач ID {} не существует, пропускаем запись", appointment.getDoctorId());
                    continue;
                }

                if (!patientExists(appointment.getPatientId())) {
                    log.warn("Пациент ID {} не существует, пропускаем запись", appointment.getPatientId());
                    continue;
                }

                // Создаем запись
                CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                        .doctorId(appointment.getDoctorId())
                        .patientId(appointment.getPatientId())
                        .startTime(appointment.getStartTime())
                        .notes(appointment.getNotes() != null ? appointment.getNotes() : "")
                        .build();

                restTemplate.postForObject(
                        appointmentServiceUrl + "/api/appointments",
                        request,
                        AppointmentDto.class
                );
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

    public String generateExportFileName() {
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