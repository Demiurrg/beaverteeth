package com.beaverteeth.patient.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync; // Добавить импорт

@Configuration
@EnableJpaAuditing
@EnableAsync
public class AuditConfig {
}