package uy.edu.um.proyTic2026;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.GroupReservationRequest;
import uy.edu.um.proyTic2026.dtos.ReservationParticipantRequest;
import uy.edu.um.proyTic2026.entities.*;
import uy.edu.um.proyTic2026.repositories.ActivityRepository;
import uy.edu.um.proyTic2026.repositories.ProviderRepository;
import uy.edu.um.proyTic2026.repositories.TouristRepository;
import uy.edu.um.proyTic2026.services.ActivityService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Prueba el plazo para reservar, que se cuenta en minutos antes del inicio de la actividad.
// @Transactional hace que lo que se crea en cada test se borre al terminar.
@SpringBootTest
@Transactional
class BookingCutoffTests {

    @Autowired ActivityService activityService;
    @Autowired ActivityRepository activityRepository;
    @Autowired ProviderRepository providerRepository;
    @Autowired TouristRepository touristRepository;

    // La salida empieza dentro de 20 minutos.
    private final LocalDateTime start = LocalDateTime.now().plusMinutes(20).truncatedTo(ChronoUnit.MINUTES);

    @Test
    void allowsBookingBeforeTheCutoff() {
        Activity activity = activityStartingSoon(10);

        assertDoesNotThrow(() -> reserve(activity));
    }

    @Test
    void rejectsBookingAfterTheCutoff() {
        Activity activity = activityStartingSoon(30);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> reserve(activity));
        assertEquals("El plazo para reservar esta fecha ya cerró.", error.getMessage());
    }

    @Test
    void zeroMinutesAllowsBookingUntilTheStart() {
        Activity activity = activityStartingSoon(0);

        assertDoesNotThrow(() -> reserve(activity));
    }

    private Activity activityStartingSoon(int cutoffMinutes) {
        Activity activity = new Activity();
        activity.setProvider(providerRepository.findAllByStatusOrderByIdAsc(ProviderStatus.APPROVED).getFirst());
        activity.setName("Actividad de prueba del plazo");
        activity.setDepartment("Montevideo");
        activity.setCity("Montevideo");
        activity.setLocation("Lugar de prueba");
        activity.setDescription("Actividad creada para un test.");
        activity.setFirstDate(start.toLocalDate().minusDays(1));
        activity.setRecurrence(ActivityRecurrence.DAILY);
        activity.setStartTimes(List.of(start.toLocalTime()));
        activity.setCapacity(10);
        activity.setPrice(BigDecimal.valueOf(1000));
        activity.setDurationMinutes(60);
        activity.setBookingCutoffMinutes(cutoffMinutes);
        return activityRepository.save(activity);
    }

    private void reserve(Activity activity) {
        Tourist tourist = touristRepository.findAll().getFirst();
        GroupReservationRequest request = new GroupReservationRequest(start, "Ana Pérez", "ana@example.com",
                "+59899123456", List.of(new ReservationParticipantRequest("Ana Pérez", DocumentType.CI,
                        "12345678", 30, null, null)));
        activityService.reserveSandbox(tourist.getId(), activity.getId(), request);
    }
}
