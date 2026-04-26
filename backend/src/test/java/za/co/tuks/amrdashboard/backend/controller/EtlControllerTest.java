package za.co.tuks.amrdashboard.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import za.co.tuks.amrdashboard.backend.service.EtlService;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EtlController.class)
class EtlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EtlService etlService;

    @Test
    void shouldAcceptBatchUpload() throws Exception {
        MockMultipartFile epicollectFile = new MockMultipartFile(
                "epicollect", 
                "metadata.csv", 
                "text/csv", 
                "Site ID,Location Name\nA10,Pretoria".getBytes()
        );

        when(etlService.processBatch(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(multipart("/api/v1/etl/upload-batch")
                        .file(epicollectFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Batch processed successfully."));
    }

    @Test
    void shouldRejectWhenNoFilesProvided() throws Exception {
        mockMvc.perform(multipart("/api/v1/etl/upload-batch"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No files provided for upload."));
    }

    @Test
    void shouldHandleValidationErrorsGracefully() throws Exception {
        MockMultipartFile badFile = new MockMultipartFile(
                "epicollect", 
                "bad.csv", 
                "text/csv", 
                "missing headers".getBytes()
        );

        when(etlService.processBatch(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Critical column 'Site ID' is missing"));

        mockMvc.perform(multipart("/api/v1/etl/upload-batch")
                        .file(badFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Validation Error: Critical column 'Site ID' is missing"));
    }
}