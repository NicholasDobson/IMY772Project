package za.co.tuks.amrdashboard.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tuks.amrdashboard.backend.dto.AdvisorRequest;
import za.co.tuks.amrdashboard.backend.dto.AdvisorResponse;
import za.co.tuks.amrdashboard.backend.service.AdvisorService;

@RestController
@RequestMapping("/api/v1/advisor")
@RequiredArgsConstructor
public class AdvisorController {

    private final AdvisorService advisorService;

    @PostMapping("/chat")
    public ResponseEntity<AdvisorResponse> chat(@RequestBody AdvisorRequest req, HttpServletRequest http) {
        try {
            String ip = clientIp(http);
            String reply = advisorService.advise(ip, req.getMessage(), req.getContextType(), req.getContextId());
            return ResponseEntity.ok(AdvisorResponse.ok(reply));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AdvisorResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(429).body(AdvisorResponse.fail(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(AdvisorResponse.fail(e.getMessage()));
        }
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
