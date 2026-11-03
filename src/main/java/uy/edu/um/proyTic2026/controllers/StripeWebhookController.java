package uy.edu.um.proyTic2026.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.services.ActivityService;
import uy.edu.um.proyTic2026.services.StripeGatewayService;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {
    private final StripeGatewayService stripeGateway;
    private final ActivityService activityService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody String payload,
                                     @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (!stripeGateway.verifyWebhook(payload, signature))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Firma de Stripe inválida."));
        try {
            var event = objectMapper.readTree(payload);
            activityService.handleStripeWebhook(event.path("type").asText(), event.path("data").path("object"));
            return ResponseEntity.ok(Map.of("received", true));
        } catch (Exception error) {
            return ResponseEntity.badRequest().body(Map.of("message", "No se pudo procesar el evento de Stripe."));
        }
    }
}
