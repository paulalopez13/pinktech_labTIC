package uy.edu.um.proyTic2026.controllers;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.dtos.ProfileUpdateRequest;
import uy.edu.um.proyTic2026.services.LoginException;
import uy.edu.um.proyTic2026.services.ProfileService;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class ProfileController {
    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<?> getProfile(HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) return unauthorized();
        try {
            return ResponseEntity.ok(profileService.getProfile(userId));
        } catch (LoginException error) {
            return unauthorized();
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest request, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) return unauthorized();
        try {
            return ResponseEntity.ok(profileService.updateProfile(userId, request));
        } catch (LoginException error) {
            return unauthorized();
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute("userId");
        return userId instanceof Long id ? id : null;
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("message", "Iniciá sesión para continuar"));
    }
}
