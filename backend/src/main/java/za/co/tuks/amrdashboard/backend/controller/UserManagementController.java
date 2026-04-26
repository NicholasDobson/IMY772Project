package za.co.tuks.amrdashboard.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tuks.amrdashboard.backend.dto.CognitoUserDTO;
import za.co.tuks.amrdashboard.backend.service.UserManagementService;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    public ResponseEntity<List<CognitoUserDTO>> listUsers() {
        return ResponseEntity.ok(userManagementService.listUsers());
    }

    @PostMapping("/{username}/promote")
    public ResponseEntity<Void> promote(@PathVariable String username) {
        userManagementService.addToAdmins(username);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{username}/promote")
    public ResponseEntity<Void> demote(@PathVariable String username) {
        userManagementService.removeFromAdmins(username);
        return ResponseEntity.ok().build();
    }
}
