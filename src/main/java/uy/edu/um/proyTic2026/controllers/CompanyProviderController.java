package uy.edu.um.proyTic2026.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.dtos.CompanyProviderRegistrationRequest;
import uy.edu.um.proyTic2026.services.CompanyProviderService;

@RestController
@RequestMapping("/api/providers/companies")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})

public class CompanyProviderController {

    private final CompanyProviderService companyProviderService;

    @PostMapping("/register")
    public ResponseEntity<String> registerCompany(@Valid @RequestBody CompanyProviderRegistrationRequest request) {
        companyProviderService.registerCompany(request);

        return ResponseEntity.status(HttpStatus.CREATED).body("Solicitud de registro enviada correctamente");
    }
}
