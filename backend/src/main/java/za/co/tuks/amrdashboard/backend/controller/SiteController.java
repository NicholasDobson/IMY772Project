package za.co.tuks.amrdashboard.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tuks.amrdashboard.backend.service.SiteService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @GetMapping
    public ResponseEntity<?> getAllSites() {
        return ResponseEntity.ok(siteService.getAllSites());
    }
}
