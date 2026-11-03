package uy.edu.um.proyTic2026.controllers;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.dtos.GroupReservationRequest;
import uy.edu.um.proyTic2026.dtos.RescheduleReservationRequest;
import uy.edu.um.proyTic2026.dtos.ReviewRequest;
import uy.edu.um.proyTic2026.entities.Tourist;
import uy.edu.um.proyTic2026.repositories.UserRepository;
import uy.edu.um.proyTic2026.services.ActivityService;

import java.util.Map;

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

    @PostMapping("/activities/{activityId}/hold")
    public ResponseEntity<?> createHold(@PathVariable Long activityId,
                                        @Valid @RequestBody GroupReservationRequest request,
                                        HttpSession session) {
        Tourist tourist = tourist(session);
        if (tourist == null) return forbidden("Iniciá sesión como turista para reservar actividades.");
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(activityService.reserveSandbox(tourist.getId(), activityId, request));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @PostMapping("/activities/{activityId}/waitlist")
    public ResponseEntity<?> joinWaitlist(@PathVariable Long activityId,
                                          @Valid @RequestBody GroupReservationRequest request, HttpSession session) {
        Tourist tourist = tourist(session);
        if (tourist == null) return forbidden("Iniciá sesión como turista para anotarte a la lista de espera.");
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(activityService.joinWaitlist(tourist.getId(), activityId, request));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @PostMapping("/{reservationId}/stripe-checkout")
    public ResponseEntity<?> createStripeCheckout(@PathVariable Long reservationId, HttpSession session) {
        Tourist tourist = tourist(session);
        if (tourist == null) return forbidden("Iniciá sesión como turista para pagar una reserva.");
        try {
            return ResponseEntity.ok(activityService.createStripeCheckout(tourist.getId(), reservationId));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @GetMapping("/{reservationId}/stripe-checkout")
    public ResponseEntity<?> verifyStripeCheckout(@PathVariable Long reservationId, @RequestParam String sessionId, HttpSession session) {
        Tourist tourist = tourist(session);
        if (tourist == null) return forbidden("Iniciá sesión como turista para consultar el pago.");
        try {
            return ResponseEntity.ok(activityService.verifyStripeCheckout(tourist.getId(), reservationId, sessionId));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @PostMapping("/{reservationId}/stripe-checkout/cancel")
    public ResponseEntity<?> cancelStripeCheckout(@PathVariable Long reservationId, HttpSession session) {
        Tourist tourist = tourist(session);
        if (tourist == null) return forbidden("Iniciá sesión como turista para cancelar el intento de pago.");
        try {
            return ResponseEntity.ok(activityService.cancelStripeCheckout(tourist.getId(), reservationId));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long reservationId, HttpSession session) {
        Tourist tourist = tourist(session);
        if (tourist == null) return forbidden("Iniciá sesión como turista para cancelar una reserva.");
        try {
            return ResponseEntity.ok(activityService.cancelReservation(tourist.getId(), reservationId));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @PostMapping("/{reservationId}/waitlist-offer")
    public ResponseEntity<?> answerWaitlistOffer(@PathVariable Long reservationId,
                                                  @RequestBody Map<String, Boolean> request, HttpSession session) {
        Tourist tourist = tourist(session);
        if (tourist == null) return forbidden("Iniciá sesión como turista para responder a la oferta.");
        try {
            return ResponseEntity.ok(activityService.answerWaitlistOffer(tourist.getId(), reservationId,
                    Boolean.TRUE.equals(request.get("accept"))));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @PutMapping("/{reservationId}/reschedule")
    public ResponseEntity<?> reschedule(@PathVariable Long reservationId,
                                        @Valid @RequestBody RescheduleReservationRequest request,
                                        HttpSession session) {
        Tourist tourist = tourist(session);
        if (tourist == null) return forbidden("Iniciá sesión como turista para modificar una reserva.");
        try {
            return ResponseEntity.ok(activityService.rescheduleReservation(tourist.getId(), reservationId, request.activityStart()));
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
