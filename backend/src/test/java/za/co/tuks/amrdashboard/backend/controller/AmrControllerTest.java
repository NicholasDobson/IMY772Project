package za.co.tuks.amrdashboard.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.tuks.amrdashboard.backend.service.AmrService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AmrController.class)
class AmrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AmrService amrService;

    @Test
    void shouldReturnResistanceGenes() throws Exception {
        when(amrService.getResistanceGenes("Klebsiella pneumoniae", 10)).thenReturn(Map.of(
                "organism", "Klebsiella pneumoniae",
                "genes", List.of(Map.of(
                        "geneSymbol", "blaTEM",
                        "resistanceClass", "Beta-lactam",
                        "occurrenceCount", 5L
                ))
        ));

        mockMvc.perform(get("/api/v1/analytics/amr/genes")
                        .param("organism", "Klebsiella pneumoniae"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organism").value("Klebsiella pneumoniae"))
                .andExpect(jsonPath("$.genes[0].geneSymbol").value("blaTEM"));
    }

    @Test
    void shouldReturnResistanceProfile() throws Exception {
        when(amrService.getResistanceProfile("E. coli")).thenReturn(Map.of(
                "organism", "E. coli",
                "arCode", "ESBL",
                "resistanceProfile", List.of(Map.of(
                        "antibiotic", "Ampicillin",
                        "resistanceRate", 0.9,
                        "level", "R"
                ))
        ));

        mockMvc.perform(get("/api/v1/analytics/amr/resistance-profile")
                        .param("organism", "E. coli"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organism").value("E. coli"))
                .andExpect(jsonPath("$.arCode").value("ESBL"))
                .andExpect(jsonPath("$.resistanceProfile[0].level").value("R"));
    }

    @Test
    void shouldReturnEmptyGenesListWhenNoneFound() throws Exception {
        when(amrService.getResistanceGenes("Rare", 10)).thenReturn(Map.of(
                "organism", "Rare",
                "genes", List.of()
        ));

        mockMvc.perform(get("/api/v1/analytics/amr/genes")
                        .param("organism", "Rare"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genes").isEmpty());
    }
}
