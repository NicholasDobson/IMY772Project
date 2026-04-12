package za.co.tuks.amrdashboard.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tuks.amrdashboard.backend.service.AnalyticsService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/analytics/dashboard")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/sample-count")
    public ResponseEntity<?> getSampleCount() {
        return ResponseEntity.ok(analyticsService.getSampleCount());
    }

    @GetMapping("/incident-rate")
    public ResponseEntity<?> getIncidentRate() {
        return ResponseEntity.ok(analyticsService.getIncidentRate());
    }

    @GetMapping("/monthly-cases")
    public ResponseEntity<?> getMonthlyCases(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(analyticsService.getMonthlyCases(year, month));
    }

    @GetMapping("/monthly-trend")
    public ResponseEntity<?> getMonthlyTrend(
            @RequestParam(defaultValue = "0") int year) {
        int targetYear = year == 0 ? java.time.Year.now().getValue() : year;
        return ResponseEntity.ok(analyticsService.getMonthlyTrend(targetYear));
    }

    @GetMapping("/monthly-trend/available-years")
    public ResponseEntity<?> getAvailableYears() {
        return ResponseEntity.ok(analyticsService.getAvailableYears());
    }

    @GetMapping("/monthly-trend/best-year")
    public ResponseEntity<?> getMonthlyTrendBestYear() {
        return ResponseEntity.ok(analyticsService.getMonthlyTrendBestYear());
    }

    @GetMapping("/top-organisms")
    public ResponseEntity<?> getTopOrganisms(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(analyticsService.getTopOrganisms(limit));
    }

    @GetMapping("/high-risk-sites")
    public ResponseEntity<?> getHighRiskSites() {
        return ResponseEntity.ok(analyticsService.getHighRiskSites());
    }

    @GetMapping("/risk-by-province")
    public ResponseEntity<?> getRiskByProvince() {
        return ResponseEntity.ok(analyticsService.getRiskByProvince());
    }

    @GetMapping("/top-resistance-genes")
    public ResponseEntity<?> getTopResistanceGenes(
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(analyticsService.getTopResistanceGenes(limit));
    }

    @GetMapping("/affected-river-sites")
    public ResponseEntity<?> getAffectedRiverSites(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(analyticsService.getAffectedRiverSites(limit));
    }
}
