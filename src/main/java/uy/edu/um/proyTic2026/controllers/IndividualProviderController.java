package uy.edu.um.proyTic2026.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.dtos.IndividualProviderRegistrationRequest;
import uy.edu.um.proyTic2026.services.IndividualProviderService;

@RestController
@RequestMapping("/api/providers/individuals")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class IndividualProviderController {
    private final IndividualProviderService individualProviderService;

    @PostMapping("/register")
    public ResponseEntity<String> registerIndividual(
            @Valid @RequestBody IndividualProviderRegistrationRequest request) {
        individualProviderService.registerIndividual(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Solicitud de registro enviada correctamente");
    }
}
