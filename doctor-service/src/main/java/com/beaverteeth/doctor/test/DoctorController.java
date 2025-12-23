package com.beaverteeth.doctor.test;

import com.beaverteeth.doctor.model.dto.CreateDoctorRequest;
import com.beaverteeth.doctor.model.dto.DoctorDto;
import com.beaverteeth.doctor.model.Specialty;
import com.beaverteeth.doctor.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctor Controller", description = "API для управления врачами")
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать нового врача")
    public DoctorDto createDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        return doctorService.createDoctor(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить врача по ID")
    public DoctorDto getDoctor(@PathVariable Long id) {
        return doctorService.getDoctorById(id);
    }

    @GetMapping
    @Operation(summary = "Получить всех врачей")
    public List<DoctorDto> getAllDoctors() {
        return doctorService.getAllDoctors();
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск врача по фамилии")
    public List<DoctorDto> searchDoctors(@RequestParam String lastName) {
        return doctorService.searchDoctorsByLastName(lastName);
    }

    @GetMapping("/specialty/{specialty}")
    @Operation(summary = "Получить врачей по специализации")
    public List<DoctorDto> getDoctorsBySpecialty(@PathVariable Specialty specialty) {
        return doctorService.getDoctorsBySpecialty(specialty);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить данные врача")
    public DoctorDto updateDoctor(
            @PathVariable Long id,
            @Valid @RequestBody DoctorDto doctorDTO,
            @RequestHeader("X-User-Id") String userId) {
        return doctorService.updateDoctor(id, doctorDTO, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить врача")
    public void deleteDoctor(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        doctorService.deleteDoctor(id, userId);
    }
}