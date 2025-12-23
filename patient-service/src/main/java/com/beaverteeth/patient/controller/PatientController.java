package com.beaverteeth.patient.controller;

import com.beaverteeth.patient.model.dto.CreatePatientRequest;
import com.beaverteeth.patient.model.dto.PatientDto;
import com.beaverteeth.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patient Controller", description = "API для управления пациентами")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать нового пациента")
    public PatientDto createPatient(@Valid @RequestBody CreatePatientRequest request) {
        return patientService.createPatient(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пациента по ID")
    public PatientDto getPatient(@PathVariable Long id) {
        return patientService.getPatientById(id);
    }

    @PutMapping("/{id}/chat-id")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Обновить Telegram chat ID пациента")
    public void updatePatientChatId(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") String userId) {

        Long telegramChatId = ((Number) request.get("telegramChatId")).longValue();
        patientService.updatePatientChatId(id, telegramChatId, userId);
    }

    @GetMapping("/{id}/with-chat")
    @Operation(summary = "Получить пациента по ID с информацией о chatId")
    public PatientDto getPatientWithChatId(@PathVariable Long id) {
        return patientService.getPatientWithChatId(id);
    }

    @GetMapping("/phone/{phone}")
    @Operation(summary = "Получить пациента по телефону")
    public PatientDto getPatientByPhone(@PathVariable String phone) {
        return patientService.getPatientByPhone(phone);
    }

    @GetMapping("/telegram/{username}")
    @Operation(summary = "Получить пациента по Telegram username")
    public PatientDto getPatientByTelegramUsername(@PathVariable String username) {
        return patientService.getPatientByTelegramUsername(username);
    }

    @GetMapping
    @Operation(summary = "Получить всех пациентов")
    public List<PatientDto> getAllPatients() {
        return patientService.getAllPatients();
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск пациентов по имени")
    public List<PatientDto> searchPatients(@RequestParam String name) {
        return patientService.searchPatientsByName(name);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить данные пациента")
    public PatientDto updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientDto patientDTO,
            @RequestHeader("X-User-Id") String userId) {

        return patientService.updatePatient(id, patientDTO, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить пациента")
    public void deletePatient(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {

        patientService.deletePatient(id, userId);
    }
}