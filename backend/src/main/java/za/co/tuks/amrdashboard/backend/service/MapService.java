package za.co.tuks.amrdashboard.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.tuks.amrdashboard.backend.dto.FilterOptionsDTO;
import za.co.tuks.amrdashboard.backend.dto.SiteMarkerDTO;
import za.co.tuks.amrdashboard.backend.dto.SiteSummaryDTO;
import za.co.tuks.amrdashboard.backend.model.Isolate;
import za.co.tuks.amrdashboard.backend.model.Site;
import za.co.tuks.amrdashboard.backend.model.WaterSample;
import za.co.tuks.amrdashboard.backend.repository.IsolateRepository;
import za.co.tuks.amrdashboard.backend.repository.SiteRepository;
import za.co.tuks.amrdashboard.backend.repository.WaterSampleRepository;
import za.co.tuks.amrdashboard.backend.repository.WgsMetricsRepository;
import za.co.tuks.amrdashboard.backend.util.MapSiteRiskUtil;
import za.co.tuks.amrdashboard.backend.util.MapSiteRiskUtil.SiteRisk;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapService {

    private final SiteRepository siteRepository;
    private final IsolateRepository isolateRepository;
    private final WgsMetricsRepository wgsMetricsRepository;
    private final WaterSampleRepository waterSampleRepository;

    @Transactional(readOnly = true)
    public List<SiteMarkerDTO> getFilteredMarkers(
            List<String> riverNames,
            List<String> organisms,
            List<String> sirProfiles,
            List<String> tripIds) {
        List<Site> sites = siteRepository.findFilteredSites(
                normalizeFilterList(riverNames),
                normalizeFilterList(organisms),
                normalizeFilterList(sirProfiles),
                normalizeFilterList(tripIds)
        );

        return sites.stream()
                .map(site -> {
                    SiteRisk risk = MapSiteRiskUtil.assess(site);
                    return new SiteMarkerDTO(
                            site.getSiteId(),
                            site.getLocationName(),
                            site.getRiverName(),
                            site.getLatitude(),
                            site.getLongitude(),
                            risk.level(),
                            risk.colorHex(),
                            risk.resistantPercent(),
                            risk.totalWgs()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FilterOptionsDTO getFilterOptions() {
        List<String> rivers = siteRepository.findDistinctRivers();
        List<String> organisms = isolateRepository.findDistinctOrganisms();
        List<String> sirProfiles = wgsMetricsRepository.findDistinctSirProfiles();
        List<String> trips = waterSampleRepository.findDistinctTripIdentifiers();

        return new FilterOptionsDTO(rivers, organisms, sirProfiles, trips);
    }

    @Transactional(readOnly = true)
    public SiteSummaryDTO getSiteSummary(String siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("Site not found with ID: " + siteId));

        int totalSamples = site.getWaterSamples() != null ? site.getWaterSamples().size() : 0;

        List<String> detectedOrganisms = site.getWaterSamples().stream()
                .flatMap(ws -> ws.getIsolates().stream())
                .map(Isolate::getOrganismIdentity)
                .filter(org -> org != null && !org.isBlank())
                .distinct()
                .collect(Collectors.toList());

        String lastSampled = site.getWaterSamples().stream()
                .map(WaterSample::getCollectionDate)
                .filter(date -> date != null)
                .max(LocalDate::compareTo)
                .map(LocalDate::toString)
                .orElse("No dates recorded");

        SiteRisk risk = MapSiteRiskUtil.assess(site);

        return new SiteSummaryDTO(
                site.getSiteId(),
                site.getLocationName(),
                site.getRiverName(),
                totalSamples,
                detectedOrganisms,
                lastSampled,
                risk.level(),
                risk.colorHex(),
                risk.headline(),
                risk.detail(),
                risk.resistantPercent(),
                risk.resistantCount(),
                risk.totalWgs(),
                risk.latestPh(),
                risk.latestDo()
        );
    }

    private List<String> normalizeFilterList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = values.stream()
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .toList();
        return normalized.isEmpty() ? null : normalized;
    }
}
