package com.beaverteeth.doctor.controller;

import com.beaverteeth.doctor.model.dto.DoctorAuditDto;
import com.beaverteeth.doctor.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
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
    public List<DoctorAuditDto> getAuditHistory(@PathVariable Long doctorId) {
        var auditLogs = auditService.getDoctorHistory(doctorId);

        return auditLogs.stream()
                .map(log -> modelMapper.map(log, DoctorAuditDto.class))
                .collect(Collectors.toList());
    }
}