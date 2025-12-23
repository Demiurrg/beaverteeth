package com.beaverteeth.doctor.service;

import com.beaverteeth.doctor.model.Doctor;
import com.beaverteeth.doctor.model.dto.CreateDoctorRequest;
import com.beaverteeth.doctor.model.dto.DoctorDto;
import com.beaverteeth.doctor.model.Specialty;
import com.beaverteeth.doctor.repository.DoctorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final ModelMapper modelMapper;
    private final AuditService auditService;

    public DoctorDto createDoctor(CreateDoctorRequest request) {
        Doctor doctor = modelMapper.map(request, Doctor.class);
        doctor.setIsActive(true);
        doctor.setChangedBy(request.getCreatedBy());

        Doctor savedDoctor = doctorRepository.save(doctor);

        // Запись в журнал
        auditService.logDoctorChange(savedDoctor.getId(), "CREATE",
                request.getCreatedBy(),
                "Создан новый врач: " + savedDoctor.getFullName());

        return convertToDTO(savedDoctor);
    }

    public List<DoctorDto> searchDoctorsByLastName(String lastName) {
        // Ищем врачей по фамилии (активных)
        List<Doctor> doctors = doctorRepository.findByLastNameContainingIgnoreCaseAndIsActiveTrue(lastName);

        if (doctors.isEmpty()) {
            // Если не нашли по точному совпадению, ищем по части фамилии в полном имени
            doctors = doctorRepository.findByFullNameContainingIgnoreCaseAndIsActiveTrue(lastName);
        }

        return doctors.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public DoctorDto getDoctorById(Long id) {
        Doctor doctor = doctorRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Врач не найден с ID: " + id));
        return convertToDTO(doctor);
    }

    public List<DoctorDto> getAllDoctors() {
        return doctorRepository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<DoctorDto> getDoctorsBySpecialty(Specialty specialty) {
        return doctorRepository.findBySpecialtyAndIsActiveTrue(specialty).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public DoctorDto updateDoctor(Long id, DoctorDto doctorDTO, String modifiedBy) {
        Doctor existingDoctor = doctorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Врач не найден"));

        // Простое описание изменений
        String changesDescription = "Обновлены данные врача";

        // Можно добавить логику определения измененных полей:
        if (!existingDoctor.getFullName().equals(doctorDTO.getFullName())) {
            changesDescription += ", ФИО";
        }
        if (!existingDoctor.getSpecialty().equals(doctorDTO.getSpecialty())) {
            changesDescription += ", специализация";
        }
        // ... другие поля

        // Обновляем поля
        modelMapper.map(doctorDTO, existingDoctor);
        existingDoctor.setId(id);
        existingDoctor.setChangedBy(modifiedBy);

        Doctor updatedDoctor = doctorRepository.save(existingDoctor);

        // Запись в журнал
        auditService.logDoctorChange(id, "UPDATE", modifiedBy, changesDescription);

        return convertToDTO(updatedDoctor);
    }

    public void deleteDoctor(Long id, String modifiedBy) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Врач не найден"));

        doctor.setIsActive(false);
        doctor.setChangedBy(modifiedBy);

        doctorRepository.save(doctor);

        // Запись в журнал
        auditService.logDoctorChange(id, "DELETE", modifiedBy,
                "Врач отключен: " + doctor.getFullName());
    }

    public DoctorDto convertToDTO(Doctor doctor) {
        return modelMapper.map(doctor, DoctorDto.class);
    }
}