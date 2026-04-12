package za.co.tuks.amrdashboard.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.tuks.amrdashboard.backend.model.Site;
import za.co.tuks.amrdashboard.backend.model.WaterSample;
import za.co.tuks.amrdashboard.backend.model.Isolate;
import za.co.tuks.amrdashboard.backend.repository.SiteRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;

    // MDRO ar_code values considered high-risk
    private static final Set<String> MDRO_CODES = Set.of("ESBL", "CRE", "MDRO", "MDR", "VRE");

    public Map<String, Object> getAllSites() {
        List<Site> sites = siteRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Site site : sites) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("siteId", site.getSiteId());
            dto.put("locationName", site.getLocationName());
            dto.put("riverName", site.getRiverName());
            dto.put("latitude", site.getLatitude());
            dto.put("longitude", site.getLongitude());

            // Calculate MDRO count and last sample date from related samples
            long mdroCount = 0;
            String lastSampleDate = null;

            if (site.getWaterSamples() != null) {
                for (WaterSample sample : site.getWaterSamples()) {
                    // Track the most recent sample date
                    if (sample.getCollectionDate() != null) {
                        String dateStr = sample.getCollectionDate().toString();
                        if (lastSampleDate == null || dateStr.compareTo(lastSampleDate) > 0) {
                            lastSampleDate = dateStr;
                        }
                    }
                    // Count MDRO isolates
                    if (sample.getIsolates() != null) {
                        for (Isolate isolate : sample.getIsolates()) {
                            if (isolate.getArCode() != null && MDRO_CODES.contains(isolate.getArCode().toUpperCase())) {
                                mdroCount++;
                            }
                        }
                    }
                }
            }

            // Calculate risk level based on MDRO ratio
            long totalIsolates = site.getWaterSamples() == null ? 0 :
                site.getWaterSamples().stream()
                    .mapToLong(s -> s.getIsolates() == null ? 0 : s.getIsolates().size())
                    .sum();

            dto.put("riskLevel", calculateRiskLevel(mdroCount, totalIsolates));
            dto.put("lastSampleDate", lastSampleDate);
            dto.put("mdroCount", mdroCount);

            result.add(dto);
        }

        return Map.of("sites", result);
    }

    private String calculateRiskLevel(long mdroCount, long totalIsolates) {
        if (totalIsolates == 0) return "LOW";
        double score = (double) mdroCount / totalIsolates * 100;
        if (score > 60) return "HIGH";
        if (score >= 30) return "MED";
        return "LOW";
    }
}
