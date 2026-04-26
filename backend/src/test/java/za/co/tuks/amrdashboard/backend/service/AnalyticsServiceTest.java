package za.co.tuks.amrdashboard.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsServiceTest {

    @Mock
    private EntityManager em;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Mock
    private Query mockQuery;

    @SuppressWarnings("rawtypes")
    @Mock
    private TypedQuery mockTypedQuery;

    @BeforeEach
    void setupQueryMocks() {
        when(em.createQuery(anyString())).thenReturn(mockQuery);
        when(em.createQuery(anyString(), eq(String.class))).thenReturn(mockTypedQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.setMaxResults(anyInt())).thenReturn(mockQuery);
        when(mockTypedQuery.setParameter(anyString(), any())).thenReturn(mockTypedQuery);
        when(mockTypedQuery.setMaxResults(anyInt())).thenReturn(mockTypedQuery);
    }

    // ── getSampleCount ────────────────────────────────────────────────────────

    @Test
    void shouldReturnSampleAndSiteCounts() {
        when(mockQuery.getSingleResult()).thenReturn(42L, 7L);

        Map<String, Object> result = analyticsService.getSampleCount();

        assertEquals(42L, result.get("total"));
        assertEquals(7L, result.get("siteCount"));
    }

    // ── getMonthlyCases ───────────────────────────────────────────────────────

    @Test
    void shouldReturnMonthlyCasesWithUpDirection() {
        when(mockQuery.getSingleResult()).thenReturn(10L, 6L);

        Map<String, Object> result = analyticsService.getMonthlyCases(2025, 5);

        assertEquals(2025, result.get("year"));
        assertEquals(5, result.get("month"));
        assertEquals(10L, result.get("caseCount"));
        assertEquals(6L, result.get("previousMonthCount"));
        assertEquals(4L, result.get("delta"));
        assertEquals("up", result.get("direction"));
    }

    @Test
    void shouldReturnMonthlyCasesWithDownDirection() {
        when(mockQuery.getSingleResult()).thenReturn(3L, 8L);

        Map<String, Object> result = analyticsService.getMonthlyCases(2025, 5);

        assertEquals("down", result.get("direction"));
        assertEquals(-5L, result.get("delta"));
    }

    @Test
    void shouldHandleJanuaryBoundaryForPreviousMonth() {
        when(mockQuery.getSingleResult()).thenReturn(5L, 5L);

        Map<String, Object> result = analyticsService.getMonthlyCases(2025, 1);

        // January's previous month should be December of prior year — just verify it doesn't throw
        assertNotNull(result);
        assertEquals("stable", result.get("direction"));
    }

    // ── getRiskByProvince ─────────────────────────────────────────────────────

    @Test
    void shouldReturnEmptyProvincesWhenNoData() {
        when(mockQuery.getResultList()).thenReturn(List.of());

        Map<String, Object> result = analyticsService.getRiskByProvince();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> provinces = (List<Map<String, Object>>) result.get("provinces");
        assertTrue(provinces.isEmpty());
    }

    @Test
    void shouldComputeRiskLevelFromRiverData() {
        // River "Apies River" → Gauteng; 4 isolates, 3 MDRO → rate=0.75 → HIGH
        when(mockQuery.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{"Apies River", 4L, 3L}));

        Map<String, Object> result = analyticsService.getRiskByProvince();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> provinces = (List<Map<String, Object>>) result.get("provinces");
        assertEquals(1, provinces.size());
        assertEquals("Gauteng", provinces.get(0).get("name"));
        assertEquals("HIGH", provinces.get(0).get("riskLevel"));
        assertEquals(3L, provinces.get(0).get("mdroCount"));
    }

    // ── getHighRiskSites ──────────────────────────────────────────────────────

    @Test
    void shouldCountHighRiskSitesAndNewThisMonth() {
        when(mockTypedQuery.getResultList()).thenReturn(List.of("A10", "B26"));

        LocalDate now = LocalDate.now();
        when(mockQuery.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{"A10", now}));  // first detection this month

        Map<String, Object> result = analyticsService.getHighRiskSites();

        assertEquals(2, result.get("count"));
        assertEquals(1L, result.get("newThisMonth"));
    }

    @Test
    void shouldReturnZeroNewThisMonthWhenNoneRecent() {
        when(mockTypedQuery.getResultList()).thenReturn(List.of("A10"));

        when(mockQuery.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{"A10", LocalDate.of(2020, 1, 1)}));

        Map<String, Object> result = analyticsService.getHighRiskSites();

        assertEquals(1, result.get("count"));
        assertEquals(0L, result.get("newThisMonth"));
    }
}