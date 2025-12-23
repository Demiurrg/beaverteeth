package com.beaverteeth.export.test;

import com.beaverteeth.export.service.ExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private ExportService exportService;

    @InjectMocks
    private ExportController exportController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(exportController).build();
    }

    @Test
    void exportAllData_IOException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        when(exportService.exportAllData()).thenThrow(new IOException("Disk full"));

        // Act & Assert
        mockMvc.perform(post("/api/export/export"))
                .andExpect(status().isInternalServerError());

        verify(exportService, times(1)).exportAllData();
    }

    @Test
    void getExportFiles_ShouldReturnFileList() throws Exception {
        // Arrange
        List<String> expectedFiles = Arrays.asList("export_2024-12-01.json", "export_2024-12-02.json");
        when(exportService.getExportFiles()).thenReturn(expectedFiles);

        // Act & Assert
        mockMvc.perform(get("/api/export/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("export_2024-12-01.json"))
                .andExpect(jsonPath("$[1]").value("export_2024-12-02.json"));

        verify(exportService, times(1)).getExportFiles();
    }

    @Test
    void getExportFiles_IOException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        when(exportService.getExportFiles()).thenThrow(new IOException("Directory not found"));

        // Act & Assert
        mockMvc.perform(get("/api/export/files"))
                .andExpect(status().isInternalServerError());

        verify(exportService, times(1)).getExportFiles();
    }

    @Test
    void downloadExportFile_ShouldReturnFileContent() throws Exception {
        // Arrange
        String fileName = "export_2024-12-01.json";
        byte[] fileContent = "test content".getBytes();
        when(exportService.downloadExportFile(fileName)).thenReturn(fileContent);

        // Act & Assert
        mockMvc.perform(get("/api/export/download/{fileName}", fileName))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(content().bytes(fileContent));

        verify(exportService, times(1)).downloadExportFile(fileName);
    }

    @Test
    void downloadExportFile_FileNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        String fileName = "nonexistent.json";
        when(exportService.downloadExportFile(fileName)).thenThrow(new IOException("File not found"));

        // Act & Assert
        mockMvc.perform(get("/api/export/download/{fileName}", fileName))
                .andExpect(status().isNotFound());

        verify(exportService, times(1)).downloadExportFile(fileName);
    }

    @Test
    void restoreFromUpload_IOException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "export.json",
                MediaType.APPLICATION_JSON_VALUE,
                "test content".getBytes()
        );

        doThrow(new IOException("Restore failed")).when(exportService).restoreFromFile(anyString());

        // Act & Assert
        mockMvc.perform(multipart("/api/export/restore/upload")
                        .file(file))
                .andExpect(status().isInternalServerError());

        verify(exportService, times(1)).restoreFromFile(anyString());
    }

    @Test
    void restoreFromExistingFile_IOException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        String fileName = "export_2024-12-01.json";
        doThrow(new IOException("Restore failed")).when(exportService).restoreFromFile("./exports/" + fileName);

        // Act & Assert
        mockMvc.perform(post("/api/export/restore/{fileName}", fileName))
                .andExpect(status().isInternalServerError());

        verify(exportService, times(1)).restoreFromFile("./exports/" + fileName);
    }
}