package za.co.tuks.amrdashboard.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.tuks.amrdashboard.backend.service.AnalyticsService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    void shouldReturnSampleCount() throws Exception {
        when(analyticsService.getSampleCount()).thenReturn(Map.of("total", 120L, "siteCount", 8L));

        mockMvc.perform(get("/api/v1/analytics/dashboard/sample-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(120))
                .andExpect(jsonPath("$.siteCount").value(8));
    }

    @Test
    void shouldReturnIncidentRate() throws Exception {
        when(analyticsService.getIncidentRate()).thenReturn(Map.of(
                "rate", 34.5,
                "comparedToYear", 2024,
                "delta", 2.1,
                "direction", "up"
        ));

        mockMvc.perform(get("/api/v1/analytics/dashboard/incident-rate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(34.5))
                .andExpect(jsonPath("$.direction").value("up"));
    }

    @Test
    void shouldReturnMonthlyCases() throws Exception {
        when(analyticsService.getMonthlyCases(2025, 5)).thenReturn(Map.of(
                "year", 2025,
                "month", 5,
                "caseCount", 10L,
                "previousMonthCount", 7L,
                "delta", 3L,
                "direction", "up"
        ));

        mockMvc.perform(get("/api/v1/analytics/dashboard/monthly-cases")
                        .param("year", "2025")
                        .param("month", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseCount").value(10))
                .andExpect(jsonPath("$.direction").value("up"));
    }

    @Test
    void shouldReturnTopOrganisms() throws Exception {
        when(analyticsService.getTopOrganisms(5)).thenReturn(Map.of(
                "organisms", List.of(Map.of(
                        "name", "Klebsiella pneumoniae",
                        "arCode", "ESBL",
                        "detectionCount", 42L,
                        "yoyTrend", "up"
                ))
        ));

        mockMvc.perform(get("/api/v1/analytics/dashboard/top-organisms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organisms[0].name").value("Klebsiella pneumoniae"))
                .andExpect(jsonPath("$.organisms[0].yoyTrend").value("up"));
    }

    @Test
    void shouldReturnHighRiskSites() throws Exception {
        when(analyticsService.getHighRiskSites()).thenReturn(Map.of(
                "count", 3,
                "newThisMonth", 1L
        ));

        mockMvc.perform(get("/api/v1/analytics/dashboard/high-risk-sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.newThisMonth").value(1));
    }

    @Test
    void shouldReturnRiskByProvince() throws Exception {
        when(analyticsService.getRiskByProvince()).thenReturn(Map.of(
                "provinces", List.of(Map.of(
                        "name", "Gauteng",
                        "riskLevel", "HIGH",
                        "riskScore", 85,
                        "mdroCount", 12L
                ))
        ));

        mockMvc.perform(get("/api/v1/analytics/dashboard/risk-by-province"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provinces[0].name").value("Gauteng"))
                .andExpect(jsonPath("$.provinces[0].riskLevel").value("HIGH"));
    }

    @Test
    void shouldReturnMonthlyTrendForYear() throws Exception {
        when(analyticsService.getMonthlyTrend(2025)).thenReturn(Map.of(
                "year", 2025,
                "months", List.of(Map.of("month", 1, "count", 5L))
        ));

        mockMvc.perform(get("/api/v1/analytics/dashboard/monthly-trend").param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2025))
                .andExpect(jsonPath("$.months[0].count").value(5));
    }

    @Test
    void shouldReturnAvailableYears() throws Exception {
        when(analyticsService.getAvailableYears()).thenReturn(Map.of(
                "years", List.of(2023, 2024, 2025)
        ));

        mockMvc.perform(get("/api/v1/analytics/dashboard/monthly-trend/available-years"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.years[0]").value(2023))
                .andExpect(jsonPath("$.years[2]").value(2025));
    }

    @Test
    void shouldReturnBestYear() throws Exception {
        when(analyticsService.getMonthlyTrendBestYear()).thenReturn(Map.of(
                "bestYear", 2024,
                "totalCases", 148L
        ));

        mockMvc.perform(get("/api/v1/analytics/dashboard/monthly-trend/best-year"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bestYear").value(2024))
                .andExpect(jsonPath("$.totalCases").value(148));
    }

    @Test
    void shouldReturnTopResistanceGenes() throws Exception {
        when(analyticsService.getTopResistanceGenes(8)).thenReturn(Map.of(
                "genes", List.of(Map.of(
                        "geneSymbol", "blaTEM",
                        "detectionCount", 34L,
                        "resistanceClass", "Beta-lactam"
                ))
        ));

        mockMvc.perform(get("/api/v1/analytics/dashboard/top-resistance-genes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genes[0].geneSymbol").value("blaTEM"))
                .andExpect(jsonPath("$.genes[0].resistanceClass").value("Beta-lactam"));
    }

    @Test
    void shouldReturnAffectedRiverSites() throws Exception {
        when(analyticsService.getAffectedRiverSites(6)).thenReturn(Map.of(
                "sites", List.of(Map.of(
                        "riverName", "Apies River",
                        "siteCount", 4L,
                        "resistantCount", 22L
                ))
        ));

        mockMvc.perform(get("/api/v1/analytics/dashboard/affected-river-sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sites[0].riverName").value("Apies River"))
                .andExpect(jsonPath("$.sites[0].resistantCount").value(22));
    }
}
