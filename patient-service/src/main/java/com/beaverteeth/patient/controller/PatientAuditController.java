package com.beaverteeth.patient.controller;

import com.beaverteeth.patient.model.dto.PatientAuditDto;
import com.beaverteeth.patient.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patients/{patientId}/audit")
@RequiredArgsConstructor
@Tag(name = "Patient Audit", description = "API для журнала изменений пациентов")
public class PatientAuditController {

    private final AuditService auditService;
    private final ModelMapper modelMapper;

    @GetMapping
    @Operation(summary = "Получить журнал изменений пациента")
    public ResponseEntity<List<PatientAuditDto>> getAuditHistory(@PathVariable Long patientId) {
        var auditLogs = auditService.getPatientHistory(patientId);

        var auditDTOs = auditLogs.stream()
                .map(log -> modelMapper.map(log, PatientAuditDto.class))
                .collect(Collectors.toList());

        return ResponseEntity.ok(auditDTOs);
    }
}