package za.co.tuks.amrdashboard.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.tuks.amrdashboard.backend.service.SitesService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SitesController.class)
class SitesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SitesService sitesService;

    @Test
    void shouldReturnAllSites() throws Exception {
        when(sitesService.getAllSites()).thenReturn(List.of(
                Map.of(
                        "siteId", "A10",
                        "locationName", "Pretoria",
                        "riverName", "Apies River",
                        "latitude", -25.747,
                        "longitude", 28.229
                )
        ));

        mockMvc.perform(get("/api/v1/sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].siteId").value("A10"))
                .andExpect(jsonPath("$[0].locationName").value("Pretoria"));
    }

    @Test
    void shouldReturnSingleSite() throws Exception {
        when(sitesService.getSiteById("A10")).thenReturn(Map.of(
                "siteId", "A10",
                "locationName", "Pretoria",
                "riverName", "Apies River",
                "latitude", -25.747,
                "longitude", 28.229
        ));

        mockMvc.perform(get("/api/v1/sites/A10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteId").value("A10"))
                .andExpect(jsonPath("$.riverName").value("Apies River"));
    }

    @Test
    void shouldReturnNotFoundWhenSiteMissing() throws Exception {
        when(sitesService.getSiteById("MISSING")).thenReturn(null);

        mockMvc.perform(get("/api/v1/sites/MISSING"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnFilteredWaterSamples() throws Exception {
        when(sitesService.getWaterSamplesBySiteId("A10", "Trip 1")).thenReturn(List.of(
                Map.of(
                        "sampleId", "SAMP-001",
                        "siteId", "A10",
                        "tripIdentifier", "Trip 1",
                        "collectionDate", "2025-05-10",
                        "waterTemperature", 18.5,
                        "phLevel", 7.2,
                        "tds", 250.0,
                        "ec", 400.0,
                        "dissolvedOxygen", 6.5
                )
        ));

        mockMvc.perform(get("/api/v1/sites/A10/water-samples").param("trip", "Trip 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sampleId").value("SAMP-001"))
                .andExpect(jsonPath("$[0].tripIdentifier").value("Trip 1"));
    }

    @Test
    void shouldReturnSiteIsolates() throws Exception {
        when(sitesService.getIsolatesBySiteId("A10")).thenReturn(List.of(
                Map.of(
                        "isolateId", "ISO-101",
                        "sampleId", "SAMP-001",
                        "isolateNumber", "TSp1H",
                        "organismIdentity", "Klebsiella pneumoniae",
                        "sourceContext", "Spinach at harvest",
                        "arCode", "B1",
                        "virulenceGenes", "rmpA, iutA",
                        "binaryTypingProfile", Map.of(
                                "Intl1", true,
                                "Intl2", false,
                                "Intl3", true,
                                "TEM", true,
                                "SHV", true
                        )
                )
        ));

        mockMvc.perform(get("/api/v1/sites/A10/isolates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isolateId").value("ISO-101"))
                .andExpect(jsonPath("$[0].binaryTypingProfile.Intl1").value(true));
    }

    @Test
    void shouldReturnComparisonStats() throws Exception {
        Map<String, Object> resultA = new java.util.HashMap<>();
        resultA.put("siteId", "A10");
        resultA.put("locationName", "Pretoria");
        resultA.put("riverName", "Apies River");
        resultA.put("totalSamples", 2);
        resultA.put("totalIsolates", 1);
        resultA.put("avgPh", 7.05);
        resultA.put("avgTemperature", 16.9);
        resultA.put("avgTds", 265.0);
        resultA.put("avgEc", 425.0);
        resultA.put("avgDo", 6.15);
        resultA.put("intl1PositiveRate", 100.0);
        resultA.put("amrGeneHits", 2);
        resultA.put("wgsPassRate", 100.0);
        resultA.put("resistantIsolates", 1);

        Map<String, Object> resultB = new java.util.HashMap<>();
        resultB.put("siteId", "B26");
        resultB.put("locationName", "Hammanskraal");
        resultB.put("riverName", "Apies River");
        resultB.put("totalSamples", 1);
        resultB.put("totalIsolates", 1);
        resultB.put("avgPh", 7.2);
        resultB.put("avgTemperature", 17.0);
        resultB.put("avgTds", 260.0);
        resultB.put("avgEc", 420.0);
        resultB.put("avgDo", 6.0);
        resultB.put("intl1PositiveRate", 100.0);
        resultB.put("amrGeneHits", 1);
        resultB.put("wgsPassRate", 100.0);
        resultB.put("resistantIsolates", 1);

        when(sitesService.compareAnalytics("A10", "B26", null)).thenReturn(List.of(resultA, resultB));

        mockMvc.perform(get("/api/v1/analytics/compare")
                        .param("siteA", "A10")
                        .param("siteB", "B26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].siteId").value("A10"))
                .andExpect(jsonPath("$[1].siteId").value("B26"));
    }
}
