package uy.edu.um.proyTic2026.config;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.entities.*;
import uy.edu.um.proyTic2026.repositories.ActivityCategoryRepository;
import uy.edu.um.proyTic2026.repositories.ActivityRepository;
import uy.edu.um.proyTic2026.repositories.IndividualProviderRepository;
import uy.edu.um.proyTic2026.repositories.UserRepository;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Seeds a small, repeatable dataset for local demonstrations. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements ApplicationRunner {
    private static final String DEMO_PASSWORD = "UruguayDemo2026!";

    private final UserRepository userRepository;
    private final IndividualProviderRepository providerRepository;
    private final ActivityCategoryRepository categoryRepository;
    private final ActivityRepository activityRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        ensureTourist();

        List<IndividualProvider> providers = List.of(
                ensureProvider("prestador.costa", "costa.demo@uruguaymnatural.uy", "Lucía", "Silva", "49000001",
                        "Maldonado", "Piriápolis", "Guía Costa Sur", "099100001"),
                ensureProvider("prestador.naturaleza", "naturaleza.demo@uruguaymnatural.uy", "Mateo", "Pérez", "49000002",
                        "Lavalleja", "Minas", "Senderos del Este", "099100002"),
                ensureProvider("prestador.cultura", "cultura.demo@uruguaymnatural.uy", "Sofía", "Ramos", "49000003",
                        "Colonia", "Colonia del Sacramento", "Historias del Río", "099100003")
        );

        ActivityCategory nature = ensureCategory("Naturaleza");
        ActivityCategory adventure = ensureCategory("Aventura");
        ActivityCategory culture = ensureCategory("Historia y cultura");
        ActivityCategory gastronomy = ensureCategory("Gastronomía");

        LocalDate start = LocalDate.now().plusDays(2);
        createActivityIfMissing(providers.get(0), "Ascenso guiado al Cerro Pan de Azúcar", "Maldonado", "Piriápolis",
                "Ruta guiada hasta la cima del cerro, con pausas para conocer la flora local y disfrutar las vistas de la costa.",
                start, new BigDecimal("900"), 10, 210, "Calzado de trekking, agua y protección solar.", Set.of(nature, adventure), "cerro-pan-azucar.png");
        createActivityIfMissing(providers.get(0), "Paseo en kayak por la bahía", "Maldonado", "Piriápolis",
                "Recorrido tranquilo en kayak por la bahía, con equipo de seguridad y guía durante toda la salida.",
                start, new BigDecimal("1200"), 8, 120, "Ropa cómoda que se pueda mojar.", Set.of(nature, adventure), "isla-gorriti.png");
        createActivityIfMissing(providers.get(1), "Caminata por Pozos Azules", "Lavalleja", "Villa Serrana",
                "Caminata por senderos serranos hasta los pozos de agua, con tiempo para descansar y disfrutar el entorno.",
                start.plusDays(1), new BigDecimal("700"), 12, 180, "Calzado cómodo y botella de agua.", Set.of(nature), "pozos-azules.png");
        createActivityIfMissing(providers.get(1), "Atardecer en el Cerro Arequita", "Lavalleja", "Minas",
                "Ascenso acompañado al mirador del Arequita para conocer su paisaje y ver caer el sol sobre las sierras.",
                start.plusDays(1), BigDecimal.ZERO, 15, 150, "Abrigo liviano y agua.", Set.of(nature, adventure), "campo-durazno.png");
        createActivityIfMissing(providers.get(2), "Recorrido por la historia de Colonia", "Colonia", "Colonia del Sacramento",
                "Paseo a pie por el Barrio Histórico y sus rincones, con relatos sobre la ciudad y su patrimonio.",
                start.plusDays(2), new BigDecimal("550"), 14, 120, "No se requiere equipamiento especial.", Set.of(culture), "calle-suspiros.png");
        createActivityIfMissing(providers.get(2), "Sabores del Barrio Histórico", "Colonia", "Colonia del Sacramento",
                "Degustación de productos locales y paseo por pequeños comercios gastronómicos de la ciudad.",
                start.plusDays(2), new BigDecimal("1500"), 10, 150, "Avisar con anticipación sobre restricciones alimentarias.", Set.of(culture, gastronomy), "bodega-jose-ignacio.png");
    }

    private void ensureTourist() {
        if (userRepository.existsByUsername("turista.demo")) return;
        Tourist tourist = new Tourist();
        tourist.setUsername("turista.demo");
        tourist.setEmail("turista.demo@uruguaymnatural.uy");
        tourist.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        tourist.setFirstName("Valentina");
        tourist.setLastName("Demo");
        tourist.setCi("48000001");
        tourist.setDocumentType(DocumentType.CI);
        tourist.setAge(28);
        userRepository.save(tourist);
    }

    private IndividualProvider ensureProvider(String username, String email, String firstName, String lastName,
                                              String ci, String department, String city, String address, String phone) {
        User existing = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, email).orElse(null);
        if (existing instanceof IndividualProvider provider) {
            provider.setStatus(ProviderStatus.APPROVED);
            return providerRepository.save(provider);
        }
        if (existing != null) throw new IllegalStateException("El usuario demo " + username + " ya existe con otro tipo de cuenta.");

        IndividualProvider provider = new IndividualProvider();
        provider.setUsername(username);
        provider.setEmail(email);
        provider.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        provider.setDocumentType(DocumentType.CI);
        provider.setFirstName(firstName);
        provider.setLastName(lastName);
        provider.setCi(ci);
        provider.setStatus(ProviderStatus.APPROVED);
        provider.setPhone(phone);
        provider.setDepartment(department);
        provider.setCity(city);
        provider.setAddress(address);
        return providerRepository.save(provider);
    }

    private ActivityCategory ensureCategory(String name) {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .filter(category -> category.getName().equalsIgnoreCase(name))
                .findFirst().orElseGet(() -> {
                    ActivityCategory category = new ActivityCategory();
                    category.setName(name);
                    return categoryRepository.save(category);
                });
    }

    private void createActivityIfMissing(IndividualProvider provider, String name, String department, String city,
                                         String description, LocalDate firstDate, BigDecimal price, int capacity,
                                         int duration, String equipment, Set<ActivityCategory> categories,
                                         String imageName) throws java.io.IOException {
        Activity activity = activityRepository.findAllByOrderByNameAsc().stream()
                .filter(existing -> existing.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (activity == null) {
            activity = new Activity();
            activity.setProvider(provider);
            activity.setName(name);
            activity.setDepartment(department);
            activity.setCity(city);
            activity.setLocation(city);
            activity.setDescription(description);
            activity.setCategories(new HashSet<>(categories));
            activity.setFirstDate(firstDate);
            activity.setRecurrence(ActivityRecurrence.WEEKLY);
            activity.setWeekdays(EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY));
            activity.setStartTimes(List.of(LocalTime.of(9, 0), LocalTime.of(15, 0)));
            activity.setCapacity(capacity);
            activity.setPrice(price);
            activity.setDurationMinutes(duration);
            activity.setBookingCutoffMinutes(1440);
            activity.setCancellationCutoffMinutes(2880);
            activity.setEquipment(equipment);
        }
        if (activity.getPhotos().isEmpty()) {
            ActivityPhoto photo = new ActivityPhoto();
            photo.setActivity(activity);
            photo.setFileName(imageName);
            photo.setContentType("image/png");
            photo.setContent(new ClassPathResource("demo-images/" + imageName).getContentAsByteArray());
            photo.setDisplayOrder(0);
            activity.getPhotos().add(photo);
        }
        activityRepository.save(activity);
    }
}
