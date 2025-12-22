package com.beaverteeth.doctor.controller;

import com.beaverteeth.doctor.model.dto.DoctorAuditDTO;
import com.beaverteeth.doctor.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors/{doctorId}/audit")
@RequiredArgsConstructor
@Tag(name = "Doctor Audit", description = "API для журнала изменений врачей")
public class DoctorAuditController {

    private final AuditService auditService;
    private final ModelMapper modelMapper;

    @GetMapping
    @Operation(summary = "Получить журнал изменений врача")
    public ResponseEntity<List<DoctorAuditDTO>> getAuditHistory(@PathVariable Long doctorId) {
        var auditLogs = auditService.getDoctorHistory(doctorId);

        var auditDTOs = auditLogs.stream()
                .map(log -> modelMapper.map(log, DoctorAuditDTO.class))
                .collect(Collectors.toList());

        return ResponseEntity.ok(auditDTOs);
    }

    @GetMapping("/actions/{action}")
    @Operation(summary = "Получить историю по типу действия")
    public ResponseEntity<List<DoctorAuditDTO>> getAuditByAction(
            @PathVariable Long doctorId,
            @PathVariable String action) {

        // Нужно добавить метод в репозиторий и сервис
        // findByDoctorIdAndActionOrderByChangedAtDesc

        return ResponseEntity.ok(List.of()); // заглушка
    }
}