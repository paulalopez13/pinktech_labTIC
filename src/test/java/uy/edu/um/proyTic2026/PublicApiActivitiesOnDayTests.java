package uy.edu.um.proyTic2026;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.ActivityResponse;
import uy.edu.um.proyTic2026.entities.*;
import uy.edu.um.proyTic2026.repositories.ActivityRepository;
import uy.edu.um.proyTic2026.repositories.ProviderRepository;
import uy.edu.um.proyTic2026.services.PublicApiService;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// Prueba "actividades de un día por departamento" (API pública).
// Usa el departamento Flores, que no tiene actividades en los datos de demostración.
@SpringBootTest
@Transactional
class PublicApiActivitiesOnDayTests {
    private static final LocalDate MONDAY = LocalDate.of(2026, 3, 9);

    @Autowired PublicApiService publicApiService;
    @Autowired ProviderRepository providerRepository;
    @Autowired ActivityRepository activityRepository;

    @BeforeEach
    void setUp() {
        // Todos los lunes de marzo de 2026, en Trinidad.
        Activity weekly = activity("Lunes en Trinidad", "Trinidad", ActivityRecurrence.WEEKLY);
        weekly.setWeekdays(Set.of(DayOfWeek.MONDAY));
        weekly.setRepeatUntil(LocalDate.of(2026, 3, 31));
        activityRepository.save(weekly);

        // Todos los días de marzo de 2026, en otra localidad del mismo departamento.
        Activity daily = activity("Todos los días en Ismael Cortinas", "Ismael Cortinas", ActivityRecurrence.DAILY);
        daily.setRepeatUntil(LocalDate.of(2026, 3, 31));
        activityRepository.save(daily);
    }

    @Test
    void listsEveryActivityOfTheDepartmentThatDay() {
        assertEquals(List.of("Lunes en Trinidad", "Todos los días en Ismael Cortinas"),
                names(publicApiService.activitiesOn(MONDAY, "Flores", null)));
    }

    @Test
    void leavesOutActivitiesThatDoNotHappenThatDay() {
        LocalDate tuesday = MONDAY.plusDays(1);
        assertEquals(List.of("Todos los días en Ismael Cortinas"),
                names(publicApiService.activitiesOn(tuesday, "Flores", null)));
    }

    @Test
    void leavesOutActivitiesAfterTheyEnd() {
        LocalDate mondayInApril = LocalDate.of(2026, 4, 6);
        assertTrue(publicApiService.activitiesOn(mondayInApril, "Flores", null).isEmpty());
    }

    @Test
    void filtersByCityWhenGiven() {
        assertEquals(List.of("Lunes en Trinidad"),
                names(publicApiService.activitiesOn(MONDAY, "Flores", "Trinidad")));
    }

    @Test
    void ignoresUppercaseAndAccents() {
        assertEquals(List.of("Lunes en Trinidad"),
                names(publicApiService.activitiesOn(MONDAY, "FLORES", "trinidad")));
    }

    @Test
    void returnsEmptyListForAnotherDepartment() {
        assertTrue(publicApiService.activitiesOn(MONDAY, "Departamento inexistente", null).isEmpty());
    }

    @Test
    void requiresDepartment() {
        assertThrows(IllegalArgumentException.class, () -> publicApiService.activitiesOn(MONDAY, " ", null));
    }

    private Activity activity(String name, String city, ActivityRecurrence recurrence) {
        Activity activity = new Activity();
        activity.setProvider(providerRepository.findAllByStatusOrderByIdAsc(ProviderStatus.APPROVED).getFirst());
        activity.setName(name);
        activity.setDepartment("Flores");
        activity.setCity(city);
        activity.setLocation("Lugar de prueba");
        activity.setDescription("Actividad creada para un test.");
        activity.setFirstDate(LocalDate.of(2026, 3, 2));
        activity.setRecurrence(recurrence);
        activity.setStartTimes(List.of(LocalTime.of(10, 0)));
        activity.setCapacity(10);
        activity.setPrice(BigDecimal.valueOf(1000));
        activity.setDurationMinutes(60);
        return activity;
    }

    private List<String> names(List<ActivityResponse> activities) {
        return activities.stream().map(ActivityResponse::name).toList();
    }
}
