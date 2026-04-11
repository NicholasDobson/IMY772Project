package za.co.tuks.amrdashboard.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.co.tuks.amrdashboard.backend.model.FileType;
import za.co.tuks.amrdashboard.backend.service.EtlService;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/etl")
@RequiredArgsConstructor
public class EtlController {

    private final EtlService etlService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadExcelFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") FileType fileType) {
        
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty.");
        }

        try {
            etlService.processExcelFile(file, fileType);
            return ResponseEntity.ok("File uploaded and processed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process file: " + e.getMessage());
        }
    }
    //Nic mappings for Tay
    @GetMapping("/sites")
    public ResponseEntity<?> getAllSites() {
        try {
            return ResponseEntity.ok(etlService.getAllSites());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve sites: " + e.getMessage());
        }
    }

    @GetMapping("/sites/{siteId}")
    public ResponseEntity<?> getSiteById(@PathVariable String siteId) {
        try {
            Map<String, Object> site = etlService.getSiteById(siteId);
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
            @RequestParam(value = "trip", required = false) String trip
    ) {
        try {
            return ResponseEntity.ok(etlService.getWaterSamplesBySiteId(siteId, trip));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve water samples: " + e.getMessage());
        }
    }

    @GetMapping("/sites/{siteId}/isolates")
    public ResponseEntity<?> getIsolatesBySiteId(@PathVariable String siteId) {
        try {
            return ResponseEntity.ok(etlService.getIsolatesBySiteId(siteId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve isolates: " + e.getMessage());
        }
    }

    @GetMapping("/analytics/compare")
    public ResponseEntity<?> compareAnalytics( // Need the site parameters to compare and optional trip parameter.
            @RequestParam("siteA") String siteA,
            @RequestParam("siteB") String siteB,
            @RequestParam(value = "trip", required = false) String trip
    ) {
        try {
            return ResponseEntity.ok(etlService.compareAnalytics(siteA, siteB, trip));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve analytics: " + e.getMessage());
        }
    }
    //




}