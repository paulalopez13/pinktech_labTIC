package uy.edu.um.proyTic2026.controllers;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.dtos.ProviderApplicationResponse;
import uy.edu.um.proyTic2026.entities.Administrator;
import uy.edu.um.proyTic2026.entities.ProviderStatus;
import uy.edu.um.proyTic2026.repositories.UserRepository;
import uy.edu.um.proyTic2026.services.ProviderApprovalService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/providers")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class ProviderApprovalController {
    private final ProviderApprovalService approvalService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> list(HttpSession session) {
        if (!isAdministrator(session)) return forbidden();
        return ResponseEntity.ok(Map.of(
                "pending", approvalService.listByStatus(ProviderStatus.PENDING),
                "reviewed", java.util.stream.Stream.concat(
                                approvalService.listByStatus(ProviderStatus.APPROVED).stream(),
                                approvalService.listByStatus(ProviderStatus.REJECTED).stream())
                        .sorted(java.util.Comparator.comparing(ProviderApplicationResponse::id))
                        .toList()));
    }

    @PostMapping("/{providerId}/approve")
    public ResponseEntity<?> approve(@PathVariable Long providerId, HttpSession session) {
        return decide(providerId, ProviderStatus.APPROVED, session);
    }

    @PostMapping("/{providerId}/reject")
    public ResponseEntity<?> reject(@PathVariable Long providerId, HttpSession session) {
        return decide(providerId, ProviderStatus.REJECTED, session);
    }

    private ResponseEntity<?> decide(Long providerId, ProviderStatus decision, HttpSession session) {
        if (!isAdministrator(session)) return forbidden();
        try {
            ProviderApplicationResponse response = approvalService.decide(providerId, decision);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    private boolean isAdministrator(HttpSession session) {
        Object id = session.getAttribute("userId");
        return id instanceof Long userId && userRepository.findById(userId).orElse(null) instanceof Administrator;
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Solo un administrador puede revisar prestadores"));
    }
}
