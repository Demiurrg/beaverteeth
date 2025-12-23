package com.beaverteeth.doctor.test;

import com.beaverteeth.doctor.model.Vacation;
import com.beaverteeth.doctor.model.dto.VacationRequest;
import com.beaverteeth.doctor.service.VacationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Добавить отпуск для врача")
    public Vacation createVacation(@Valid @RequestBody VacationRequest request) {
        return vacationService.createVacation(request);
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Получить все отпуска врача")
    public List<Vacation> getDoctorVacations(@PathVariable Long doctorId) {
        return vacationService.getVacationsByDoctor(doctorId);
    }

    @GetMapping("/doctor/{doctorId}/check")
    @Operation(summary = "Проверить, в отпуске ли врач на дату")
    public boolean isDoctorOnVacation(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return vacationService.isDoctorOnVacation(doctorId, date);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить запись об отпуске")
    public void deleteVacation(@PathVariable Long id) {
        vacationService.deleteVacation(id);
    }
}