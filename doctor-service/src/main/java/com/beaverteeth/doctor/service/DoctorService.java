package com.beaverteeth.doctor.service;

import com.beaverteeth.doctor.model.Doctor;
import com.beaverteeth.doctor.model.dto.CreateDoctorRequest;
import com.beaverteeth.doctor.model.dto.DoctorDTO;
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

    public DoctorDTO createDoctor(CreateDoctorRequest request) {
        Doctor doctor = modelMapper.map(request, Doctor.class);

        // Устанавливаем автора создания
        doctor.setCreatedBy(request.getCreatedBy());
        doctor.setLastModifiedBy(request.getCreatedBy());

        Doctor savedDoctor = doctorRepository.save(doctor);
        auditService.logDoctorChange(savedDoctor, "CREATE");

        return convertToDTO(savedDoctor);
    }

    public List<DoctorDTO> searchDoctorsByLastName(String lastName) {
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

    public DoctorDTO getDoctorById(Long id) {
        Doctor doctor = doctorRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Врач не найден с ID: " + id));
        return convertToDTO(doctor);
    }

    public List<DoctorDTO> getAllDoctors() {
        return doctorRepository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<DoctorDTO> getDoctorsBySpecialty(Specialty specialty) {
        return doctorRepository.findBySpecialtyAndIsActiveTrue(specialty).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public DoctorDTO updateDoctor(Long id, DoctorDTO doctorDTO, String modifiedBy) {
        Doctor existingDoctor = doctorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Врач не найден с ID: " + id));

        // Сохраняем старую версию для аудита
        Doctor oldDoctor = new Doctor();
        modelMapper.map(existingDoctor, oldDoctor);

        // Обновляем поля
        modelMapper.map(doctorDTO, existingDoctor);
        existingDoctor.setId(id); // чтобы не перезаписалось
        existingDoctor.setLastModifiedBy(modifiedBy);

        Doctor updatedDoctor = doctorRepository.save(existingDoctor);
        auditService.logDoctorChange(oldDoctor, "UPDATE");

        return convertToDTO(updatedDoctor);
    }

    public void deleteDoctor(Long id, String modifiedBy) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Врач не найден с ID: " + id));

        // Мягкое удаление
        doctor.setIsActive(false);
        doctor.setLastModifiedBy(modifiedBy);

        doctorRepository.save(doctor);
        auditService.logDoctorChange(doctor, "DELETE");
    }

    public DoctorDTO convertToDTO(Doctor doctor) {
        return modelMapper.map(doctor, DoctorDTO.class);
    }
}