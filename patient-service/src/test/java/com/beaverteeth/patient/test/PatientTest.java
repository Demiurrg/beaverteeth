package com.beaverteeth.patient.test;

import com.beaverteeth.patient.model.Patient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatientTest {

    @Test
    void patientBuilder_ShouldCreatePatientWithDefaultValues() {
        // Act
        Patient patient = Patient.builder()
                .fullName("Иванов Иван Иванович")
                .age(30)
                .address("Москва")
                .phone("+79123456789")
                .build();

        // Assert
        assertNotNull(patient);
        assertEquals("Иванов Иван Иванович", patient.getFullName());
        assertEquals(30, patient.getAge());
        assertEquals("+79123456789", patient.getPhone());
        assertTrue(patient.getIsActive()); // Проверяем значение по умолчанию
    }

    @Test
    void patientSettersAndGetters_ShouldWorkCorrectly() {
        // Arrange
        Patient patient = new Patient();

        // Act
        patient.setId(1L);
        patient.setFullName("Петров Петр Петрович");
        patient.setAge(25);
        patient.setAddress("Санкт-Петербург");
        patient.setPhone("+79234567890");
        patient.setEmail("petrov@example.com");
        patient.setTelegramUsername("@petrov");
        patient.setTelegramChatId(123456L);
        patient.setIsActive(false);
        patient.setNotes("Примечания");

        // Assert
        assertEquals(1L, patient.getId());
        assertEquals("Петров Петр Петрович", patient.getFullName());
        assertEquals(25, patient.getAge());
        assertEquals("Санкт-Петербург", patient.getAddress());
        assertEquals("+79234567890", patient.getPhone());
        assertEquals("petrov@example.com", patient.getEmail());
        assertEquals("@petrov", patient.getTelegramUsername());
        assertEquals(123456L, patient.getTelegramChatId());
        assertFalse(patient.getIsActive());
        assertEquals("Примечания", patient.getNotes());
    }

    @Test
    void allArgsConstructor_ShouldCreatePatientWithAllFields() {
        // Act
        Patient patient = new Patient(
                1L,
                "Иванов Иван Иванович",
                30,
                "Москва",
                "+79123456789",
                "ivanov@example.com",
                "@ivanov",
                123456789L,
                true,
                "Аллергия на пенициллин"
        );

        // Assert
        assertNotNull(patient);
        assertEquals(1L, patient.getId());
        assertEquals("Иванов Иван Иванович", patient.getFullName());
        assertEquals(30, patient.getAge());
        assertEquals("Москва", patient.getAddress());
        assertEquals("+79123456789", patient.getPhone());
        assertEquals("ivanov@example.com", patient.getEmail());
        assertEquals("@ivanov", patient.getTelegramUsername());
        assertEquals(123456789L, patient.getTelegramChatId());
        assertTrue(patient.getIsActive());
        assertEquals("Аллергия на пенициллин", patient.getNotes());
    }
}