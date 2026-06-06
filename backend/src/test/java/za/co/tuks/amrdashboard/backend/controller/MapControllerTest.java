package za.co.tuks.amrdashboard.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.tuks.amrdashboard.backend.dto.FilterOptionsDTO;
import za.co.tuks.amrdashboard.backend.service.MapService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MapController.class)
class MapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean // Mocks the service layer so the controller has something to call
    private MapService mapService;

    @Test
    void shouldReturnFilterOptions() throws Exception {
        FilterOptionsDTO mockOptions = new FilterOptionsDTO(
                List.of("Apies River"), List.of("E. coli"), List.of("Resistant"), List.of("Trip 1")
        );
        when(mapService.getFilterOptions()).thenReturn(mockOptions);

        mockMvc.perform(get("/api/v1/map/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rivers[0]").value("Apies River"))
                .andExpect(jsonPath("$.organisms[0]").value("E. coli"));
    }

    @Test
    void shouldReturnFilteredMarkers() throws Exception {
        za.co.tuks.amrdashboard.backend.dto.SiteMarkerDTO marker =
                new za.co.tuks.amrdashboard.backend.dto.SiteMarkerDTO(
                        "A10", "Pretoria North", "Apies River",
                        -25.747, 28.229, "HIGH", "#FF0000", 75, 8
                );
        when(mapService.getFilteredMarkers(
                List.of("Apies River"), List.of("E. coli"), null, null))
                .thenReturn(List.of(marker));

        mockMvc.perform(get("/api/v1/map/markers")
                        .param("riverName", "Apies River")
                        .param("organism", "E. coli"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].siteId").value("A10"))
                .andExpect(jsonPath("$[0].riskLevel").value("HIGH"))
                .andExpect(jsonPath("$[0].resistantPercent").value(75));
    }

    @Test
    void shouldReturnAllMarkersWhenNoFiltersProvided() throws Exception {
        za.co.tuks.amrdashboard.backend.dto.SiteMarkerDTO marker =
                new za.co.tuks.amrdashboard.backend.dto.SiteMarkerDTO(
                        "B26", "Hammanskraal", "Apies River",
                        -25.4, 28.3, "MEDIUM", "#FFA500", 40, 5
                );
        when(mapService.getFilteredMarkers(null, null, null, null))
                .thenReturn(List.of(marker));

        mockMvc.perform(get("/api/v1/map/markers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].siteId").value("B26"))
                .andExpect(jsonPath("$[0].riskLevel").value("MEDIUM"));
    }

    @Test
    void shouldReturnSiteSummary() throws Exception {
        za.co.tuks.amrdashboard.backend.dto.SiteSummaryDTO summary =
                new za.co.tuks.amrdashboard.backend.dto.SiteSummaryDTO(
                        "A10", "Pretoria North", "Apies River",
                        12, List.of("E. coli", "K. pneumoniae"), "2025-05-10",
                        "HIGH", "#FF0000", "High AMR Risk", "Frequent resistant isolates detected",
                        75, 9, 12, 7.2, 6.5
                );
        when(mapService.getSiteSummary("A10")).thenReturn(summary);

        mockMvc.perform(get("/api/v1/map/sites/A10/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteId").value("A10"))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.detectedOrganisms[0]").value("E. coli"));
    }

    @Test
    void shouldReturn404ForUnknownSiteSummary() throws Exception {
        when(mapService.getSiteSummary("UNKNOWN"))
                .thenThrow(new RuntimeException("Site not found"));

        mockMvc.perform(get("/api/v1/map/sites/UNKNOWN/summary"))
                .andExpect(status().isNotFound());
    }
}