package uy.edu.um.proyTic2026.controllers;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.dtos.GroupReservationRequest;
import uy.edu.um.proyTic2026.dtos.ReviewRequest;
import uy.edu.um.proyTic2026.entities.Tourist;
import uy.edu.um.proyTic2026.repositories.UserRepository;
import uy.edu.um.proyTic2026.services.ActivityService;

import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class ReservationController {
    private final ActivityService activityService;
    private final UserRepository userRepository;

    @GetMapping("/mine")
    public ResponseEntity<?> mine(HttpSession session) {
        Tourist tourist = tourist(session);
        if (tourist == null) return forbidden("Iniciá sesión como turista para ver tus reservas.");
        return ResponseEntity.ok(activityService.reservations(tourist.getId()));
    }

    @PostMapping("/activities/{activityId}/sandbox-payment")
    public ResponseEntity<?> sandboxPayment(@PathVariable Long activityId,
                                             @Valid @RequestBody GroupReservationRequest request,
                                             HttpSession session) {
        Tourist tourist = tourist(session);
        if (tourist == null) return forbidden("Iniciá sesión como turista para reservar actividades.");
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "reservation", activityService.reserveSandbox(tourist.getId(), activityId, request),
                    "status", "APPROVED",
                    "total", activityService.get(activityId).price().multiply(BigDecimal.valueOf(request.participants().size())),
                    "message", "Pago de prueba aprobado. No se realizó ningún cobro real."
            ));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @PostMapping("/{reservationId}/review")
    public ResponseEntity<?> review(@PathVariable Long reservationId, @Valid @RequestBody ReviewRequest request,
                                    HttpSession session) {
        Tourist tourist = tourist(session);
        if (tourist == null) return forbidden("Iniciá sesión como turista para dejar una opinión.");
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(activityService.review(tourist.getId(), reservationId, request));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    private Tourist tourist(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long userId)) return null;
        return userRepository.findById(userId).filter(Tourist.class::isInstance).map(Tourist.class::cast).orElse(null);
    }

    private ResponseEntity<Map<String, String>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", message));
    }
}
