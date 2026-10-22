package uy.edu.um.proyTic2026.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.entities.*;
import uy.edu.um.proyTic2026.repositories.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DemoActivitiesInitializer {
    private final ActivityRepository activityRepository;
    private final ActivityCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ResourceLoader resourceLoader;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedActivities() {
        IndividualProvider provider = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        "prestador.demo", "prestador.demo")
                .filter(IndividualProvider.class::isInstance).map(IndividualProvider.class::cast)
                .filter(item -> item.getStatus() == ProviderStatus.APPROVED).orElse(null);
        if (provider == null) return;
        List<ActivityCategory> categories = categoryRepository.findAllByOrderByNameAsc();
        if (categories.isEmpty()) return;
        Map<String, ActivityCategory> byName = new HashMap<>();
        categories.forEach(category -> byName.put(category.getName().toLowerCase(Locale.ROOT), category));
        LocalDate today = LocalDate.now();
        List<DemoActivity> demos = List.of(
                new DemoActivity("Atardecer en Casa Pueblo", "Maldonado", "Punta Ballena", "Casapueblo, Punta Ballena",
                        "Recorré las terrazas de Casapueblo y disfrutá el atardecer sobre el Río de la Plata.", "Cultural", "casa-pueblo.png", 850, 14, 90, 0, null, "Fácil", "Calzado cómodo", ActivityRecurrence.DAILY, 2, 2, 24),
                new DemoActivity("Paseo histórico por Calle de los Suspiros", "Colonia", "Colonia del Sacramento", "Barrio Histórico",
                        "Caminata guiada por calles empedradas, arquitectura colonial y relatos de la antigua Colonia.", "Historia", "calle-suspiros.png", 620, 12, 100, 0, null, "Fácil", "Ninguno", ActivityRecurrence.WEEKLY, 3, 3, 12),
                new DemoActivity("Sabores de bodega en José Ignacio", "Maldonado", "José Ignacio", "Camino Eugenio Sainz Martínez",
                        "Visita a una bodega local con recorrido entre viñedos y degustación de etiquetas de la casa.", "Gastronomía", "bodega-jose-ignacio.png", 1800, 10, 120, 18, null, "Fácil", "Documento de identidad", ActivityRecurrence.WEEKLY, 4, 4, 24),
                new DemoActivity("Faro y costa de José Ignacio", "Maldonado", "José Ignacio", "Faro de José Ignacio",
                        "Paseo costero para conocer el faro, la historia del pueblo y sus playas abiertas al océano.", "Tranquilo", "faro-jose-ignacio.png", 500, 16, 75, 0, null, "Fácil", "Protector solar", ActivityRecurrence.DAILY, 5, 5, 4),
                new DemoActivity("Cabalgata entre campos de Durazno", "Durazno", "Durazno", "Establecimiento rural, Durazno",
                        "Salida a caballo por caminos rurales y praderas, acompañada por guías del establecimiento.", "Naturaleza", "campo-durazno.png", 1450, 8, 120, 12, 140, "Inicial", "Pantalón largo y calzado cerrado", ActivityRecurrence.WEEKLY, 6, 6, 24),
                new DemoActivity("Ascenso guiado al Cerro Pan de Azúcar", "Maldonado", "Piriápolis", "Reserva de Fauna y Cerro Pan de Azúcar",
                        "Subida guiada por el sendero del cerro con pausas para descansar y disfrutar las vistas de la zona.", "Aventura", "cerro-pan-azucar.png", 900, 10, 210, 12, 0, "Intermedio", "Agua, gorro y calzado de trekking", ActivityRecurrence.BIWEEKLY, 7, 7, 24),
                new DemoActivity("Caminata por Pozos Azules", "Maldonado", "Pan de Azúcar", "Senderos de Pozos Azules",
                        "Explorá el arroyo y sus pozas de agua clara en un recorrido acompañado por senderos naturales.", "Naturaleza", "pozos-azules.png", 1100, 10, 180, 10, 0, "Intermedio", "Ropa cómoda y muda de ropa", ActivityRecurrence.SEASONAL, 8, 8, 24),
                new DemoActivity("Atardecer entre los médanos de Valizas", "Rocha", "Barra de Valizas", "Médanos de Valizas",
                        "Caminata por la arena hasta los médanos para ver caer el sol sobre la costa de Rocha.", "Familiar", "medano-valizas.png", 700, 15, 100, 0, null, "Fácil", "Agua y protección solar", ActivityRecurrence.DAILY, 9, 9, 6),
                new DemoActivity("Paseo en barco a Isla Gorriti", "Maldonado", "Punta del Este", "Puerto de Punta del Este",
                        "Navegación corta hasta Isla Gorriti para recorrer sus playas y senderos con tiempo libre.", "Familiar", "isla-gorriti.png", 1250, 18, 180, 0, null, "Fácil", "Ropa cómoda y protector solar", ActivityRecurrence.WEEKLY, 10, 10, 12),
                new DemoActivity("Avistamiento responsable de ballenas", "Maldonado", "Punta del Este", "Puerto de Punta del Este",
                        "Salida embarcada de observación durante la temporada, con guía y recomendaciones para cuidar la fauna.", "Naturaleza", "ballena-punta-del-este.png", 2200, 10, 150, 0, null, "Fácil", "Abrigo liviano y protección solar", ActivityRecurrence.SEASONAL, 11, 11, 24)
        );
        for (DemoActivity demo : demos) {
            if (activityRepository.existsByNameIgnoreCase(demo.name())) continue;
            Activity activity = new Activity();
            activity.setProvider(provider);
            activity.setName(demo.name()); activity.setDepartment(demo.department()); activity.setCity(demo.city());
            activity.setLocation(demo.location()); activity.setDescription(demo.description());
            activity.setCategories(Set.of(byName.getOrDefault(demo.category().toLowerCase(Locale.ROOT), categories.get(0))));
            activity.setRecurrence(demo.recurrence());
            activity.setFirstDate(today.plusDays(demo.daysFromToday()));
            activity.setRepeatUntil(demo.recurrence() == ActivityRecurrence.ONCE ? null : today.plusDays(demo.daysFromToday() + demo.durationDays()));
            activity.setStartTimes(List.of(LocalTime.of(10, 0), LocalTime.of(15, 0)));
            activity.setWeekdays(demo.recurrence() == ActivityRecurrence.WEEKLY || demo.recurrence() == ActivityRecurrence.BIWEEKLY || demo.recurrence() == ActivityRecurrence.SEASONAL
                    ? Set.of(activity.getFirstDate().getDayOfWeek()) : Set.of());
            activity.setCapacity(demo.capacity()); activity.setPrice(BigDecimal.valueOf(demo.price()));
            activity.setDurationMinutes(demo.durationMinutes()); activity.setBookingCutoffHours(demo.bookingCutoffHours());
            activity.setMinimumAge(demo.minimumAge() > 0 ? demo.minimumAge() : null);
            activity.setMinimumHeightCm(demo.minimumHeightCm()); activity.setDifficulty(demo.difficulty());
            activity.setEquipment(demo.equipment());
            attachPhoto(activity, demo.photo());
            activityRepository.save(activity);
        }
    }

    private void attachPhoto(Activity activity, String fileName) {
        try {
            Resource resource = resourceLoader.getResource("classpath:demo-activity-images/" + fileName);
            if (!resource.exists()) return;
            ActivityPhoto photo = new ActivityPhoto();
            photo.setActivity(activity); photo.setFileName(fileName);
            photo.setContentType("image/png"); photo.setContent(resource.getInputStream().readAllBytes());
            photo.setDisplayOrder(0); activity.getPhotos().add(photo);
        } catch (IOException ignored) { /* Si falta una imagen, la ficha conserva el fondo con el logo. */ }
    }

    private record DemoActivity(String name, String department, String city, String location, String description,
                                String category, String photo, long price, int capacity, int durationMinutes,
                                int minimumAge, Integer minimumHeightCm, String difficulty, String equipment,
                                ActivityRecurrence recurrence, int daysFromToday, int durationDays, int bookingCutoffHours) { }
}
