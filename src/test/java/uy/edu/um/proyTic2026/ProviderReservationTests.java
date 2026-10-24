package uy.edu.um.proyTic2026;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.ProviderReservationResponse;
import uy.edu.um.proyTic2026.entities.*;
import uy.edu.um.proyTic2026.repositories.*;
import uy.edu.um.proyTic2026.services.ProviderReservationService;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Prueba que el prestador vea las reservas de sus actividades, y solo las suyas.
// @Transactional hace que lo que se crea en cada test se borre al terminar.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class ProviderReservationTests {

    @Autowired ProviderReservationService providerReservationService;
    @Autowired ProviderRepository providerRepository;
    @Autowired TouristRepository touristRepository;
    @Autowired ActivityRepository activityRepository;
    @Autowired ActivityReservationRepository reservationRepository;
    @Autowired Environment environment;

    private Provider provider;
    private Provider otherProvider;
    private Tourist tourist;

    @BeforeEach
    void setUp() {
        List<Provider> approved = providerRepository.findAllByStatusOrderByIdAsc(ProviderStatus.APPROVED);
        provider = approved.get(0);
        otherProvider = approved.get(1);
        tourist = touristRepository.findAll().getFirst();
    }

    @Test
    void listsOnlyReservationsOfTheProvidersActivities() {
        Activity mine = activity("Mi actividad de prueba", provider);
        Activity notMine = activity("Actividad de otro prestador", otherProvider);
        reservation(mine, LocalDateTime.of(2026, 12, 20, 10, 0), "Ana Pérez");
        reservation(mine, LocalDateTime.of(2026, 12, 5, 10, 0), "Bruno Silva");
        reservation(notMine, LocalDateTime.of(2026, 12, 10, 10, 0), "Carla Díaz");

        List<ProviderReservationResponse> result = providerReservationService.reservations(provider.getId()).stream()
                .filter(reservation -> reservation.activityName().equals("Mi actividad de prueba")
                        || reservation.activityName().equals("Actividad de otro prestador"))
                .toList();

        // Solo las 2 suyas, la más próxima primero.
        assertEquals(List.of("Bruno Silva", "Ana Pérez"),
                result.stream().map(ProviderReservationResponse::contactName).toList());
    }

    @Test
    void includesParticipantsAndTotal() {
        Activity mine = activity("Mi actividad de prueba", provider);
        reservation(mine, LocalDateTime.of(2026, 12, 5, 10, 0), "Ana Pérez");

        ProviderReservationResponse result = providerReservationService.reservations(provider.getId()).stream()
                .filter(reservation -> reservation.activityName().equals("Mi actividad de prueba"))
                .findFirst().orElseThrow();

        assertEquals(2, result.participantCount());
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(result.total()));
        assertEquals("ana@example.com", result.contactEmail());
        assertEquals(List.of("Ana Pérez", "Luis Pérez"),
                result.participants().stream().map(ProviderReservationResponse.Participant::fullName).toList());
        assertEquals("APPROVED", result.paymentStatus());
    }

    @Test
    void requiresProviderSession() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(
                        "http://localhost:" + environment.getProperty("local.server.port") + "/api/reservations/provider"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
    }

    private Activity activity(String name, Provider owner) {
        Activity activity = new Activity();
        activity.setProvider(owner);
        activity.setName(name);
        activity.setDepartment("Montevideo");
        activity.setCity("Montevideo");
        activity.setLocation("Lugar de prueba");
        activity.setDescription("Actividad creada para un test.");
        activity.setFirstDate(LocalDate.of(2026, 12, 1));
        activity.setRecurrence(ActivityRecurrence.DAILY);
        activity.setStartTimes(List.of(LocalTime.of(10, 0)));
        activity.setCapacity(10);
        activity.setPrice(BigDecimal.valueOf(1000));
        activity.setDurationMinutes(60);
        return activityRepository.save(activity);
    }

    // Una reserva para 2 personas: quien reserva y un acompañante.
    private void reservation(Activity activity, LocalDateTime start, String contactName) {
        ActivityReservation reservation = new ActivityReservation();
        reservation.setActivity(activity);
        reservation.setTourist(tourist);
        reservation.setActivityStart(start);
        reservation.setParticipantCount(2);
        reservation.setContactName(contactName);
        reservation.setContactEmail("ana@example.com");
        reservation.setContactPhone("+59899123456");
        reservation.getParticipants().add(participant(reservation, contactName));
        reservation.getParticipants().add(participant(reservation, "Luis Pérez"));
        reservationRepository.save(reservation);
    }

    private ReservationParticipant participant(ActivityReservation reservation, String fullName) {
        ReservationParticipant participant = new ReservationParticipant();
        participant.setReservation(reservation);
        participant.setFullName(fullName);
        participant.setDocumentNumber("12345678");
        participant.setAge(30);
        return participant;
    }
}
