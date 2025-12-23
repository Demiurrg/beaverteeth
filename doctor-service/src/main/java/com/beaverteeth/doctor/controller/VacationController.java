package com.beaverteeth.doctor.controller;

import com.beaverteeth.doctor.model.Vacation;
import com.beaverteeth.doctor.model.dto.VacationRequest;
import com.beaverteeth.doctor.service.VacationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vacations")
@RequiredArgsConstructor
@Tag(name = "Vacation Controller", description = "API для управления отпусками врачей")
public class VacationController {

    private final VacationService vacationService;

    @PostMapping
    @Operation(summary = "Добавить отпуск для врача")
    public ResponseEntity<Vacation> createVacation(@Valid @RequestBody VacationRequest request) {
        Vacation vacation = vacationService.createVacation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(vacation);
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Получить все отпуска врача")
    public ResponseEntity<List<Vacation>> getDoctorVacations(@PathVariable Long doctorId) {
        List<Vacation> vacations = vacationService.getVacationsByDoctor(doctorId);
        return ResponseEntity.ok(vacations);
    }

    @GetMapping("/doctor/{doctorId}/check")
    @Operation(summary = "Проверить, в отпуске ли врач на дату")
    public ResponseEntity<Boolean> isDoctorOnVacation(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        boolean isOnVacation = vacationService.isDoctorOnVacation(doctorId, date);
        return ResponseEntity.ok(isOnVacation);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запись об отпуске")
    public ResponseEntity<Void> deleteVacation(@PathVariable Long id) {
        vacationService.deleteVacation(id);
        return ResponseEntity.noContent().build();
    }
}