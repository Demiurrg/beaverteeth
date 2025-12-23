package com.beaverteeth.patient.controller;

import com.beaverteeth.patient.model.dto.CreatePatientRequest;
import com.beaverteeth.patient.model.dto.PatientDto;
import com.beaverteeth.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "Создать нового пациента")
    public ResponseEntity<PatientDto> createPatient(@Valid @RequestBody CreatePatientRequest request) {
        PatientDto patientDTO = patientService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(patientDTO);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пациента по ID")
    public ResponseEntity<PatientDto> getPatient(@PathVariable Long id) {
        PatientDto patientDTO = patientService.getPatientById(id);
        return ResponseEntity.ok(patientDTO);
    }

    @PutMapping("/{id}/chat-id")
    @Operation(summary = "Обновить Telegram chat ID пациента")
    public ResponseEntity<Void> updatePatientChatId(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") String userId) {

        Long telegramChatId = ((Number) request.get("telegramChatId")).longValue();
        patientService.updatePatientChatId(id, telegramChatId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/with-chat")
    @Operation(summary = "Получить пациента по ID с информацией о chatId")
    public ResponseEntity<PatientDto> getPatientWithChatId(@PathVariable Long id) {
        PatientDto patientDTO = patientService.getPatientWithChatId(id);
        return ResponseEntity.ok(patientDTO);
    }

    @GetMapping("/phone/{phone}")
    @Operation(summary = "Получить пациента по телефону")
    public ResponseEntity<PatientDto> getPatientByPhone(@PathVariable String phone) {
        PatientDto patientDTO = patientService.getPatientByPhone(phone);
        return ResponseEntity.ok(patientDTO);
    }

    @GetMapping("/telegram/{username}")
    @Operation(summary = "Получить пациента по Telegram username")
    public ResponseEntity<PatientDto> getPatientByTelegramUsername(@PathVariable String username) {
        PatientDto patientDTO = patientService.getPatientByTelegramUsername(username);
        return ResponseEntity.ok(patientDTO);
    }

    @GetMapping
    @Operation(summary = "Получить всех пациентов")
    public ResponseEntity<List<PatientDto>> getAllPatients() {
        List<PatientDto> patients = patientService.getAllPatients();
        return ResponseEntity.ok(patients);
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск пациентов по имени")
    public ResponseEntity<List<PatientDto>> searchPatients(@RequestParam String name) {
        List<PatientDto> patients = patientService.searchPatientsByName(name);
        return ResponseEntity.ok(patients);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить данные пациента")
    public ResponseEntity<PatientDto> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientDto patientDTO,
            @RequestHeader("X-User-Id") String userId) {

        PatientDto updatedPatient = patientService.updatePatient(id, patientDTO, userId);
        return ResponseEntity.ok(updatedPatient);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пациента")
    public ResponseEntity<Void> deletePatient(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {

        patientService.deletePatient(id, userId);
        return ResponseEntity.noContent().build();
    }
}