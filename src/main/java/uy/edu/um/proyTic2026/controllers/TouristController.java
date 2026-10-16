package uy.edu.um.proyTic2026.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.dtos.TouristRegistrationRequest;
import uy.edu.um.proyTic2026.services.TouristService;

@RestController
@RequestMapping("/api/tourists")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class TouristController {
    private final TouristService touristService;

    @PostMapping("/register")
    public ResponseEntity<String> registerTourist(@Valid @RequestBody TouristRegistrationRequest request) {
        touristService.registerTourist(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Cuenta creada correctamente");
    }
}
