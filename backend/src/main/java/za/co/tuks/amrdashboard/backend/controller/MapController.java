package za.co.tuks.amrdashboard.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tuks.amrdashboard.backend.dto.FilterOptionsDTO;
import za.co.tuks.amrdashboard.backend.dto.SiteMarkerDTO;
import za.co.tuks.amrdashboard.backend.dto.SiteSummaryDTO;
import za.co.tuks.amrdashboard.backend.service.MapService;

import java.util.List;

@CrossOrigin(origins = "*") // Allows the Vue frontend to call this during dev
@RestController
@RequestMapping("/api/v1/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    // 1. Get markers with optional filters (repeat params for multi-select)
    // Example: GET /api/v1/map/markers?organism=E.%20coli&organism=K.%20pneumoniae&sirProfile=Resistant
    @GetMapping("/markers")
    public ResponseEntity<List<SiteMarkerDTO>> getMarkers(
            @RequestParam(required = false) List<String> riverName,
            @RequestParam(required = false) List<String> organism,
            @RequestParam(required = false) List<String> sirProfile,
            @RequestParam(required = false) List<String> trip) {

        return ResponseEntity.ok(mapService.getFilteredMarkers(riverName, organism, sirProfile, trip));
    }

    // 2. Get dynamic options for the frontend dropdown menus
    @GetMapping("/filters")
    public ResponseEntity<FilterOptionsDTO> getFilterOptions() {
        return ResponseEntity.ok(mapService.getFilterOptions());
    }

    // 3. Get detailed summary when a marker is clicked
    @GetMapping("/sites/{siteId}/summary")
    public ResponseEntity<?> getSiteSummary(@PathVariable String siteId) {
        try {
            SiteSummaryDTO summary = mapService.getSiteSummary(siteId);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}