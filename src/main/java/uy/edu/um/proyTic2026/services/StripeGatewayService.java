package uy.edu.um.proyTic2026.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(StripeGatewayService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private RestClient stripe() {
        return RestClient.create("https://api.stripe.com");
    }

    @Value("${app.stripe.secret-key:}")
    private String secretKey;

    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${app.stripe.publishable-key:}")
    private String publishableKey;

    @Value("${app.frontend-url:http://127.0.0.1:5173}")
    private String frontendUrl;

    public record Checkout(String id, String url) { }
    public record PaymentIntent(String id, String clientSecret, String status, String publishableKey) { }

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

    /** Creates/reuses a card PaymentIntent through Stripe's REST API. Card data stays in Stripe.js. */
    public PaymentIntent createPaymentIntent(ActivityReservation reservation) {
        requireConfigured();
        String publicKey = publishableKey == null ? "" : publishableKey.trim();
        if (!(publicKey.startsWith("pk_test_") || publicKey.startsWith("pk_live_")))
            throw new IllegalArgumentException("Configurá STRIPE_PUBLISHABLE_KEY (pk_test_...) para mostrar el formulario de pago.");
        boolean testSecret = configuredSecretKey().startsWith("sk_test_") || configuredSecretKey().startsWith("rk_test_");
        if (testSecret != publicKey.startsWith("pk_test_"))
            throw new IllegalArgumentException("La clave publicable y la secreta de Stripe deben ser ambas de prueba o ambas activas.");
        if (reservation.getId() == null || reservation.getParticipantCount() == null || reservation.getParticipantCount() < 1)
            throw new IllegalArgumentException("La reserva no tiene un identificador o una cantidad de participantes válida.");
        var activity = reservation.getActivity();
        if (activity == null || activity.getPrice() == null || activity.getPrice().signum() < 0)
            throw new IllegalArgumentException("El precio de la actividad no es válido.");
        if (reservation.getContactEmail() == null || reservation.getContactEmail().isBlank())
            throw new IllegalArgumentException("La reserva no tiene un correo válido para el pago.");

        if (reservation.getPaymentOrderId() != null && reservation.getPaymentOrderId().startsWith("pi_")) {
            JsonNode existing = retrievePaymentIntent(reservation.getPaymentOrderId());
            validatePaymentIntent(reservation, existing, reservation.getPaymentOrderId(), false);
            String status = existing.path("status").asText();
            if (!"canceled".equals(status))
                return paymentIntentResponse(existing, publicKey);
        }

        long amount = stripeAmount(reservation);
        if (amount < 50) throw new IllegalArgumentException("Stripe requiere un pago mínimo de $U 0,50. Para actividades gratuitas no se crea un pago.");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("amount", Long.toString(amount));
        form.add("currency", "uyu");
        // Stripe now manages the PaymentIntent's methods through dashboard settings.
        // Exclude redirect-based methods because this flow stays inside the app.
        form.add("automatic_payment_methods[enabled]", "true");
        form.add("automatic_payment_methods[allow_redirects]", "never");
        form.add("receipt_email", reservation.getContactEmail());
        form.add("description", activity.getName() + " · " + reservation.getParticipantCount() + " participante(s)");
        form.add("metadata[reservation_id]", reservation.getId().toString());
        JsonNode result;
        try {
            String response = stripe().post().uri("/v1/payment_intents")
                    .headers(headers -> {
                        headers.setBasicAuth(configuredSecretKey(), "");
                        headers.set("Idempotency-Key", "reservation-payment-" + reservation.getId());
                    })
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(String.class);
            result = parseStripeResponse(response);
        } catch (RestClientResponseException error) {
            throw stripeError(error);
        } catch (RestClientException error) {
            throw new IllegalArgumentException("Stripe no está disponible ahora. La reserva sigue pendiente y podés volver a intentar el pago.");
        }
        validatePaymentIntent(reservation, result, null, false);
        return paymentIntentResponse(result, publicKey);
    }

    public JsonNode retrievePaymentIntent(String paymentIntentId) {
        requireConfigured();
        if (paymentIntentId == null || !paymentIntentId.matches("pi_[A-Za-z0-9]+"))
            throw new IllegalArgumentException("El identificador de pago de Stripe no es válido.");
        try {
            String response = stripe().get().uri("/v1/payment_intents/{id}", paymentIntentId)
                    .headers(headers -> headers.setBasicAuth(configuredSecretKey(), ""))
                    .retrieve().body(String.class);
            return parseStripeResponse(response);
        } catch (RestClientResponseException error) {
            throw stripeError(error);
        } catch (RestClientException error) {
            throw new IllegalArgumentException("No pudimos consultar el pago con Stripe. Volvé a intentarlo.");
        }
    }

    public void validatePaymentIntent(ActivityReservation reservation, JsonNode intent,
                                      String expectedIntentId, boolean requirePaid) {
        String reservationId = reservation.getId().toString();
        if (intent == null || (expectedIntentId != null && !expectedIntentId.equals(intent.path("id").asText()))
                || !reservationId.equals(intent.path("metadata").path("reservation_id").asText()))
            throw new IllegalArgumentException("El pago de Stripe no corresponde a esta reserva.");
        if (!"uyu".equalsIgnoreCase(intent.path("currency").asText())
                || intent.path("amount").asLong(Long.MIN_VALUE) != stripeAmount(reservation))
            throw new IllegalArgumentException("El importe o la moneda del pago no coinciden con la reserva.");
        String status = intent.path("status").asText();
        if (!java.util.Set.of("requires_payment_method", "requires_confirmation", "requires_action", "processing", "succeeded", "canceled").contains(status))
            throw new IllegalArgumentException("Stripe devolvió un estado de pago desconocido.");
        if (requirePaid && (!"succeeded".equals(status)
                || intent.path("amount_received").asLong(Long.MIN_VALUE) != stripeAmount(reservation)))
            throw new IllegalArgumentException("Stripe todavía no confirmó el pago completo de esta reserva.");
    }

    public void cancelPaymentIntent(String paymentIntentId) {
        if (!isConfigured() || paymentIntentId == null || !paymentIntentId.startsWith("pi_")) return;
        try {
            stripe().post().uri("/v1/payment_intents/{id}/cancel", paymentIntentId)
                    .headers(headers -> headers.setBasicAuth(configuredSecretKey(), ""))
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException error) {
            throw stripeError(error);
        } catch (RestClientException error) {
            throw new IllegalArgumentException("No se pudo confirmar con Stripe la cancelación del intento de pago.");
        }
    }

    private PaymentIntent paymentIntentResponse(JsonNode intent, String publicKey) {
        String clientSecret = intent.path("client_secret").asText();
        String id = intent.path("id").asText();
        if (id.isBlank() || clientSecret.isBlank())
            throw new IllegalArgumentException("Stripe no devolvió los datos para abrir el formulario de pago.");
        return new PaymentIntent(id, clientSecret, intent.path("status").asText(), publicKey);
    }

    private long stripeAmount(ActivityReservation reservation) {
        try {
            return reservation.getActivity().getPrice().multiply(BigDecimal.valueOf(reservation.getParticipantCount()))
                    .multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException | NullPointerException error) {
            throw new IllegalArgumentException("El importe de la reserva no es válido.");
        }
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

    public void refundPaymentIntent(String paymentIntentId, Long reservationId, BigDecimal amount) {
        if (!isConfigured()) throw new IllegalArgumentException("No se pudo verificar la configuración de Stripe para devolver el pago.");
        if (paymentIntentId == null || !paymentIntentId.matches("pi_[A-Za-z0-9]+") || reservationId == null)
            throw new IllegalArgumentException("Stripe no devolvió un identificador de pago válido para tramitar la devolución.");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("payment_intent", paymentIntentId);
        long minorAmount;
        try {
            minorAmount = amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("El importe del reembolso no es válido.");
        }
        if (minorAmount <= 0) return;
        form.add("amount", Long.toString(minorAmount));
        try {
            String response = stripe().post().uri("/v1/refunds").headers(headers -> {
                        headers.setBasicAuth(configuredSecretKey(), "");
                        headers.set("Idempotency-Key", "refund-reservation-" + reservationId);
                    })
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(String.class);
            JsonNode refund = parseStripeResponse(response);
            String status = refund.path("status").asText();
            if (refund.path("id").asText().isBlank()
                    || !paymentIntentId.equals(refund.path("payment_intent").asText())
                    || refund.path("amount").asLong(Long.MIN_VALUE) != minorAmount
                    || !("succeeded".equals(status) || "pending".equals(status)))
                throw new IllegalArgumentException("Stripe no confirmó el reembolso solicitado.");
        } catch (RestClientResponseException error) {
            throw stripeError(error);
        } catch (RestClientException error) {
            throw new IllegalArgumentException("No se pudo contactar a Stripe para confirmar el reembolso. La reserva sigue sin cancelarse; intentá de nuevo.");
        }
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
        String type = "unknown";
        String code = "unknown";
        String message = "";
        try {
            JsonNode details = JSON.readTree(error.getResponseBodyAsString()).path("error");
            type = details.path("type").asText(type);
            code = details.path("code").asText(code);
            message = details.path("message").asText("");
        } catch (Exception ignored) { }
        String requestId = error.getResponseHeaders() == null ? "unknown"
                : error.getResponseHeaders().getFirst("Request-Id");
        logger.warn("Stripe API rejected request: httpStatus={}, type={}, code={}, requestId={}, message={}",
                error.getStatusCode().value(), type, code, requestId, message);
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
