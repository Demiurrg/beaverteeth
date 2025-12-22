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

        // Проверка уникальности телефона
        if (patientRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Пациент с таким телефоном уже существует");
        }

        // Проверка уникальности email (если указан)
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && patientRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Пациент с таким email уже существует");
        }

        // Проверка уникальности Telegram username (если указан)
        if (request.getTelegramUsername() != null && !request.getTelegramUsername().isBlank()
                && patientRepository.existsByTelegramUsername(request.getTelegramUsername())) {
            throw new IllegalArgumentException("Пациент с таким Telegram username уже существует");
        }

        Patient patient = modelMapper.map(request, Patient.class);

        // Устанавливаем автора создания
        patient.setCreatedBy(request.getCreatedBy());
        patient.setLastModifiedBy(request.getCreatedBy());

        Patient savedPatient = patientRepository.save(patient);
        auditService.logPatientChange(savedPatient, "CREATE");

        return convertToDTO(savedPatient);
    }

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

    public PatientDTO updatePatient(Long id, PatientDTO patientDTO, String modifiedBy) {
        Patient existingPatient = patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пациент не найден с ID: " + id));

        // Сохраняем старую версию для аудита
        Patient oldPatient = new Patient();
        modelMapper.map(existingPatient, oldPatient);

        // Проверка уникальности телефона (если изменился)
        if (!existingPatient.getPhone().equals(patientDTO.getPhone())
                && patientRepository.existsByPhone(patientDTO.getPhone())) {
            throw new IllegalArgumentException("Пациент с таким телефоном уже существует");
        }

        // Проверка уникальности email (если изменился)
        if (patientDTO.getEmail() != null
                && !patientDTO.getEmail().equals(existingPatient.getEmail())
                && patientRepository.existsByEmail(patientDTO.getEmail())) {
            throw new IllegalArgumentException("Пациент с таким email уже существует");
        }

        // Проверка уникальности Telegram username (если изменился)
        if (patientDTO.getTelegramUsername() != null
                && !patientDTO.getTelegramUsername().equals(existingPatient.getTelegramUsername())
                && patientRepository.existsByTelegramUsername(patientDTO.getTelegramUsername())) {
            throw new IllegalArgumentException("Пациент с таким Telegram username уже существует");
        }

        // Обновляем поля
        modelMapper.map(patientDTO, existingPatient);
        existingPatient.setId(id); // чтобы не перезаписалось
        existingPatient.setLastModifiedBy(modifiedBy);

        Patient updatedPatient = patientRepository.save(existingPatient);
        auditService.logPatientChange(oldPatient, "UPDATE");

        return convertToDTO(updatedPatient);
    }

    public void deletePatient(Long id, String modifiedBy) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пациент не найден с ID: " + id));

        // Мягкое удаление
        patient.setIsActive(false);
        patient.setLastModifiedBy(modifiedBy);

        patientRepository.save(patient);
        auditService.logPatientChange(patient, "DELETE");
    }

    public PatientDTO convertToDTO(Patient patient) {
        return modelMapper.map(patient, PatientDTO.class);
    }
}