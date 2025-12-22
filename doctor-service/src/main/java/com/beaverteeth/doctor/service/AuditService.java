package com.beaverteeth.doctor.service;

import com.beaverteeth.doctor.model.Doctor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuditService {

    public void logDoctorChange(Doctor doctor, String action) {
        log.info("Действие над врачом: {} | ID: {} | ФИО: {} | Действие: {}",
                doctor.getClass().getSimpleName(),
                doctor.getId(),
                doctor.getFullName(),
                action);
    }
}