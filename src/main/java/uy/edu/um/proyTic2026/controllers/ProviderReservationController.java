package uy.edu.um.proyTic2026.controllers;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.entities.Provider;
import uy.edu.um.proyTic2026.repositories.UserRepository;
import uy.edu.um.proyTic2026.services.ProviderReservationService;

import java.util.Map;

// Reservas que recibió el prestador que inició sesión.
@RestController
@RequestMapping("/api/reservations/provider")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class ProviderReservationController {
    private final ProviderReservationService providerReservationService;
    private final UserRepository userRepository;

    // GET /api/reservations/provider
    @GetMapping
    public ResponseEntity<?> mine(HttpSession session) {
        Provider provider = provider(session);
        if (provider == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Iniciá sesión como prestador para ver las reservas de tus actividades."));
        }
        return ResponseEntity.ok(providerReservationService.reservations(provider.getId()));
    }

    // El id del usuario se guarda en la sesión al iniciar sesión; acá se comprueba que sea un prestador.
    private Provider provider(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long userId)) return null;
        return userRepository.findById(userId).filter(Provider.class::isInstance).map(Provider.class::cast).orElse(null);
    }
}
