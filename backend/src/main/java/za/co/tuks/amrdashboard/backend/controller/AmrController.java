package za.co.tuks.amrdashboard.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tuks.amrdashboard.backend.service.AmrService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/analytics/amr")
@RequiredArgsConstructor
public class AmrController {

    private final AmrService amrService;

    @GetMapping("/genes")
    public ResponseEntity<?> getResistanceGenes(
            @RequestParam String organism,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(amrService.getResistanceGenes(organism, limit));
    }

    @GetMapping("/resistance-profile")
    public ResponseEntity<?> getResistanceProfile(
            @RequestParam String organism) {
        return ResponseEntity.ok(amrService.getResistanceProfile(organism));
    }
}
