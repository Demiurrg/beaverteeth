package com.beaverteeth.patient.service;

import com.beaverteeth.patient.model.Patient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuditService {

    public void logPatientChange(Patient patient, String action) {
        log.info("Действие над пациентом: {} | ID: {} | ФИО: {} | Телефон: {} | Telegram: {} | Действие: {}",
                patient.getClass().getSimpleName(),
                patient.getId(),
                patient.getFullName(),
                patient.getPhone(),
                patient.getTelegramUsername(),
                action);
    }
}