package com.beaverteeth.patient.service;

import com.beaverteeth.patient.model.Patient;
import com.beaverteeth.patient.model.dto.CreatePatientRequest;
import com.beaverteeth.patient.model.dto.PatientDTO;
import com.beaverteeth.patient.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final ModelMapper modelMapper;
    private final AuditService auditService;

    public PatientDTO createPatient(CreatePatientRequest request) {
        log.info("Создание пациента: {}", request.getFullName());

        // Проверки уникальности...
        if (patientRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Пациент с таким телефоном уже существует");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && patientRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Пациент с таким email уже существует");
        }

        if (request.getTelegramUsername() != null && !request.getTelegramUsername().isBlank()
                && patientRepository.existsByTelegramUsername(request.getTelegramUsername())) {
            throw new IllegalArgumentException("Пациент с таким Telegram username уже существует");
        }

        Patient patient = modelMapper.map(request, Patient.class);
        patient.setChangedBy(request.getCreatedBy());

        Patient savedPatient = patientRepository.save(patient);

        // ЗАПИСЬ В ЖУРНАЛ
        auditService.logPatientChange(savedPatient.getId(), "CREATE",
                request.getCreatedBy(),
                "Создан новый пациент: " + savedPatient.getFullName() +
                        " (тел.: " + savedPatient.getPhone() + ")");

        return convertToDTO(savedPatient);
    }

    public void updatePatientChatId(Long id, Long telegramChatId, String modifiedBy) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пациент не найден с ID: " + id));

        patient.setTelegramChatId(telegramChatId);
        patient.setChangedBy(modifiedBy);

        patientRepository.save(patient);
        log.info("Обновлен telegramChatId для пациента {}: {}", id, telegramChatId);
    }

    public PatientDTO getPatientWithChatId(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пациент не найден с ID: " + id));
        return convertToDTO(patient);
    }

    public PatientDTO updatePatient(Long id, PatientDTO patientDTO, String modifiedBy) {
        Patient existingPatient = patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пациент не найден с ID: " + id));

        // Проверки уникальности...
        if (!existingPatient.getPhone().equals(patientDTO.getPhone())
                && patientRepository.existsByPhone(patientDTO.getPhone())) {
            throw new IllegalArgumentException("Пациент с таким телефоном уже существует");
        }

        if (patientDTO.getEmail() != null
                && !patientDTO.getEmail().equals(existingPatient.getEmail())
                && patientRepository.existsByEmail(patientDTO.getEmail())) {
            throw new IllegalArgumentException("Пациент с таким email уже существует");
        }

        if (patientDTO.getTelegramUsername() != null
                && !patientDTO.getTelegramUsername().equals(existingPatient.getTelegramUsername())
                && patientRepository.existsByTelegramUsername(patientDTO.getTelegramUsername())) {
            throw new IllegalArgumentException("Пациент с таким Telegram username уже существует");
        }

        // Определяем измененные поля для описания
        StringBuilder changesDescription = new StringBuilder("Обновлены данные пациента");
        boolean hasChanges = false;

        if (!existingPatient.getFullName().equals(patientDTO.getFullName())) {
            changesDescription.append(", ФИО");
            hasChanges = true;
        }
        if (!existingPatient.getPhone().equals(patientDTO.getPhone())) {
            changesDescription.append(", телефон");
            hasChanges = true;
        }
        if (patientDTO.getEmail() != null && !patientDTO.getEmail().equals(existingPatient.getEmail())) {
            changesDescription.append(", email");
            hasChanges = true;
        }
        if (patientDTO.getAge() != null && !patientDTO.getAge().equals(existingPatient.getAge())) {
            changesDescription.append(", возраст");
            hasChanges = true;
        }
        // ... можно добавить проверку других полей

        if (!hasChanges) {
            changesDescription.append(" (без изменений данных)");
        }

        // Обновляем поля
        modelMapper.map(patientDTO, existingPatient);
        existingPatient.setId(id);
        existingPatient.setChangedBy(modifiedBy);

        Patient updatedPatient = patientRepository.save(existingPatient);

        // ЗАПИСЬ В ЖУРНАЛ
        auditService.logPatientChange(id, "UPDATE", modifiedBy, changesDescription.toString());

        return convertToDTO(updatedPatient);
    }

    public void deletePatient(Long id, String modifiedBy) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пациент не найден с ID: " + id));

        patient.setIsActive(false);
        patient.setChangedBy(modifiedBy);

        patientRepository.save(patient);

        // ЗАПИСЬ В ЖУРНАЛ
        auditService.logPatientChange(id, "DELETE", modifiedBy,
                "Пациент отключен: " + patient.getFullName() +
                        " (тел.: " + patient.getPhone() + ")");
    }

    // Остальные методы остаются БЕЗ ИЗМЕНЕНИЙ:
    public PatientDTO getPatientById(Long id) {
        Patient patient = patientRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Пациент не найден с ID: " + id));
        return convertToDTO(patient);
    }

    public PatientDTO getPatientByPhone(String phone) {
        Patient patient = patientRepository.findByPhone(phone)
                .orElseThrow(() -> new EntityNotFoundException("Пациент не найден с телефоном: " + phone));

        if (!patient.getIsActive()) {
            throw new EntityNotFoundException("Пациент не активен");
        }

        return convertToDTO(patient);
    }

    public PatientDTO getPatientByTelegramUsername(String telegramUsername) {
        Patient patient = patientRepository.findByTelegramUsername(telegramUsername)
                .orElseThrow(() -> new EntityNotFoundException("Пациент не найден с Telegram username: " + telegramUsername));

        if (!patient.getIsActive()) {
            throw new EntityNotFoundException("Пациент не активен");
        }

        return convertToDTO(patient);
    }

    public List<PatientDTO> getAllPatients() {
        return patientRepository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PatientDTO> searchPatientsByName(String name) {
        return patientRepository.findByFullNameContainingIgnoreCase(name).stream()
                .filter(Patient::getIsActive)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PatientDTO convertToDTO(Patient patient) {
        return modelMapper.map(patient, PatientDTO.class);
    }
}