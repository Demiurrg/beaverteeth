package com.beaverteeth.export.test;

import com.beaverteeth.export.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
    public String exportAllData() {
        try {
            String exportFilePath = exportService.exportAllData();
            return "Экспорт успешно завершен. Файл: " + exportFilePath;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ошибка при экспорте данных: " + e.getMessage());
        }
    }

    @GetMapping("/files")
    @Operation(summary = "Получить список файлов экспорта")
    public List<String> getExportFiles() {
        try {
            return exportService.getExportFiles();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ошибка при получении списка файлов: " + e.getMessage());
        }
    }

    @GetMapping("/download/{fileName}")
    @Operation(summary = "Скачать файл экспорта")
    public Resource downloadExportFile(@PathVariable String fileName) {
        try {
            byte[] fileContent = exportService.downloadExportFile(fileName);

            return new ByteArrayResource(fileContent);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Файл не найден: " + fileName);
        }
    }

    @PostMapping("/restore/upload")
    @Operation(summary = "Восстановить данные из загруженного файла")
    public String restoreFromUpload(@RequestParam("file") MultipartFile file) {
        try {
            // Сохраняем загруженный файл временно
            String tempFilePath = saveUploadedFile(file);

            // Восстанавливаем данные из файла
            exportService.restoreFromFile(tempFilePath);

            // Удаляем временный файл
            Files.deleteIfExists(Paths.get(tempFilePath));

            return "Данные успешно восстановлены из файла: " + file.getOriginalFilename();

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ошибка при восстановлении данных: " + e.getMessage());
        }
    }

    @PostMapping("/restore/{fileName}")
    @Operation(summary = "Восстановить данные из существующего файла")
    public String restoreFromExistingFile(@PathVariable String fileName) {
        try {
            exportService.restoreFromFile("./exports/" + fileName);
            return "Данные успешно восстановлены из файла: " + fileName;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ошибка при восстановлении данных: " + e.getMessage());
        }
    }

    private String saveUploadedFile(MultipartFile file) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        Path tempFilePath = Paths.get(tempDir, file.getOriginalFilename());

        file.transferTo(tempFilePath);

        return tempFilePath.toString();
    }
}