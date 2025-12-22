package com.beaverteeth.doctor.service;

import com.beaverteeth.doctor.model.Vacation;
import com.beaverteeth.doctor.model.dto.VacationRequest;
import com.beaverteeth.doctor.repository.DoctorRepository;
import com.beaverteeth.doctor.repository.VacationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VacationService {

    private final VacationRepository vacationRepository;
    private final DoctorRepository doctorRepository;

    public Vacation createVacation(VacationRequest request) {
        // Проверяем, что врач существует
        doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new EntityNotFoundException("Врач не найден с ID: " + request.getDoctorId()));

        // Проверяем, что дата начала раньше даты окончания
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Дата начала отпуска должна быть раньше даты окончания");
        }

        // Проверяем, что отпуск не в прошлом (только на текущий год и будущее)
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Нельзя добавлять отпуск в прошлом");
        }

        // Проверяем пересечение с существующими отпусками
        List<Vacation> overlappingVacations = vacationRepository
                .findByDoctorIdAndDateRange(
                        request.getDoctorId(),
                        request.getStartDate(),
                        request.getEndDate()
                );

        if (!overlappingVacations.isEmpty()) {
            throw new IllegalArgumentException("Врач уже в отпуске в указанный период");
        }

        Vacation vacation = Vacation.builder()
                .doctorId(request.getDoctorId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .build();

        Vacation savedVacation = vacationRepository.save(vacation);
        log.info("Добавлен отпуск для врача {}: с {} по {}",
                request.getDoctorId(), request.getStartDate(), request.getEndDate());

        return savedVacation;
    }

    public List<Vacation> getVacationsByDoctor(Long doctorId) {
        return vacationRepository.findByDoctorId(doctorId);
    }

    public boolean isDoctorOnVacation(Long doctorId, LocalDate date) {
        List<Vacation> vacations = vacationRepository.findByDoctorIdAndDate(doctorId, date);
        return !vacations.isEmpty();
    }

    public void deleteVacation(Long id) {
        if (!vacationRepository.existsById(id)) {
            throw new EntityNotFoundException("Запись об отпуске не найдена с ID: " + id);
        }
        vacationRepository.deleteById(id);
        log.info("Удалена запись об отпуске: ID={}", id);
    }
}