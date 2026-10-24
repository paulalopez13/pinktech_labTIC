package uy.edu.um.proyTic2026;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.TopActivityResponse;
import uy.edu.um.proyTic2026.entities.*;
import uy.edu.um.proyTic2026.repositories.*;
import uy.edu.um.proyTic2026.services.PublicApiService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Prueba el top 3 de actividades mejor puntuadas del mes (API pública).
// @Transactional hace que lo que se crea en cada test se borre al terminar.
@SpringBootTest
@Transactional
class PublicApiTopRatedTests {
    private static final YearMonth MARCH = YearMonth.of(2025, 3);

    @Autowired PublicApiService publicApiService;
    @Autowired ProviderRepository providerRepository;
    @Autowired TouristRepository touristRepository;
    @Autowired ActivityRepository activityRepository;
    @Autowired ActivityReservationRepository reservationRepository;
    @Autowired ActivityReviewRepository reviewRepository;

    private Provider provider;
    private Tourist tourist;
    private int reservationCount;

    @BeforeEach
    void setUp() {
        // Usa un prestador aprobado y un turista de los datos de demostración.
        provider = providerRepository.findAllByStatusOrderByIdAsc(ProviderStatus.APPROVED).getFirst();
        tourist = touristRepository.findAll().getFirst();
    }

    @Test
    void ranksBestAverageFirstAndBreaksTiesByReviewCount() {
        Activity bestWithMoreReviews = activity("Test A", "Maldonado");
        Activity bestWithFewerReviews = activity("Test B", "Maldonado");
        Activity third = activity("Test C", "Maldonado");
        Activity fourth = activity("Test D", "Maldonado");

        review(bestWithMoreReviews, MARCH.atDay(5), 5);
        review(bestWithMoreReviews, MARCH.atDay(12), 5);
        review(bestWithFewerReviews, MARCH.atDay(8), 5);
        review(third, MARCH.atDay(3), 4);
        review(third, MARCH.atDay(20), 4);
        review(fourth, MARCH.atDay(15), 3);

        List<TopActivityResponse> top = publicApiService.topRated("Maldonado", MARCH);

        assertEquals(3, top.size());
        assertEquals("Test A", top.get(0).activity().name());
        assertEquals("Test B", top.get(1).activity().name());
        assertEquals("Test C", top.get(2).activity().name());
        assertEquals(1, top.get(0).position());
        assertEquals(5.0, top.get(0).monthAverageRating());
        assertEquals(2, top.get(0).monthReviewCount());
    }

    @Test
    void countsOnlyActivitiesDoneInThatMonthAndDepartment() {
        Activity maldonado = activity("Test Maldonado", "Maldonado");
        Activity rocha = activity("Test Rocha", "Rocha");

        review(maldonado, MARCH.atDay(10), 4);
        // Se hizo en abril: no tiene que bajar el promedio de marzo.
        review(maldonado, MARCH.plusMonths(1).atDay(2), 1);
        // Es de otro departamento: no tiene que aparecer.
        review(rocha, MARCH.atDay(10), 5);

        List<TopActivityResponse> top = publicApiService.topRated("maldonado", MARCH);

        assertEquals(1, top.size());
        assertEquals("Test Maldonado", top.getFirst().activity().name());
        assertEquals(4.0, top.getFirst().monthAverageRating());
        assertEquals(1, top.getFirst().monthReviewCount());
    }

    @Test
    void returnsEmptyListWhenThereAreNoReviews() {
        assertTrue(publicApiService.topRated("Maldonado", MARCH).isEmpty());
    }

    @Test
    void requiresDepartment() {
        assertThrows(IllegalArgumentException.class, () -> publicApiService.topRated(" ", MARCH));
    }

    private Activity activity(String name, String department) {
        Activity activity = new Activity();
        activity.setProvider(provider);
        activity.setName(name);
        activity.setDepartment(department);
        activity.setCity("Ciudad de prueba");
        activity.setLocation("Lugar de prueba");
        activity.setDescription("Actividad creada para un test.");
        activity.setFirstDate(MARCH.atDay(1));
        activity.setRecurrence(ActivityRecurrence.DAILY);
        activity.setStartTimes(List.of(LocalTime.of(10, 0)));
        activity.setCapacity(10);
        activity.setPrice(BigDecimal.valueOf(1000));
        activity.setDurationMinutes(60);
        return activityRepository.save(activity);
    }

    // Crea una reserva en esa fecha y una opinión con ese puntaje.
    private void review(Activity activity, LocalDate date, int rating) {
        ActivityReservation reservation = new ActivityReservation();
        reservation.setActivity(activity);
        reservation.setTourist(tourist);
        // Cambia los minutos en cada reserva para no repetir actividad + turista + horario.
        reservation.setActivityStart(LocalDateTime.of(date, LocalTime.of(10, reservationCount++)));
        reservationRepository.save(reservation);

        ActivityReview review = new ActivityReview();
        review.setReservation(reservation);
        review.setRating(rating);
        review.setComment("Opinión de prueba.");
        reviewRepository.save(review);
    }
}
