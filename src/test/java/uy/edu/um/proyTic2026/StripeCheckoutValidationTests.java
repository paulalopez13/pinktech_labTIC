package uy.edu.um.proyTic2026;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.edu.um.proyTic2026.entities.Activity;
import uy.edu.um.proyTic2026.entities.ActivityReservation;
import uy.edu.um.proyTic2026.services.StripeGatewayService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StripeCheckoutValidationTests {
    private final StripeGatewayService stripeGateway = new StripeGatewayService();
    private final ObjectMapper mapper = new ObjectMapper();
    private ActivityReservation reservation;

    @BeforeEach
    void setUp() {
        Activity activity = new Activity();
        activity.setPrice(new BigDecimal("175.50"));
        reservation = new ActivityReservation();
        reservation.setId(31L);
        reservation.setParticipantCount(2);
        reservation.setActivity(activity);
    }

    @Test
    void acceptsPaidSessionWithMatchingReservationCurrencyAndTotal() {
        assertDoesNotThrow(() -> stripeGateway.validateCheckoutSession(reservation, session(35100, "uyu", "paid"), "cs_test_abc123", true));
    }

    @Test
    void rejectsSessionWithWrongAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> stripeGateway.validateCheckoutSession(reservation, session(35101, "uyu", "paid"), "cs_test_abc123", true));
    }

    @Test
    void rejectsSessionWithWrongCurrencyOrReservationReference() {
        assertThrows(IllegalArgumentException.class,
                () -> stripeGateway.validateCheckoutSession(reservation, session(35100, "usd", "paid"), "cs_test_abc123", true));
        ObjectNode wrongReservation = session(35100, "uyu", "paid");
        wrongReservation.put("client_reference_id", "other-reservation");
        assertThrows(IllegalArgumentException.class,
                () -> stripeGateway.validateCheckoutSession(reservation, wrongReservation, "cs_test_abc123", true));
    }

    @Test
    void doesNotAcceptAnUnpaidOrIncompleteSession() {
        assertThrows(IllegalArgumentException.class,
                () -> stripeGateway.validateCheckoutSession(reservation, session(35100, "uyu", "unpaid"), "cs_test_abc123", true));
        ObjectNode open = session(35100, "uyu", "paid");
        open.put("status", "open");
        assertThrows(IllegalArgumentException.class,
                () -> stripeGateway.validateCheckoutSession(reservation, open, "cs_test_abc123", true));
    }

    private ObjectNode session(long amount, String currency, String paymentStatus) {
        ObjectNode session = mapper.createObjectNode();
        session.put("id", "cs_test_abc123");
        session.put("mode", "payment");
        session.put("status", "complete");
        session.put("payment_status", paymentStatus);
        session.put("currency", currency);
        session.put("amount_total", amount);
        session.put("client_reference_id", "31");
        session.put("payment_intent", "pi_testabc123");
        session.putObject("metadata").put("reservation_id", "31");
        return session;
    }
}
