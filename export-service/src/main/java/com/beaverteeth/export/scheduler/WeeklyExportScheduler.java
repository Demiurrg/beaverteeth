package com.beaverteeth.export.scheduler;

import com.beaverteeth.export.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeeklyExportScheduler {

    private final ExportService exportService;

    // Запускается каждый понедельник в 2:00 ночи
    @Scheduled(cron = "0 0 2 * * MON")
    public void performWeeklyExport() {
        log.info("Запуск еженедельного экспорта данных...");

        try {
            String exportFilePath = exportService.exportAllData();
            log.info("Еженедельный экспорт успешно завершен: {}", exportFilePath);
        } catch (IOException e) {
            log.error("Ошибка при выполнении еженедельного экспорта: {}", e.getMessage(), e);
        }
    }

    // Дополнительная задача: удаление старых файлов (старше 30 дней)
    @Scheduled(cron = "0 0 3 * * *") // Каждый день в 3:00
    public void cleanupOldExports() {
        log.info("Очистка старых файлов экспорта...");
        // Реализация очистки может быть добавлена при необходимости
    }
}