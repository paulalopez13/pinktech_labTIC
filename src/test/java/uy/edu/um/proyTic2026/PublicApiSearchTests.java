package uy.edu.um.proyTic2026;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.ActivityResponse;
import uy.edu.um.proyTic2026.entities.*;
import uy.edu.um.proyTic2026.repositories.ActivityCategoryRepository;
import uy.edu.um.proyTic2026.repositories.ActivityRepository;
import uy.edu.um.proyTic2026.repositories.ProviderRepository;
import uy.edu.um.proyTic2026.services.PublicApiService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// Prueba "buscar actividades por palabra clave" (API pública).
// @Transactional hace que lo que se crea en cada test se borre al terminar.
@SpringBootTest
@Transactional
class PublicApiSearchTests {

    @Autowired PublicApiService publicApiService;
    @Autowired ProviderRepository providerRepository;
    @Autowired ActivityRepository activityRepository;
    @Autowired ActivityCategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        // Una actividad con palabras que no aparecen en los datos de demostración.
        ActivityCategory category = new ActivityCategory();
        category.setName("Observación de aves");
        categoryRepository.save(category);

        Activity activity = new Activity();
        activity.setProvider(providerRepository.findAllByStatusOrderByIdAsc(ProviderStatus.APPROVED).getFirst());
        activity.setName("Paseo del Zorzal");
        activity.setDepartment("Tacuarembó");
        activity.setCity("Valle Edén");
        activity.setLocation("Lugar de prueba");
        activity.setDescription("Caminata por el monte nativo escuchando pájaros.");
        activity.setCategories(Set.of(category));
        activity.setFirstDate(LocalDate.of(2026, 3, 2));
        activity.setStartTimes(List.of(LocalTime.of(10, 0)));
        activity.setCapacity(10);
        activity.setPrice(BigDecimal.valueOf(1000));
        activity.setDurationMinutes(60);
        activityRepository.save(activity);
    }

    @Test
    void findsByNameIgnoringUppercase() {
        assertEquals(List.of("Paseo del Zorzal"), names(publicApiService.search("ZORZAL")));
    }

    @Test
    void findsByDescription() {
        assertTrue(names(publicApiService.search("pájaros")).contains("Paseo del Zorzal"));
    }

    @Test
    void findsWithOrWithoutAccents() {
        // En la base están con tilde ("Tacuarembó", "Valle Edén", "pájaros") y se buscan sin tilde.
        assertTrue(names(publicApiService.search("tacuarembo")).contains("Paseo del Zorzal"));
        assertTrue(names(publicApiService.search("valle eden")).contains("Paseo del Zorzal"));
        assertTrue(names(publicApiService.search("pajaros")).contains("Paseo del Zorzal"));
        // Y al revés: se busca con tilde una palabra que en la base no la tiene ("monte").
        assertTrue(names(publicApiService.search("mónte")).contains("Paseo del Zorzal"));
    }

    @Test
    void findsByCategory() {
        assertTrue(names(publicApiService.search("observacion de aves")).contains("Paseo del Zorzal"));
    }

    @Test
    void returnsEmptyListWhenNothingMatches() {
        assertTrue(publicApiService.search("xyzwq").isEmpty());
    }

    @Test
    void requiresAtLeastTwoLetters() {
        assertThrows(IllegalArgumentException.class, () -> publicApiService.search("a"));
        assertThrows(IllegalArgumentException.class, () -> publicApiService.search("  "));
    }

    private List<String> names(List<ActivityResponse> activities) {
        return activities.stream().map(ActivityResponse::name).toList();
    }
}
