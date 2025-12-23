package com.beaverteeth.doctor.controller;

import com.beaverteeth.doctor.model.dto.CreateDoctorRequest;
import com.beaverteeth.doctor.model.dto.DoctorDto;
import com.beaverteeth.doctor.model.Specialty;
import com.beaverteeth.doctor.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctor Controller", description = "API для управления врачами")
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    @Operation(summary = "Создать нового врача")
    public ResponseEntity<DoctorDto> createDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        DoctorDto doctorDTO = doctorService.createDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorDTO);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить врача по ID")
    public ResponseEntity<DoctorDto> getDoctor(@PathVariable Long id) {
        DoctorDto doctorDTO = doctorService.getDoctorById(id);
        return ResponseEntity.ok(doctorDTO);
    }

    @GetMapping
    @Operation(summary = "Получить всех врачей")
    public ResponseEntity<List<DoctorDto>> getAllDoctors() {
        List<DoctorDto> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск врача по фамилии")
    public ResponseEntity<List<DoctorDto>> searchDoctors(@RequestParam String lastName) {
        List<DoctorDto> doctors = doctorService.searchDoctorsByLastName(lastName);
        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/specialty/{specialty}")
    @Operation(summary = "Получить врачей по специализации")
    public ResponseEntity<List<DoctorDto>> getDoctorsBySpecialty(@PathVariable Specialty specialty) {
        List<DoctorDto> doctors = doctorService.getDoctorsBySpecialty(specialty);
        return ResponseEntity.ok(doctors);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить данные врача")
    public ResponseEntity<DoctorDto> updateDoctor(
            @PathVariable Long id,
            @Valid @RequestBody DoctorDto doctorDTO,
            @RequestHeader("X-User-Id") String userId) {

        DoctorDto updatedDoctor = doctorService.updateDoctor(id, doctorDTO, userId);
        return ResponseEntity.ok(updatedDoctor);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить врача")
    public ResponseEntity<Void> deleteDoctor(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {

        doctorService.deleteDoctor(id, userId);
        return ResponseEntity.noContent().build();
    }
}