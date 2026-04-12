package za.co.tuks.amrdashboard.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.tuks.amrdashboard.backend.service.SitesService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping({"/api", "/api/v1"})
@RequiredArgsConstructor
public class SitesController {

    private final SitesService sitesService;

    @GetMapping("/sites")
    public ResponseEntity<?> getAllSites() {
        try {
            return ResponseEntity.ok(sitesService.getAllSites());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve sites: " + e.getMessage());
        }
    }

    @GetMapping("/sites/{siteId}")
    public ResponseEntity<?> getSiteById(@PathVariable String siteId) {
        try {
            var site = sitesService.getSiteById(siteId);
            if (site == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Site not found");
            }
            return ResponseEntity.ok(site);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve site: " + e.getMessage());
        }
    }

    @GetMapping("/sites/{siteId}/water-samples")
    public ResponseEntity<?> getWaterSamplesBySiteId(
            @PathVariable String siteId,
            @RequestParam(value = "trip", required = false) String trip) {
        try {
            return ResponseEntity.ok(sitesService.getWaterSamplesBySiteId(siteId, trip));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve water samples: " + e.getMessage());
        }
    }

    @GetMapping("/sites/{siteId}/isolates")
    public ResponseEntity<?> getIsolatesBySiteId(@PathVariable String siteId) {
        try {
            return ResponseEntity.ok(sitesService.getIsolatesBySiteId(siteId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve isolates: " + e.getMessage());
        }
    }

    @GetMapping("/analytics/compare")
    public ResponseEntity<?> compareAnalytics(
            @RequestParam("siteA") String siteA,
            @RequestParam("siteB") String siteB,
            @RequestParam(value = "trip", required = false) String trip) {
        try {
            return ResponseEntity.ok(sitesService.compareAnalytics(siteA, siteB, trip));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve analytics: " + e.getMessage());
        }
    }
}
