package com.beaverteeth.patient.controller;

import com.beaverteeth.patient.model.dto.CreatePatientRequest;
import com.beaverteeth.patient.model.dto.PatientDTO;
import com.beaverteeth.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patient Controller", description = "API для управления пациентами")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @Operation(summary = "Создать нового пациента")
    public ResponseEntity<PatientDTO> createPatient(@Valid @RequestBody CreatePatientRequest request) {
        PatientDTO patientDTO = patientService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(patientDTO);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пациента по ID")
    public ResponseEntity<PatientDTO> getPatient(@PathVariable Long id) {
        PatientDTO patientDTO = patientService.getPatientById(id);
        return ResponseEntity.ok(patientDTO);
    }

    @GetMapping("/phone/{phone}")
    @Operation(summary = "Получить пациента по телефону")
    public ResponseEntity<PatientDTO> getPatientByPhone(@PathVariable String phone) {
        PatientDTO patientDTO = patientService.getPatientByPhone(phone);
        return ResponseEntity.ok(patientDTO);
    }

    @GetMapping("/telegram/{username}")
    @Operation(summary = "Получить пациента по Telegram username")
    public ResponseEntity<PatientDTO> getPatientByTelegramUsername(@PathVariable String username) {
        PatientDTO patientDTO = patientService.getPatientByTelegramUsername(username);
        return ResponseEntity.ok(patientDTO);
    }

    @GetMapping
    @Operation(summary = "Получить всех пациентов")
    public ResponseEntity<List<PatientDTO>> getAllPatients() {
        List<PatientDTO> patients = patientService.getAllPatients();
        return ResponseEntity.ok(patients);
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск пациентов по имени")
    public ResponseEntity<List<PatientDTO>> searchPatients(@RequestParam String name) {
        List<PatientDTO> patients = patientService.searchPatientsByName(name);
        return ResponseEntity.ok(patients);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить данные пациента")
    public ResponseEntity<PatientDTO> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientDTO patientDTO,
            @RequestHeader("X-User-Id") String userId) {

        PatientDTO updatedPatient = patientService.updatePatient(id, patientDTO, userId);
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