package com.beaverteeth.export.test;

import com.beaverteeth.export.model.ExportData;
import com.beaverteeth.export.service.ExportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExportService exportService;

    @TempDir
    Path tempDir;

    private final ObjectMapper testObjectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Настраиваем временную директорию для экспорта через рефлексию
        setField(exportService, "exportDirectory", tempDir.toString());
        setField(exportService, "doctorServiceUrl", "http://doctor-service");
        setField(exportService, "patientServiceUrl", "http://patient-service");
        setField(exportService, "appointmentServiceUrl", "http://appointment-service");
    }

    private void setField(Object object, String fieldName, Object value) {
        try {
            var field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getExportFiles_ShouldReturnSortedFileList() throws IOException {
        // Arrange
        // Создаем тестовые файлы
        Files.createFile(tempDir.resolve("export_2024-12-02.json"));
        Files.createFile(tempDir.resolve("export_2024-12-01.json"));
        Files.createFile(tempDir.resolve("not_a_json.txt")); // Должен быть отфильтрован

        // Act
        List<String> files = exportService.getExportFiles();

        // Assert
        assertEquals(2, files.size());
        assertEquals("export_2024-12-01.json", files.get(0)); // Отсортированы
        assertEquals("export_2024-12-02.json", files.get(1));
    }

    @Test
    void getExportFiles_DirectoryNotExists_ShouldReturnEmptyList() throws IOException {
        // Arrange
        // Изменяем директорию на несуществующую
        setField(exportService, "exportDirectory", tempDir + "/nonexistent");

        // Act
        List<String> files = exportService.getExportFiles();

        // Assert
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    void downloadExportFile_ShouldReturnFileContent() throws IOException {
        // Arrange
        String fileName = "export_test.json";
        String fileContent = "test content";
        Files.writeString(tempDir.resolve(fileName), fileContent);

        // Act
        byte[] result = exportService.downloadExportFile(fileName);

        // Assert
        assertNotNull(result);
        assertEquals(fileContent, new String(result));
    }

    @Test
    void downloadExportFile_FileNotFound_ShouldThrowException() {
        // Arrange
        String fileName = "nonexistent.json";

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            exportService.downloadExportFile(fileName);
        });

        assertEquals("Файл не найден: " + fileName, exception.getMessage());
    }

    @Test
    void restoreFromFile_FileNotFound_ShouldThrowException() {
        // Arrange
        String nonExistentFile = tempDir + "/nonexistent.json";

        // Act & Assert
        assertThrows(IOException.class, () -> {
            exportService.restoreFromFile(nonExistentFile);
        });
    }

    @Test
    void generateExportFileName_ShouldIncludeTimestamp() {
        // Этот тест нужно запускать быстро, чтобы timestamp не успел измениться
        // Arrange
        String fileName = exportService.generateExportFileName();

        // Act & Assert
        assertTrue(fileName.startsWith("export_"));
        assertTrue(fileName.endsWith(".json"));
        assertTrue(fileName.length() > "export_".length() + ".json".length());
    }

    // Вспомогательные методы
    private void mockServiceResponses() {
        // Мокаем данные врачей
        ExportData.DoctorData doctor1 = ExportData.DoctorData.builder()
                .id(1L)
                .fullName("Доктор Иванов")
                .build();

        ExportData.DoctorData doctor2 = ExportData.DoctorData.builder()
                .id(2L)
                .fullName("Доктор Петров")
                .build();

        when(restTemplate.exchange(
                eq("http://doctor-service/api/doctors"),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(Arrays.asList(doctor1, doctor2)));

        // Мокаем данные пациентов
        ExportData.PatientData patient1 = ExportData.PatientData.builder()
                .id(1L)
                .fullName("Пациент Сидоров")
                .build();

        ExportData.PatientData patient2 = ExportData.PatientData.builder()
                .id(2L)
                .fullName("Пациент Николаев")
                .build();

        when(restTemplate.exchange(
                eq("http://patient-service/api/patients"),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(Arrays.asList(patient1, patient2)));

        // Мокаем данные записей
        ExportData.AppointmentData appointment = ExportData.AppointmentData.builder()
                .id(1L)
                .doctorId(1L)
                .patientId(1L)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(2))
                .status("CONFIRMED")
                .build();

        when(restTemplate.exchange(
                eq("http://appointment-service/api/appointments"),
                eq(HttpMethod.GET),
                eq(null),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of(appointment)));
    }

    private ExportData createTestExportData() {
        ExportData.DoctorData doctor = ExportData.DoctorData.builder()
                .id(1L)
                .fullName("Доктор Иванов")
                .build();

        ExportData.PatientData patient = ExportData.PatientData.builder()
                .id(1L)
                .fullName("Пациент Петров")
                .phone("+79123456789")
                .build();

        ExportData.AppointmentData appointment = ExportData.AppointmentData.builder()
                .id(1L)
                .doctorId(1L)
                .patientId(1L)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(2))
                .status("PENDING")
                .build();

        return ExportData.builder()
                .exportDate(LocalDateTime.now())
                .doctors(List.of(doctor))
                .patients(List.of(patient))
                .appointments(List.of(appointment))
                .build();
    }
}