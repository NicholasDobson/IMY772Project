package za.co.tuks.amrdashboard.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.tuks.amrdashboard.backend.model.Isolate;
import za.co.tuks.amrdashboard.backend.model.Site;
import za.co.tuks.amrdashboard.backend.model.WaterSample;
import za.co.tuks.amrdashboard.backend.model.WgsMetrics;
import za.co.tuks.amrdashboard.backend.repository.IsolateRepository;
import za.co.tuks.amrdashboard.backend.repository.SiteRepository;
import za.co.tuks.amrdashboard.backend.repository.WaterSampleRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SitesService {

    private final SiteRepository siteRepository;
    private final WaterSampleRepository waterSampleRepository;
    private final IsolateRepository isolateRepository;

    public List<Map<String, Object>> getAllSites() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Site site : siteRepository.findAll()) {
            result.add(mapSite(site));
        }
        return result;
    }

    public Map<String, Object> getSiteById(String siteId) {
        Site site = siteRepository.findById(siteId).orElse(null);
        return site == null ? null : mapSite(site);
    }

    public List<Map<String, Object>> getWaterSamplesBySiteId(String siteId, String trip) {
        List<WaterSample> samples = waterSampleRepository.findBySite_SiteIdOrderByCollectionDateAsc(siteId);
        if (trip != null && !trip.isBlank()) {
            samples = samples.stream()
                    .filter(sample -> trip.equals(sample.getTripIdentifier()))
                    .toList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (WaterSample sample : samples) {
            result.add(mapWaterSample(sample));
        }
        return result;
    }

    public List<Map<String, Object>> getIsolatesBySiteId(String siteId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Isolate isolate : isolateRepository.findByWaterSample_Site_SiteId(siteId)) {
            result.add(mapIsolate(isolate));
        }
        return result;
    }

    public List<Map<String, Object>> compareAnalytics(String siteAId, String siteBId, String trip) {
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(aggregateSiteStats(siteAId, trip));
        result.add(aggregateSiteStats(siteBId, trip));
        return result;
    }

    private Map<String, Object> aggregateSiteStats(String siteId, String trip) {
        Map<String, Object> stats = new HashMap<>();
        Site site = siteRepository.findById(siteId).orElse(null);
        if (site == null) {
            return stats;
        }

        stats.put("siteId", site.getSiteId());
        stats.put("locationName", site.getLocationName());
        stats.put("riverName", site.getRiverName());

        List<WaterSample> samples = waterSampleRepository.findBySite_SiteId(siteId);
        if (trip != null && !trip.isBlank()) {
            samples = samples.stream()
                    .filter(sample -> trip.equals(sample.getTripIdentifier()))
                    .toList();
        }
        stats.put("totalSamples", samples.size());

        List<Isolate> isolates;
        if (samples.isEmpty()) {
            isolates = List.of();
        } else {
            Set<String> sampleIds = samples.stream().map(WaterSample::getSampleId).collect(Collectors.toSet());
            isolates = isolateRepository.findByWaterSample_SampleIdIn(new ArrayList<>(sampleIds));
        }

        stats.put("totalIsolates", isolates.size());
        stats.put("avgPh", average(samples.stream().map(WaterSample::getPhLevel).toList()));
        stats.put("avgTemperature", average(samples.stream().map(WaterSample::getWaterTemperature).toList()));
        stats.put("avgTds", average(samples.stream().map(WaterSample::getTds).toList()));
        stats.put("avgEc", average(samples.stream().map(WaterSample::getEc).toList()));
        stats.put("avgDo", average(samples.stream().map(WaterSample::getDissolvedOxygen).toList()));

        long intl1Pos = isolates.stream()
                .filter(isolate -> isolate.getBinaryTypingProfile() != null
                        && Boolean.TRUE.equals(isolate.getBinaryTypingProfile().get("Intl1")))
                .count();
        stats.put("intl1PositiveRate", isolates.isEmpty() ? 0.0 : (intl1Pos * 100.0 / isolates.size()));

        int amrGeneHits = isolates.stream()
                .mapToInt(isolate -> isolate.getAmrSequences() == null ? 0 : isolate.getAmrSequences().size())
                .sum();
        stats.put("amrGeneHits", amrGeneHits);

        long wgsTotal = isolates.stream().filter(isolate -> isolate.getWgsMetrics() != null).count();
        long wgsPass = isolates.stream()
                .filter(isolate -> isolate.getWgsMetrics() != null
                        && "PASS".equalsIgnoreCase(isolate.getWgsMetrics().getQualityStatus()))
                .count();
        stats.put("wgsPassRate", wgsTotal == 0 ? 0.0 : (wgsPass * 100.0 / wgsTotal));

        long resistant = isolates.stream().filter(isolate -> {
            WgsMetrics wgs = isolate.getWgsMetrics();
            if (wgs == null) {
                return false;
            }
            String pheno = wgs.getPredictedPhenotype();
            String sir = wgs.getPredictedSirProfile();
            return (pheno != null && pheno.toLowerCase().contains("resistant"))
                    || (sir != null && sir.toLowerCase().contains("resistant"))
                    || (pheno != null && pheno.contains("R"))
                    || (sir != null && sir.contains("R"));
        }).count();
        stats.put("resistantIsolates", resistant);

        return stats;
    }

    private Map<String, Object> mapSite(Site site) {
        Map<String, Object> mapped = new HashMap<>();
        mapped.put("siteId", site.getSiteId());
        mapped.put("locationName", site.getLocationName());
        mapped.put("riverName", site.getRiverName());
        mapped.put("latitude", site.getLatitude());
        mapped.put("longitude", site.getLongitude());
        return mapped;
    }

    private Map<String, Object> mapWaterSample(WaterSample sample) {
        Map<String, Object> mapped = new HashMap<>();
        mapped.put("sampleId", sample.getSampleId());
        mapped.put("siteId", sample.getSite() != null ? sample.getSite().getSiteId() : null);
        mapped.put("tripIdentifier", sample.getTripIdentifier());
        mapped.put("collectionDate", sample.getCollectionDate() != null ? sample.getCollectionDate().toString() : null);
        mapped.put("waterTemperature", sample.getWaterTemperature());
        mapped.put("phLevel", sample.getPhLevel());
        mapped.put("tds", sample.getTds());
        mapped.put("ec", sample.getEc());
        mapped.put("dissolvedOxygen", sample.getDissolvedOxygen());
        return mapped;
    }

    private Map<String, Object> mapIsolate(Isolate isolate) {
        Map<String, Object> mapped = new HashMap<>();
        mapped.put("isolateId", isolate.getIsolateId());
        mapped.put("sampleId", isolate.getWaterSample() != null ? isolate.getWaterSample().getSampleId() : null);
        mapped.put("isolateNumber", isolate.getIsolateNumber());
        mapped.put("organismIdentity", isolate.getOrganismIdentity());
        mapped.put("sourceContext", isolate.getSourceContext());
        mapped.put("arCode", isolate.getArCode());
        mapped.put("virulenceGenes", isolate.getVirulenceGenes());
        mapped.put("binaryTypingProfile", isolate.getBinaryTypingProfile());
        return mapped;
    }

    private Double average(List<Double> values) {
        return values.stream()
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(Double.NaN);
    }
}
