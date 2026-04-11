package za.co.tuks.amrdashboard.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import za.co.tuks.amrdashboard.backend.service.EtlService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EtlController.class)
class EtlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EtlService etlService;

    @Test
    void shouldAcceptExcelUpload() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test.xlsx", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                "dummy content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/etl/upload")
                        .file(file)
                        .param("fileType", "EPICOLLECT"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectEmptyFile() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/etl/upload")
                        .file(emptyFile)
                        .param("fileType", "EPICOLLECT"))
                .andExpect(status().isBadRequest()); // Maps to our logic handling empty files
    }
}