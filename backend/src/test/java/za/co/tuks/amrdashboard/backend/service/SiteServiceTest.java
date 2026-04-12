package za.co.tuks.amrdashboard.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.tuks.amrdashboard.backend.model.Isolate;
import za.co.tuks.amrdashboard.backend.model.Site;
import za.co.tuks.amrdashboard.backend.model.WaterSample;
import za.co.tuks.amrdashboard.backend.repository.SiteRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

    @Mock
    private SiteRepository siteRepository;

    @InjectMocks
    private SiteService siteService;

    @Test
    void shouldReturnAllSitesMapped() {
        Site site = buildSite("A10", "Pretoria", "Apies River", -25.747, 28.229, List.of());

        when(siteRepository.findAll()).thenReturn(List.of(site));

        Map<String, Object> result = siteService.getAllSites();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sites = (List<Map<String, Object>>) result.get("sites");
        assertEquals(1, sites.size());
        assertEquals("A10", sites.get(0).get("siteId"));
        assertEquals("Pretoria", sites.get(0).get("locationName"));
    }

    @Test
    void shouldReturnLowRiskWhenNoIsolates() {
        Site site = buildSite("A10", "Pretoria", "Apies River", -25.747, 28.229, List.of());

        when(siteRepository.findAll()).thenReturn(List.of(site));

        Map<String, Object> result = siteService.getAllSites();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sites = (List<Map<String, Object>>) result.get("sites");
        assertEquals("LOW", sites.get(0).get("riskLevel"));
        assertEquals(0L, sites.get(0).get("mdroCount"));
    }

    @Test
    void shouldReturnHighRiskWhenMostIsolatesAreMdro() {
        Isolate mdroIsolate1 = new Isolate();
        mdroIsolate1.setArCode("ESBL");

        Isolate mdroIsolate2 = new Isolate();
        mdroIsolate2.setArCode("CRE");

        Isolate sensitiveIsolate = new Isolate();
        sensitiveIsolate.setArCode("SENS");

        WaterSample sample = new WaterSample();
        sample.setCollectionDate(LocalDate.of(2025, 5, 10));
        sample.setIsolates(List.of(mdroIsolate1, mdroIsolate2, sensitiveIsolate));

        Site site = buildSite("B26", "Hammanskraal", "Apies River", -25.5, 28.3, List.of(sample));

        when(siteRepository.findAll()).thenReturn(List.of(site));

        Map<String, Object> result = siteService.getAllSites();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sites = (List<Map<String, Object>>) result.get("sites");
        // 2 MDRO out of 3 isolates = 66.7% → HIGH
        assertEquals("HIGH", sites.get(0).get("riskLevel"));
        assertEquals(2L, sites.get(0).get("mdroCount"));
    }

    @Test
    void shouldTrackLatestSampleDate() {
        WaterSample older = new WaterSample();
        older.setCollectionDate(LocalDate.of(2024, 1, 15));
        older.setIsolates(List.of());

        WaterSample newer = new WaterSample();
        newer.setCollectionDate(LocalDate.of(2025, 6, 20));
        newer.setIsolates(List.of());

        Site site = buildSite("A10", "Pretoria", "Apies River", -25.747, 28.229, List.of(older, newer));

        when(siteRepository.findAll()).thenReturn(List.of(site));

        Map<String, Object> result = siteService.getAllSites();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sites = (List<Map<String, Object>>) result.get("sites");
        assertEquals("2025-06-20", sites.get(0).get("lastSampleDate"));
    }

    @Test
    void shouldReturnEmptyListWhenNoSites() {
        when(siteRepository.findAll()).thenReturn(List.of());

        Map<String, Object> result = siteService.getAllSites();

        @SuppressWarnings("unchecked")
        List<?> sites = (List<?>) result.get("sites");
        assertTrue(sites.isEmpty());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Site buildSite(String id, String location, String river,
                            double lat, double lon, List<WaterSample> samples) {
        Site site = new Site();
        site.setSiteId(id);
        site.setLocationName(location);
        site.setRiverName(river);
        site.setLatitude(lat);
        site.setLongitude(lon);
        site.setWaterSamples(samples);
        return site;
    }
}
