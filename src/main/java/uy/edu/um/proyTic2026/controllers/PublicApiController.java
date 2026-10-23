package uy.edu.um.proyTic2026.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.services.PublicApiService;

// API abierta para que otras plataformas consulten la oferta de Uruguay Mnatural.
// Solo tiene consultas (GET), no necesita sesión y acepta pedidos desde cualquier origen.
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicApiController {
    private final PublicApiService publicApiService;

    // GET /api/public/providers
    @GetMapping("/providers")
    public ResponseEntity<?> providers() {
        return ResponseEntity.ok(publicApiService.providers());
    }

    // GET /api/public/providers/verify?document=44444444
    // Acepta un RUT (empresa) o una cédula (prestador particular).
    @GetMapping("/providers/verify")
    public ResponseEntity<?> verify(@RequestParam String document) {
        return ResponseEntity.ok(publicApiService.verify(document));
    }

    // GET /api/public/activities/search?q=ballenas
    @GetMapping("/activities/search")
    public ResponseEntity<?> search(@RequestParam("q") String keyword) {
        return ResponseEntity.ok(publicApiService.search(keyword));
    }
}
