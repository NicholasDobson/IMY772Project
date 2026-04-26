package za.co.tuks.amrdashboard.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.co.tuks.amrdashboard.backend.service.EtlService;

import java.util.Map;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/etl")
@RequiredArgsConstructor
public class EtlController {

    private final EtlService etlService;

    @PostMapping("/upload-batch")
    public ResponseEntity<?> uploadBatch(
            @RequestParam(value = "epicollect", required = false) MultipartFile epicollect,
            @RequestParam(value = "binaryInfo", required = false) MultipartFile binaryInfo,
            @RequestParam(value = "amrFinder", required = false) MultipartFile amrFinder,
            @RequestParam(value = "starAmr", required = false) MultipartFile starAmr) {

        if (epicollect == null && binaryInfo == null && amrFinder == null && starAmr == null) {
            return ResponseEntity.badRequest().body("No files provided for upload.");
        }

        try {
            // return list of warnings
            List<String> warnings = etlService.processBatch(epicollect, binaryInfo, amrFinder, starAmr);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Batch processed successfully.",
                    "warnings", warnings
            ));
        } catch (IllegalArgumentException e) {
            // validation errors (like missing headers or wrong formats)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Validation Error: " + e.getMessage());
        } catch (Exception e) {
            // system crash
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Critical Failure (Rolled back): " + e.getMessage());
        }
    }
}