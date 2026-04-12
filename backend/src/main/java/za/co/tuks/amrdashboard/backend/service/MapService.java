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
import za.co.tuks.amrdashboard.backend.repository.WgsMetricsRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapService {

    private final SiteRepository siteRepository;
    private final IsolateRepository isolateRepository;
    private final WgsMetricsRepository wgsMetricsRepository;

    @Transactional(readOnly = true)
    public List<SiteMarkerDTO> getFilteredMarkers(String riverName, String organism, String sirProfile) {
        List<Site> sites = siteRepository.findFilteredSites(riverName, organism, sirProfile);
        
        return sites.stream()
                .map(site -> new SiteMarkerDTO(
                        site.getSiteId(),
                        site.getLocationName(),
                        site.getRiverName(),
                        site.getLatitude(),
                        site.getLongitude()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FilterOptionsDTO getFilterOptions() {
        // Dynamically fetching options directly from the database
        List<String> rivers = siteRepository.findDistinctRivers();
        List<String> organisms = isolateRepository.findDistinctOrganisms();
        List<String> sirProfiles = wgsMetricsRepository.findDistinctSirProfiles();
        
        return new FilterOptionsDTO(rivers, organisms, sirProfiles);
    }

    @Transactional(readOnly = true)
    public SiteSummaryDTO getSiteSummary(String siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("Site not found with ID: " + siteId));

        int totalSamples = site.getWaterSamples() != null ? site.getWaterSamples().size() : 0;

        // Traverse relations to get all unique organisms ever found at this site
        List<String> detectedOrganisms = site.getWaterSamples().stream()
                .flatMap(ws -> ws.getIsolates().stream())
                .map(Isolate::getOrganismIdentity)
                .filter(org -> org != null && !org.isBlank())
                .distinct()
                .collect(Collectors.toList());

        // Find the most recent date this site was sampled
        String lastSampled = site.getWaterSamples().stream()
                .map(WaterSample::getCollectionDate)
                .filter(date -> date != null)
                .max(LocalDate::compareTo)
                .map(LocalDate::toString)
                .orElse("No dates recorded");

        return new SiteSummaryDTO(
                site.getSiteId(),
                site.getLocationName(),
                site.getRiverName(),
                totalSamples,
                detectedOrganisms,
                lastSampled
        );
    }
}