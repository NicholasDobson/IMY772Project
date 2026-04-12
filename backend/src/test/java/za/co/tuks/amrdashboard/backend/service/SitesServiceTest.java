package za.co.tuks.amrdashboard.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.tuks.amrdashboard.backend.model.AmrSequence;
import za.co.tuks.amrdashboard.backend.model.Isolate;
import za.co.tuks.amrdashboard.backend.model.Site;
import za.co.tuks.amrdashboard.backend.model.WaterSample;
import za.co.tuks.amrdashboard.backend.model.WgsMetrics;
import za.co.tuks.amrdashboard.backend.repository.IsolateRepository;
import za.co.tuks.amrdashboard.backend.repository.SiteRepository;
import za.co.tuks.amrdashboard.backend.repository.WaterSampleRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SitesServiceTest {

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private WaterSampleRepository waterSampleRepository;

    @Mock
    private IsolateRepository isolateRepository;

    @InjectMocks
    private SitesService sitesService;

    @Test
    void shouldReturnAllSitesMapped() {
        Site site = new Site();
        site.setSiteId("A10");
        site.setLocationName("Pretoria");
        site.setRiverName("Apies River");
        site.setLatitude(-25.747);
        site.setLongitude(28.229);

        when(siteRepository.findAll()).thenReturn(List.of(site));

        List<Map<String, Object>> result = sitesService.getAllSites();

        assertEquals(1, result.size());
        assertEquals("A10", result.get(0).get("siteId"));
        assertEquals("Pretoria", result.get(0).get("locationName"));
        assertEquals(28.229, result.get(0).get("longitude"));
    }

    @Test
    void shouldReturnSiteById() {
        Site site = new Site();
        site.setSiteId("A10");
        site.setLocationName("Pretoria");
        site.setRiverName("Apies River");
        site.setLatitude(-25.747);
        site.setLongitude(28.229);

        when(siteRepository.findById("A10")).thenReturn(Optional.of(site));

        Map<String, Object> result = sitesService.getSiteById("A10");

        assertNotNull(result);
        assertEquals("A10", result.get("siteId"));
        assertEquals("Apies River", result.get("riverName"));
    }

    @Test
    void shouldReturnNullWhenSiteMissing() {
        when(siteRepository.findById("MISSING")).thenReturn(Optional.empty());

        Map<String, Object> result = sitesService.getSiteById("MISSING");

        assertNull(result);
    }

    @Test
    void shouldReturnFilteredWaterSamplesMapped() {
        Site site = new Site();
        site.setSiteId("A10");

        WaterSample trip1 = new WaterSample();
        trip1.setSampleId("SAMP-001");
        trip1.setSite(site);
        trip1.setTripIdentifier("Trip 1");
        trip1.setCollectionDate(LocalDate.of(2025, 5, 10));
        trip1.setWaterTemperature(18.5);
        trip1.setPhLevel(7.2);
        trip1.setTds(250.0);
        trip1.setEc(400.0);
        trip1.setDissolvedOxygen(6.5);

        WaterSample trip2 = new WaterSample();
        trip2.setSampleId("SAMP-002");
        trip2.setSite(site);
        trip2.setTripIdentifier("Trip 2");
        trip2.setCollectionDate(LocalDate.of(2025, 6, 10));

        when(waterSampleRepository.findBySite_SiteIdOrderByCollectionDateAsc("A10")).thenReturn(List.of(trip1, trip2));

        List<Map<String, Object>> result = sitesService.getWaterSamplesBySiteId("A10", "Trip 1");

        assertEquals(1, result.size());
        assertEquals("SAMP-001", result.get(0).get("sampleId"));
        assertEquals("Trip 1", result.get(0).get("tripIdentifier"));
        assertEquals("2025-05-10", result.get(0).get("collectionDate"));
    }

    @Test
    void shouldReturnIsolatesMappedBySite() {
        Site site = new Site();
        site.setSiteId("A10");

        WaterSample sample = new WaterSample();
        sample.setSampleId("SAMP-001");
        sample.setSite(site);

        Isolate isolate = new Isolate();
        isolate.setIsolateId("ISO-101");
        isolate.setWaterSample(sample);
        isolate.setIsolateNumber("TSp1H");
        isolate.setOrganismIdentity("Klebsiella pneumoniae");
        isolate.setSourceContext("Spinach at harvest");
        isolate.setArCode("B1");
        isolate.setVirulenceGenes("rmpA, iutA");
        isolate.setBinaryTypingProfile(Map.of(
                "Intl1", true,
                "Intl2", false,
                "Intl3", true,
                "TEM", true,
                "SHV", true
        ));

        when(isolateRepository.findByWaterSample_Site_SiteId("A10")).thenReturn(List.of(isolate));

        List<Map<String, Object>> result = sitesService.getIsolatesBySiteId("A10");

        assertEquals(1, result.size());
        assertEquals("ISO-101", result.get(0).get("isolateId"));
        assertEquals("SAMP-001", result.get(0).get("sampleId"));
        assertEquals("Klebsiella pneumoniae", result.get(0).get("organismIdentity"));
        @SuppressWarnings("unchecked")
        Map<String, Boolean> profile = (Map<String, Boolean>) result.get(0).get("binaryTypingProfile");
        assertEquals(true, profile.get("Intl1"));
    }

    @Test
    void shouldReturnComparisonStatsForTwoSites() {
        Site siteA = new Site();
        siteA.setSiteId("A10");
        siteA.setLocationName("Pretoria");
        siteA.setRiverName("Apies River");

        Site siteB = new Site();
        siteB.setSiteId("B26");
        siteB.setLocationName("Hammanskraal");
        siteB.setRiverName("Apies River");

        WaterSample sampleA = new WaterSample();
        sampleA.setSampleId("SAMP-001");
        sampleA.setSite(siteA);
        sampleA.setTripIdentifier("Trip 1");
        sampleA.setCollectionDate(LocalDate.of(2025, 5, 10));
        sampleA.setWaterTemperature(18.5);
        sampleA.setPhLevel(7.2);
        sampleA.setTds(250.0);
        sampleA.setEc(400.0);
        sampleA.setDissolvedOxygen(6.5);

        WaterSample sampleB = new WaterSample();
        sampleB.setSampleId("SAMP-002");
        sampleB.setSite(siteB);
        sampleB.setTripIdentifier("Trip 1");
        sampleB.setCollectionDate(LocalDate.of(2025, 5, 11));
        sampleB.setWaterTemperature(17.0);
        sampleB.setPhLevel(7.0);
        sampleB.setTds(260.0);
        sampleB.setEc(420.0);
        sampleB.setDissolvedOxygen(6.0);

        Isolate isolateA = new Isolate();
        isolateA.setIsolateId("ISO-101");
        isolateA.setWaterSample(sampleA);
        isolateA.setBinaryTypingProfile(Map.of("Intl1", true));
        isolateA.setAmrSequences(List.of(new AmrSequence(), new AmrSequence()));
        WgsMetrics wgsA = new WgsMetrics();
        wgsA.setQualityStatus("PASS");
        wgsA.setPredictedPhenotype("Resistant");
        wgsA.setPredictedSirProfile("Resistant");
        isolateA.setWgsMetrics(wgsA);

        Isolate isolateB = new Isolate();
        isolateB.setIsolateId("ISO-201");
        isolateB.setWaterSample(sampleB);
        isolateB.setBinaryTypingProfile(Map.of("Intl1", true));
        isolateB.setAmrSequences(List.of(new AmrSequence()));
        WgsMetrics wgsB = new WgsMetrics();
        wgsB.setQualityStatus("PASS");
        wgsB.setPredictedPhenotype("Resistant");
        wgsB.setPredictedSirProfile("Resistant");
        isolateB.setWgsMetrics(wgsB);

        when(siteRepository.findById("A10")).thenReturn(Optional.of(siteA));
        when(siteRepository.findById("B26")).thenReturn(Optional.of(siteB));
        when(waterSampleRepository.findBySite_SiteId("A10")).thenReturn(List.of(sampleA));
        when(waterSampleRepository.findBySite_SiteId("B26")).thenReturn(List.of(sampleB));
        when(isolateRepository.findByWaterSample_SampleIdIn(List.of("SAMP-001"))).thenReturn(List.of(isolateA));
        when(isolateRepository.findByWaterSample_SampleIdIn(List.of("SAMP-002"))).thenReturn(List.of(isolateB));

        List<Map<String, Object>> result = sitesService.compareAnalytics("A10", "B26", null);

        assertEquals(2, result.size());
        assertEquals("A10", result.get(0).get("siteId"));
        assertEquals(1, result.get(0).get("totalSamples"));
        assertEquals(1, result.get(0).get("totalIsolates"));
        assertEquals(1, result.get(0).get("resistantIsolates"));
        assertEquals("B26", result.get(1).get("siteId"));
        assertEquals(1, result.get(1).get("totalSamples"));
        assertEquals(1, result.get(1).get("totalIsolates"));
    }
}
