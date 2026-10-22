package uy.edu.um.proyTic2026.controllers;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.dtos.AdministratorRegistrationRequest;
import uy.edu.um.proyTic2026.entities.Administrator;
import uy.edu.um.proyTic2026.repositories.UserRepository;
import uy.edu.um.proyTic2026.services.AdministratorService;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/administrators")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class AdministratorController {
    private final AdministratorService administratorService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AdministratorRegistrationRequest request, HttpSession session) {
        Object id = session.getAttribute("userId");
        boolean isAdministrator = id instanceof Long userId
                && userRepository.findById(userId).orElse(null) instanceof Administrator;
        if (!isAdministrator) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Solo un administrador puede dar de alta a otro administrador."));
        }
        try {
            administratorService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Administrador creado correctamente."));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }
}
