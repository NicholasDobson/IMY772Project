package za.co.tuks.amrdashboard.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.tuks.amrdashboard.backend.model.Isolate;
import za.co.tuks.amrdashboard.backend.repository.IsolateRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AmrServiceTest {

    @Mock
    private EntityManager em;

    @Mock
    private IsolateRepository isolateRepository;

    @Mock
    private Query mockQuery;

    private AmrService amrService;

    @BeforeEach
    void setup() {
        amrService = new AmrService(isolateRepository);
        ReflectionTestUtils.setField(amrService, "em", em);
        when(em.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.setMaxResults(anyInt())).thenReturn(mockQuery);
    }

    // ── getResistanceProfile ──────────────────────────────────────────────────

    @Test
    void shouldReturnEmptyProfileWhenNoIsolates() {
        when(isolateRepository.findByOrganismIdentity("UnknownOrg")).thenReturn(List.of());

        Map<String, Object> result = amrService.getResistanceProfile("UnknownOrg");

        assertEquals("UnknownOrg", result.get("organism"));
        assertEquals("UNKNOWN", result.get("arCode"));
        assertTrue(((List<?>) result.get("resistanceProfile")).isEmpty());
    }

    @Test
    void shouldClassifyHighResistanceAsR() {
        Isolate isolate = new Isolate();
        isolate.setArCode("ESBL");
        isolate.setBinaryTypingProfile(Map.of("Ampicillin", true));

        when(isolateRepository.findByOrganismIdentity("Klebsiella pneumoniae"))
                .thenReturn(List.of(isolate));

        Map<String, Object> result = amrService.getResistanceProfile("Klebsiella pneumoniae");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> profile = (List<Map<String, Object>>) result.get("resistanceProfile");

        assertEquals(1, profile.size());
        assertEquals("Ampicillin", profile.get(0).get("antibiotic"));
        assertEquals(1.0, profile.get(0).get("resistanceRate"));
        assertEquals("R", profile.get(0).get("level"));
    }

    @Test
    void shouldClassifyZeroResistanceAsS() {
        Isolate isolate = new Isolate();
        isolate.setArCode("SENS");
        isolate.setBinaryTypingProfile(Map.of("Meropenem", false));

        when(isolateRepository.findByOrganismIdentity("E. coli"))
                .thenReturn(List.of(isolate));

        Map<String, Object> result = amrService.getResistanceProfile("E. coli");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> profile = (List<Map<String, Object>>) result.get("resistanceProfile");

        assertEquals("S", profile.get(0).get("level"));
        assertEquals(0.0, profile.get(0).get("resistanceRate"));
    }

    @Test
    void shouldSortProfileByResistanceRateDescending() {
        Isolate i1 = new Isolate();
        i1.setArCode("ESBL");
        i1.setBinaryTypingProfile(Map.of("DrugA", true, "DrugB", false));

        Isolate i2 = new Isolate();
        i2.setArCode("ESBL");
        i2.setBinaryTypingProfile(Map.of("DrugA", true, "DrugB", true));

        when(isolateRepository.findByOrganismIdentity("Organism"))
                .thenReturn(List.of(i1, i2));

        Map<String, Object> result = amrService.getResistanceProfile("Organism");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> profile = (List<Map<String, Object>>) result.get("resistanceProfile");

        // DrugA: 2/2 = 1.0 (R), DrugB: 1/2 = 0.5 (I) — sorted desc so DrugA first
        assertEquals("DrugA", profile.get(0).get("antibiotic"));
        assertEquals(1.0, profile.get(0).get("resistanceRate"));
        assertEquals("DrugB", profile.get(1).get("antibiotic"));
        assertEquals(0.5, profile.get(1).get("resistanceRate"));
        assertEquals("I", profile.get(1).get("level"));
    }

    // ── getResistanceGenes ────────────────────────────────────────────────────

    @Test
    void shouldReturnGenesForOrganism() {
        when(mockQuery.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{"blaTEM", "Beta-lactam", "Penicillin", "AMR", 5L, 98.5, 99.1}));

        Map<String, Object> result = amrService.getResistanceGenes("Klebsiella pneumoniae", 10);

        assertEquals("Klebsiella pneumoniae", result.get("organism"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> genes = (List<Map<String, Object>>) result.get("genes");
        assertEquals(1, genes.size());
        assertEquals("blaTEM", genes.get(0).get("geneSymbol"));
        assertEquals(5L, genes.get(0).get("occurrenceCount"));
    }

    @Test
    void shouldReturnEmptyGenesListWhenNoneFound() {
        when(mockQuery.getResultList()).thenReturn(List.of());

        Map<String, Object> result = amrService.getResistanceGenes("RareOrganism", 10);

        @SuppressWarnings("unchecked")
        List<?> genes = (List<?>) result.get("genes");
        assertTrue(genes.isEmpty());
    }
}