package uy.edu.um.proyTic2026.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import uy.edu.um.proyTic2026.entities.ActivityReservation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.net.URI;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class StripeGatewayService {
    private static final ObjectMapper JSON = new ObjectMapper();

    private RestClient stripe() {
        return RestClient.create("https://api.stripe.com");
    }

    @Value("${app.stripe.secret-key:}")
    private String secretKey;

    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${app.frontend-url:http://127.0.0.1:5173}")
    private String frontendUrl;

    public record Checkout(String id, String url) { }

    public boolean isConfigured() {
        String key = configuredSecretKey();
        return key.startsWith("sk_test_") || key.startsWith("rk_test_");
    }

    public Checkout createCheckout(ActivityReservation reservation) {
        requireConfigured();
        var activity = reservation.getActivity();
        if (reservation.getId() == null || reservation.getParticipantCount() == null || reservation.getParticipantCount() < 1)
            throw new IllegalArgumentException("La reserva no tiene un identificador o una cantidad de participantes válida.");
        if (activity == null || activity.getPrice() == null || activity.getPrice().signum() < 0)
            throw new IllegalArgumentException("El precio de la actividad no es válido.");
        if (reservation.getContactEmail() == null || reservation.getContactEmail().isBlank())
            throw new IllegalArgumentException("La reserva no tiene un correo válido para el pago.");
        BigDecimal total = activity.getPrice().multiply(BigDecimal.valueOf(reservation.getParticipantCount()));
        long minorUnits;
        try {
            minorUnits = total.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("El total debe tener como máximo dos decimales.");
        }
        if (minorUnits < 50) throw new IllegalArgumentException("Stripe requiere un pago mínimo de $U 0,50. Para actividades gratuitas no se crea un pago.");
        if (minorUnits % reservation.getParticipantCount() != 0)
            throw new IllegalArgumentException("No se pudo calcular el importe por participante.");
        String reservationId = reservation.getId().toString();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("locale", "es-419");
        form.add("client_reference_id", reservationId);
        form.add("customer_email", reservation.getContactEmail());
        form.add("success_url", frontendUrl + "/?payment=success&reservationId=" + reservationId + "&session_id={CHECKOUT_SESSION_ID}");
        form.add("cancel_url", frontendUrl + "/?payment=cancelled&reservationId=" + reservationId);
        form.add("line_items[0][price_data][currency]", "uyu");
        form.add("line_items[0][price_data][unit_amount]", Long.toString(minorUnits / reservation.getParticipantCount()));
        form.add("line_items[0][price_data][product_data][name]", activity.getName());
        form.add("line_items[0][price_data][product_data][description]", reservation.getParticipantCount() + " participante(s) · " + activity.getCity());
        form.add("line_items[0][quantity]", Integer.toString(reservation.getParticipantCount()));
        form.add("metadata[reservation_id]", reservationId);
        form.add("payment_intent_data[metadata][reservation_id]", reservationId);
        JsonNode result;
        try {
            String response = stripe().post().uri("/v1/checkout/sessions")
                    .headers(headers -> headers.setBasicAuth(configuredSecretKey(), ""))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().body(String.class);
            result = parseStripeResponse(response);
        } catch (RestClientResponseException error) {
            throw stripeError(error);
        } catch (RestClientException error) {
            throw new IllegalArgumentException("Stripe no está disponible ahora. La reserva sigue pendiente y podés volver a intentar el pago.");
        }
        if (result == null || result.path("id").asText().isBlank() || result.path("url").asText().isBlank())
            throw new IllegalArgumentException("Stripe no devolvió el enlace de pago.");
        String sessionId = result.path("id").asText();
        validateCheckoutSession(reservation, result, null, false);
        if (!"open".equals(result.path("status").asText()))
            throw new IllegalArgumentException("Stripe no creó una sesión de pago abierta.");
        String checkoutUrl = result.path("url").asText();
        validateCheckoutUrl(checkoutUrl);
        return new Checkout(sessionId, checkoutUrl);
    }

    public void validateCheckoutUrl(String checkoutUrl) {
        try {
            URI uri = URI.create(checkoutUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !"checkout.stripe.com".equalsIgnoreCase(uri.getHost()))
                throw new IllegalArgumentException("Stripe devolvió un enlace de pago no válido.");
        } catch (IllegalArgumentException error) {
            if ("Stripe devolvió un enlace de pago no válido.".equals(error.getMessage())) throw error;
            throw new IllegalArgumentException("Stripe devolvió un enlace de pago no válido.");
        }
    }

    public JsonNode retrieveCheckout(String sessionId) {
        requireConfigured();
        if (sessionId == null || !sessionId.matches("cs_(test|live)_[A-Za-z0-9]+"))
            throw new IllegalArgumentException("El identificador de sesión de Stripe no es válido.");
        try {
            String response = stripe().get().uri("/v1/checkout/sessions/{id}", sessionId)
                    .headers(headers -> headers.setBasicAuth(configuredSecretKey(), ""))
                    .retrieve().body(String.class);
            return parseStripeResponse(response);
        } catch (RestClientResponseException error) {
            throw stripeError(error);
        } catch (RestClientException error) {
            throw new IllegalArgumentException("No pudimos consultar el estado del pago con Stripe. No confirmamos la reserva; volvé a intentarlo.");
        }
    }

    public void validateCheckoutSession(ActivityReservation reservation, JsonNode session,
                                         String expectedSessionId, boolean requirePaid) {
        String reservationId = reservation.getId().toString();
        if (session == null || (expectedSessionId != null && !expectedSessionId.equals(session.path("id").asText()))
                || !reservationId.equals(session.path("client_reference_id").asText())
                || !reservationId.equals(session.path("metadata").path("reservation_id").asText()))
            throw new IllegalArgumentException("La sesión de Stripe no corresponde a esta reserva.");
        if (!"payment".equals(session.path("mode").asText()) || !"uyu".equalsIgnoreCase(session.path("currency").asText()))
            throw new IllegalArgumentException("La sesión de Stripe tiene una moneda o modalidad inesperada.");
        String status = session.path("status").asText();
        if (!"open".equals(status) && !"complete".equals(status) && !"expired".equals(status))
            throw new IllegalArgumentException("Stripe devolvió un estado de sesión desconocido.");
        long expectedAmount;
        try {
            expectedAmount = reservation.getActivity().getPrice()
                    .multiply(BigDecimal.valueOf(reservation.getParticipantCount()))
                    .multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException | NullPointerException error) {
            throw new IllegalArgumentException("El importe de la reserva no es válido.");
        }
        if (session.path("amount_total").asLong(Long.MIN_VALUE) != expectedAmount)
            throw new IllegalArgumentException("El importe informado por Stripe no coincide con el total de la reserva.");
        if (requirePaid && (!"complete".equals(status) || !"paid".equals(session.path("payment_status").asText())
                || !session.path("payment_intent").asText().matches("pi_[A-Za-z0-9]+")))
            throw new IllegalArgumentException("Stripe todavía no confirmó un pago válido para esta reserva.");
    }

    public void expireCheckout(String sessionId) {
        if (!isConfigured() || sessionId == null || !sessionId.startsWith("cs_")) return;
        try {
            stripe().post().uri("/v1/checkout/sessions/{id}/expire", sessionId)
                    .headers(headers -> headers.setBasicAuth(configuredSecretKey(), ""))
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException ignored) {
            // La sesión puede haber terminado justo al expirar el plazo; el resultado se verifica al volver.
        }
    }

    public void refundPaymentIntent(String paymentIntentId, Long reservationId) {
        if (!isConfigured()) throw new IllegalArgumentException("No se pudo verificar la configuración de Stripe para devolver el pago.");
        if (paymentIntentId == null || !paymentIntentId.matches("pi_[A-Za-z0-9]+") || reservationId == null)
            throw new IllegalArgumentException("Stripe no devolvió un identificador de pago válido para tramitar la devolución.");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("payment_intent", paymentIntentId);
        stripe().post().uri("/v1/refunds").headers(headers -> {
                    headers.setBasicAuth(configuredSecretKey(), "");
                    headers.set("Idempotency-Key", "refund-reservation-" + reservationId);
                })
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().toBodilessEntity();
    }

    public boolean verifyWebhook(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank() || signatureHeader == null) return false;
        String timestamp = null;
        java.util.List<String> signatures = new java.util.ArrayList<>();
        for (String component : signatureHeader.split(",")) {
            String[] parts = component.split("=", 2);
            if (parts.length != 2) continue;
            if (parts[0].equals("t")) timestamp = parts[1];
            if (parts[0].equals("v1")) signatures.add(parts[1]);
        }
        if (timestamp == null || signatures.isEmpty()) return false;
        try {
            long time = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - time) > 300) return false;
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
            byte[] expectedHex = HexFormat.of().formatHex(expected).getBytes(StandardCharsets.UTF_8);
            return signatures.stream().anyMatch(signature -> MessageDigest.isEqual(expectedHex, signature.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            return false;
        }
    }

    private void requireConfigured() {
        if (!isConfigured()) throw new IllegalArgumentException("Configurá una clave de prueba STRIPE_SECRET_KEY (sk_test_... o rk_test_...) para usar los pagos sandbox.");
    }

    private String configuredSecretKey() {
        if (secretKey != null && !secretKey.isBlank()) return secretKey.trim();
        String environmentValue = System.getenv("STRIPE_SECRET_KEY");
        if (environmentValue != null && !environmentValue.isBlank()) return environmentValue.trim();
        try {
            return Files.readAllLines(Path.of(".env")).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("STRIPE_SECRET_KEY="))
                    .map(line -> line.substring("STRIPE_SECRET_KEY=".length()).trim())
                    .map(value -> value.replaceAll("^['\\\"]|['\\\"]$", ""))
                    .findFirst().orElse("");
        } catch (Exception ignored) {
            return "";
        }
    }

    private IllegalArgumentException stripeError(RestClientResponseException error) {
        return new IllegalArgumentException("Stripe no pudo procesar la solicitud. Revisá que la clave de prueba sea válida y que el monto esté admitido.");
    }

    private JsonNode parseStripeResponse(String response) {
        try {
            return JSON.readTree(response);
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("Stripe devolvió una respuesta que el servidor no pudo leer.");
        }
    }
}
