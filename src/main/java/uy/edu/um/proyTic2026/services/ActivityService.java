package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uy.edu.um.proyTic2026.dtos.*;
import uy.edu.um.proyTic2026.entities.*;
import uy.edu.um.proyTic2026.repositories.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import java.io.IOException;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final ActivityCategoryRepository categoryRepository;
    private final ActivityReservationRepository reservationRepository;
    private final ActivityReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final AccountEmailService accountEmailService;

    @Transactional(readOnly = true)
    public List<ActivityResponse> list() {
        return activityRepository.findAllByOrderByNameAsc().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> listForProvider(Long providerId) {
        return activityRepository.findAllByProvider_IdOrderByNameAsc(providerId).stream().map(this::response).toList();
    }

    @Transactional
    public ActivityResponse create(Long providerId, ActivityRequest request, List<MultipartFile> uploads) {
        Provider provider = userRepository.findById(providerId).filter(Provider.class::isInstance)
                .map(Provider.class::cast).orElseThrow(() -> new IllegalArgumentException("No se encontró el prestador."));
        if (provider.getStatus() != ProviderStatus.APPROVED) {
            throw new IllegalArgumentException("Solo los prestadores aprobados pueden publicar actividades.");
        }
        validate(request);
        Activity activity = new Activity();
        activity.setProvider(provider);
        apply(activity, request);
        addPhotos(activity, uploads);
        Activity saved = activityRepository.save(activity);
        String providerEmail = provider.getEmail();
        String activityName = saved.getName();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { accountEmailService.activityPublished(providerEmail, activityName); }
        });
        return response(saved);
    }

    @Transactional
    public ActivityResponse update(Long providerId, Long activityId, ActivityRequest request,
                                   List<MultipartFile> uploads, List<Long> keepPhotoIds) {
        Activity activity = ownedActivity(providerId, activityId);
        if (reservationRepository.existsByActivity_Id(activityId)) {
            throw new IllegalArgumentException("No se puede modificar una actividad que ya tiene reservas.");
        }
        validate(request);
        apply(activity, request);
        Set<Long> kept = keepPhotoIds == null ? Set.of() : new HashSet<>(keepPhotoIds);
        activity.getPhotos().removeIf(photo -> !kept.contains(photo.getId()));
        if (activity.getPhotos().size() + nonEmptyUploads(uploads).size() > 8) {
            throw new IllegalArgumentException("Podés guardar hasta 8 fotos por actividad.");
        }
        addPhotos(activity, uploads);
        return response(activityRepository.save(activity));
    }

    @Transactional
    public void delete(Long providerId, Long activityId) {
        Activity activity = ownedActivity(providerId, activityId);
        if (reservationRepository.existsByActivity_Id(activityId)) {
            throw new IllegalArgumentException("No se puede eliminar una actividad que ya tiene reservas.");
        }
        activityRepository.delete(activity);
    }

    @Transactional(readOnly = true)
    public ActivityResponse get(Long activityId) {
        return response(activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la actividad.")));
    }

    @Transactional(readOnly = true)
    public int remainingCapacity(Long activityId, LocalDateTime occurrence) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la actividad."));
        if (!isOccurrence(activity, occurrence)) throw new IllegalArgumentException("Ese horario no pertenece a la actividad.");
        return Math.max(0, activity.getCapacity() - (int) reservationRepository.countByActivity_IdAndActivityStart(activityId, occurrence));
    }

    @Transactional(readOnly = true)
    public ProviderPublicResponse publicProvider(Long providerId) {
        Provider provider = userRepository.findById(providerId).filter(Provider.class::isInstance)
                .map(Provider.class::cast).filter(item -> item.getStatus() == ProviderStatus.APPROVED)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el prestador."));
        String displayName = provider instanceof IndividualProvider individual
                ? individual.getFirstName() + " " + individual.getLastName()
                : provider instanceof CompanyProvider company ? company.getCompanyName() : "Prestador";
        return new ProviderPublicResponse(provider.getId(), displayName,
                provider instanceof CompanyProvider ? "Empresa" : "Prestador independiente",
                provider.getEmail(), provider.getPhone(), provider.getDepartment(), provider.getCity(), provider.getAddress(), provider.getWebsite(),
                provider.getInstagram(), provider.getTwitter(), provider.getWhatsapp(), provider.getYoutube(), provider.getFacebook(),
                Optional.ofNullable(reviewRepository.findAverageRatingByProviderId(providerId)).orElse(0.0),
                reviewRepository.countByProviderId(providerId));
    }

    @Transactional(readOnly = true)
    public ActivityPhoto photo(Long activityId, Long photoId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la actividad."));
        return activity.getPhotos().stream().filter(photo -> photo.getId().equals(photoId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la foto."));
    }

    private List<MultipartFile> nonEmptyUploads(List<MultipartFile> uploads) {
        return uploads == null ? List.of() : uploads.stream().filter(file -> file != null && !file.isEmpty()).toList();
    }

    private void addPhotos(Activity activity, List<MultipartFile> uploads) {
        List<MultipartFile> files = nonEmptyUploads(uploads);
        if (activity.getPhotos().size() + files.size() > 8) {
            throw new IllegalArgumentException("Podés guardar hasta 8 fotos por actividad.");
        }
        for (MultipartFile file : files) {
            String type = file.getContentType();
            if (!List.of("image/jpeg", "image/png", "image/webp").contains(type)) {
                throw new IllegalArgumentException("Las fotos deben estar en formato JPG, PNG o WEBP.");
            }
            if (file.getSize() > 8L * 1024 * 1024) {
                throw new IllegalArgumentException("Cada foto puede pesar hasta 8 MB.");
            }
            try {
                ActivityPhoto photo = new ActivityPhoto();
                photo.setActivity(activity);
                photo.setFileName(StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "foto")));
                photo.setContentType(type);
                photo.setContent(file.getBytes());
                photo.setDisplayOrder(activity.getPhotos().size());
                activity.getPhotos().add(photo);
            } catch (IOException error) {
                throw new IllegalArgumentException("No se pudo leer una de las fotos.");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> reviews(Long activityId) {
        if (!activityRepository.existsById(activityId)) throw new IllegalArgumentException("No se encontró la actividad.");
        return reviewRepository.findAllByReservation_Activity_IdOrderByCreatedAtDesc(activityId)
                .stream().map(ReviewResponse::from).toList();
    }

    @Transactional
    public ReservationResponse reserve(Long touristId, Long activityId, LocalDateTime occurrence) {
        Tourist tourist = userRepository.findById(touristId).filter(Tourist.class::isInstance)
                .map(Tourist.class::cast).orElseThrow(() -> new IllegalArgumentException("Solo los turistas pueden reservar."));
        Activity activity = activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la actividad."));
        if (!isOccurrence(activity, occurrence)) throw new IllegalArgumentException("Ese horario no pertenece a la actividad.");
        if (!occurrence.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("Ya no se puede reservar una fecha pasada.");
        if (occurrence.minusHours(activity.getBookingCutoffHours()).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El plazo para reservar esta fecha ya cerró.");
        }
        if (reservationRepository.existsByActivity_IdAndTourist_IdAndActivityStart(activityId, touristId, occurrence)) {
            throw new IllegalArgumentException("Ya tenés una reserva para ese horario.");
        }
        long reserved = reservationRepository.countByActivity_IdAndActivityStart(activityId, occurrence);
        if (reserved >= activity.getCapacity()) throw new IllegalArgumentException("No quedan cupos para ese horario.");
        ActivityReservation reservation = new ActivityReservation();
        reservation.setActivity(activity);
        reservation.setTourist(tourist);
        reservation.setActivityStart(occurrence);
        return ReservationResponse.from(reservationRepository.save(reservation), false);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> reservations(Long touristId) {
        return reservationRepository.findAllByTourist_IdOrderByActivityStartDesc(touristId).stream()
                .map(reservation -> ReservationResponse.from(reservation,
                        reviewRepository.findByReservation_Id(reservation.getId()).isPresent())).toList();
    }

    @Transactional
    public ReviewResponse review(Long touristId, Long reservationId, ReviewRequest request) {
        ActivityReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la reserva."));
        if (!reservation.getTourist().getId().equals(touristId)) {
            throw new IllegalArgumentException("Solo podés opinar sobre tus propias reservas.");
        }
        if (!reservation.getActivityStart().plusMinutes(reservation.getActivity().getDurationMinutes())
                .isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Podés dejar tu comentario después de que termine la actividad.");
        }
        if (reviewRepository.findByReservation_Id(reservationId).isPresent()) {
            throw new IllegalArgumentException("Ya dejaste una opinión para esta reserva.");
        }
        ActivityReview review = new ActivityReview();
        review.setReservation(reservation);
        review.setRating(request.rating());
        review.setComment(request.comment().trim());
        return ReviewResponse.from(reviewRepository.save(review));
    }

    private Activity ownedActivity(Long providerId, Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la actividad."));
        if (!activity.getProvider().getId().equals(providerId)) {
            throw new IllegalArgumentException("No tenés permiso para modificar esta actividad.");
        }
        return activity;
    }

    private void validate(ActivityRequest request) {
        if (request.startTimes().size() > 8 || new HashSet<>(request.startTimes()).size() != request.startTimes().size()) {
            throw new IllegalArgumentException("Ingresá entre 1 y 8 horarios distintos por día.");
        }
        if (request.recurrence() != ActivityRecurrence.ONCE
                && request.repeatUntil() != null && !request.repeatUntil().isAfter(request.firstDate())) {
            throw new IllegalArgumentException("La repetición debe terminar después de la primera fecha.");
        }
        if (request.recurrence() == ActivityRecurrence.ONCE
                && request.firstDate().equals(LocalDate.now())
                && request.startTimes().stream().noneMatch(time -> time.isAfter(LocalTime.now()))) {
            throw new IllegalArgumentException("Para una actividad de una sola vez, elegí una hora futura.");
        }
        if ((request.recurrence() == ActivityRecurrence.WEEKLY || request.recurrence() == ActivityRecurrence.BIWEEKLY
                || request.recurrence() == ActivityRecurrence.SEASONAL)
                && (request.weekdays() == null || request.weekdays().isEmpty())) {
            throw new IllegalArgumentException("Elegí al menos un día de la semana para repetir la actividad.");
        }
        if (request.recurrence() == ActivityRecurrence.SEASONAL && request.repeatUntil() == null) {
            throw new IllegalArgumentException("Indicá hasta qué fecha dura la temporada.");
        }
        if (request.maximumAge() != null && request.minimumAge() != null && request.maximumAge() < request.minimumAge()) {
            throw new IllegalArgumentException("La edad máxima debe ser mayor o igual a la mínima.");
        }
        if (request.maximumHeightCm() != null && request.minimumHeightCm() != null
                && request.maximumHeightCm() < request.minimumHeightCm()) {
            throw new IllegalArgumentException("La altura máxima debe ser mayor o igual a la mínima.");
        }
        if (request.categoryIds().stream().distinct().count() != request.categoryIds().size()) {
            throw new IllegalArgumentException("No repitas categorías.");
        }
        if (categoryRepository.findAllById(request.categoryIds()).size() != request.categoryIds().size()) {
            throw new IllegalArgumentException("Una o más categorías ya no están disponibles.");
        }
    }

    private void apply(Activity activity, ActivityRequest request) {
        activity.setName(request.name().trim());
        activity.setDepartment(request.department().trim());
        activity.setCity(request.city().trim());
        activity.setLocation(request.location().trim());
        activity.setDescription(request.description().trim());
        activity.setCategories(new HashSet<>(categoryRepository.findAllById(request.categoryIds())));
        activity.setFirstDate(request.firstDate());
        activity.setRecurrence(request.recurrence());
        activity.setRepeatUntil(request.recurrence() == ActivityRecurrence.ONCE ? null : request.repeatUntil());
        activity.setStartTimes(request.startTimes().stream().sorted().toList());
        activity.setWeekdays(request.weekdays() == null ? Set.of() : new HashSet<>(request.weekdays()));
        activity.setCapacity(request.capacity());
        activity.setPrice(request.price());
        activity.setDurationMinutes(request.durationMinutes());
        activity.setBookingCutoffHours(request.bookingCutoffHours());
        activity.setEquipment(blankToNull(request.equipment()));
        activity.setMinimumAge(request.minimumAge());
        activity.setMaximumAge(request.maximumAge());
        activity.setMinimumHeightCm(request.minimumHeightCm());
        activity.setMaximumHeightCm(request.maximumHeightCm());
        activity.setDifficulty(blankToNull(request.difficulty()));
        activity.setOtherRequirements(blankToNull(request.otherRequirements()));
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private ActivityResponse response(Activity activity) {
        List<ActivityReview> reviews = reviewRepository.findAllByReservation_Activity_IdOrderByCreatedAtDesc(activity.getId());
        double average = reviews.isEmpty() ? 0 : reviews.stream().mapToInt(ActivityReview::getRating).average().orElse(0);
        long recentReservations = reservationRepository.countByActivity_IdAndReservedAtAfter(activity.getId(), Instant.now().minus(30, ChronoUnit.DAYS));
        return ActivityResponse.from(activity, upcoming(activity), average, reviews.size(), recentReservations);
    }

    private List<LocalDateTime> upcoming(Activity activity) {
        LocalDate today = LocalDate.now();
        LocalDate last = activity.getRepeatUntil() == null ? today.plusDays(90)
                : activity.getRepeatUntil().isBefore(today.plusDays(90)) ? activity.getRepeatUntil() : today.plusDays(90);
        LocalDate begin = activity.getFirstDate().isAfter(today) ? activity.getFirstDate() : today;
        if (activity.getRecurrence() == ActivityRecurrence.ONCE) last = activity.getFirstDate();
        List<LocalDateTime> result = new ArrayList<>();
        for (LocalDate date = begin; !date.isAfter(last) && result.size() < 12; date = date.plusDays(1)) {
            if (occursOn(activity, date)) {
                for (LocalTime time : activity.getStartTimes()) {
                    LocalDateTime occurrence = date.atTime(time);
                    if (occurrence.isAfter(LocalDateTime.now())) result.add(occurrence);
                    if (result.size() >= 12) break;
                }
            }
        }
        return result;
    }

    private boolean isOccurrence(Activity activity, LocalDateTime dateTime) {
        return activity.getStartTimes().contains(dateTime.toLocalTime()) && occursOn(activity, dateTime.toLocalDate());
    }

    private boolean occursOn(Activity activity, LocalDate date) {
        if (date.isBefore(activity.getFirstDate()) || (activity.getRepeatUntil() != null && date.isAfter(activity.getRepeatUntil()))) return false;
        return switch (activity.getRecurrence()) {
            case ONCE -> date.equals(activity.getFirstDate());
            case DAILY -> true;
            case WEEKLY -> selectedWeekday(activity, date) && ChronoUnit.WEEKS.between(
                    activity.getFirstDate().with(DayOfWeek.MONDAY), date.with(DayOfWeek.MONDAY)) % 1 == 0;
            case BIWEEKLY -> selectedWeekday(activity, date) && ChronoUnit.WEEKS.between(
                    activity.getFirstDate().with(DayOfWeek.MONDAY), date.with(DayOfWeek.MONDAY)) % 2 == 0;
            case MONTHLY -> date.getDayOfMonth() == activity.getFirstDate().getDayOfMonth();
            case SEASONAL -> selectedWeekday(activity, date);
        };
    }

    private boolean selectedWeekday(Activity activity, LocalDate date) {
        Set<DayOfWeek> days = activity.getWeekdays();
        if (days.isEmpty()) days = Set.of(activity.getFirstDate().getDayOfWeek());
        return days.contains(date.getDayOfWeek());
    }
}
