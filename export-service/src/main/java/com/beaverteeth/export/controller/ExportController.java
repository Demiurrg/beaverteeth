package com.beaverteeth.export.controller;

import com.beaverteeth.export.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "Export Controller", description = "API для экспорта и восстановления данных")
public class ExportController {

    private final ExportService exportService;

    @PostMapping("/export")
    @Operation(summary = "Выполнить экспорт всех данных")
    public ResponseEntity<String> exportAllData() {
        try {
            String exportFilePath = exportService.exportAllData();
            return ResponseEntity.ok("Экспорт успешно завершен. Файл: " + exportFilePath);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка при экспорте данных: " + e.getMessage());
        }
    }

    @GetMapping("/files")
    @Operation(summary = "Получить список файлов экспорта")
    public ResponseEntity<List<String>> getExportFiles() {
        try {
            List<String> files = exportService.getExportFiles();
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/download/{fileName}")
    @Operation(summary = "Скачать файл экспорта")
    public ResponseEntity<Resource> downloadExportFile(@PathVariable String fileName) {
        try {
            byte[] fileContent = exportService.downloadExportFile(fileName);

            ByteArrayResource resource = new ByteArrayResource(fileContent);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(fileContent.length)
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/restore/upload")
    @Operation(summary = "Восстановить данные из загруженного файла")
    public ResponseEntity<String> restoreFromUpload(@RequestParam("file") MultipartFile file) {
        try {
            // Сохраняем загруженный файл временно
            String tempFilePath = saveUploadedFile(file);

            // Восстанавливаем данные из файла
            exportService.restoreFromFile(tempFilePath);

            // Удаляем временный файл
            Files.deleteIfExists(Paths.get(tempFilePath));

            return ResponseEntity.ok("Данные успешно восстановлены из файла: " + file.getOriginalFilename());

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка при восстановлении данных: " + e.getMessage());
        }
    }

    @PostMapping("/restore/{fileName}")
    @Operation(summary = "Восстановить данные из существующего файла")
    public ResponseEntity<String> restoreFromExistingFile(@PathVariable String fileName) {
        try {
            exportService.restoreFromFile("./exports/" + fileName);
            return ResponseEntity.ok("Данные успешно восстановлены из файла: " + fileName);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка при восстановлении данных: " + e.getMessage());
        }
    }

    private String saveUploadedFile(MultipartFile file) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        Path tempFilePath = Paths.get(tempDir, file.getOriginalFilename());

        file.transferTo(tempFilePath);

        return tempFilePath.toString();
    }
}