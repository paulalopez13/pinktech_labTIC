package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.scheduling.annotation.Scheduled;
import uy.edu.um.proyTic2026.dtos.*;
import uy.edu.um.proyTic2026.entities.*;
import uy.edu.um.proyTic2026.repositories.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.math.BigDecimal;

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
    private final ActivityFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final AccountEmailService accountEmailService;
    private final StripeGatewayService stripeGateway;

    @Transactional(readOnly = true)
    public List<ActivityResponse> list() {
        return activityRepository.findAllByOrderByNameAsc().stream().filter(activity -> !activity.isArchived()).map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> listForProvider(Long providerId) {
        return activityRepository.findAllByProvider_IdAndArchivedFalseOrderByNameAsc(providerId).stream().map(this::response).toList();
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
    public Map<String, Boolean> delete(Long providerId, Long activityId) {
        Activity activity = ownedActivity(providerId, activityId);
        List<ActivityReservation> reservations = reservationRepository.findAllByActivity_IdOrderByActivityStartAsc(activityId);
        boolean hasPendingPayment = reservations.stream().anyMatch(this::paymentIsPending);
        if (reservations.isEmpty()) {
            favoriteRepository.deleteAllByActivity_Id(activityId);
            activityRepository.delete(activity);
            return Map.of("retainedForReservations", false, "hasPendingPayment", false);
        }
        activity.setArchived(true);
        activity.setAcceptingReservations(false);
        if (!hasPendingPayment) {
            activity.getPhotos().clear();
            activity.getCategories().clear();
            activity.getStartTimes().clear();
            activity.getWeekdays().clear();
            activity.setDescription("");
            activity.setEquipment(null);
            activity.setMinimumAge(null);
            activity.setMaximumAge(null);
            activity.setMinimumHeightCm(null);
            activity.setMaximumHeightCm(null);
            activity.setDifficulty(null);
            activity.setOtherRequirements(null);
            activity.setRepeatUntil(null);
            activity.setCapacity(0);
        }
        activityRepository.save(activity);
        return Map.of("retainedForReservations", true, "hasPendingPayment", hasPendingPayment);
    }

    @Transactional(readOnly = true)
    public ActivityResponse get(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no disponible."));
        if (activity.isArchived() && !hasPendingPayment(activityId)) {
            throw new IllegalArgumentException("Actividad no disponible.");
        }
        return response(activity);
    }

    @Transactional(readOnly = true)
    public int remainingCapacity(Long activityId, LocalDateTime occurrence) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la actividad."));
        if (!isOccurrence(activity, occurrence)) throw new IllegalArgumentException("Ese horario no pertenece a la actividad.");
        return Math.max(0, activity.getCapacity() - (int) reservationRepository.countBookedParticipants(activityId, occurrence, Instant.now()));
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
    public ReservationResponse reserveSandbox(Long touristId, Long activityId, GroupReservationRequest request) {
        Tourist tourist = userRepository.findById(touristId).filter(Tourist.class::isInstance)
                .map(Tourist.class::cast).orElseThrow(() -> new IllegalArgumentException("Solo los turistas pueden reservar."));
        Activity activity = activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la actividad."));
        if (!activity.isAcceptingReservations() || activity.isArchived()) {
            throw new IllegalArgumentException("Esta actividad ya no acepta nuevas reservas.");
        }
        LocalDateTime occurrence = request.activityStart();
        if (!isOccurrence(activity, occurrence)) throw new IllegalArgumentException("Ese horario no pertenece a la actividad.");
        if (!occurrence.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("Ya no se puede reservar una fecha pasada.");
        if (occurrence.minusMinutes(activity.getBookingCutoffMinutes()).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El plazo para reservar esta fecha ya cerró.");
        }
        if (reservationRepository.existsByActivity_IdAndTourist_IdAndActivityStart(activityId, touristId, occurrence)) {
            throw new IllegalArgumentException("Ya tenés una reserva para ese horario.");
        }
        int count = request.participants().size();
        long reserved = reservationRepository.countBookedParticipants(activityId, occurrence, Instant.now());
        if (reserved + count > activity.getCapacity()) throw new IllegalArgumentException("No quedan suficientes cupos para todas las personas.");
        validateParticipants(activity, request.participants());

        ActivityReservation reservation = new ActivityReservation();
        reservation.setActivity(activity);
        reservation.setTourist(tourist);
        reservation.setActivityStart(occurrence);
        reservation.setParticipantCount(count);
        reservation.setContactName(request.contactName().trim());
        reservation.setContactEmail(request.contactEmail().trim());
        reservation.setContactPhone(request.contactPhone().trim());
        boolean free = activity.getPrice().signum() == 0;
        reservation.setPaymentStatus(free ? ReservationPaymentStatus.APPROVED : ReservationPaymentStatus.PENDING_PAYMENT);
        reservation.setPaymentExpiresAt(free ? null : Instant.now().plus(Duration.ofHours(2)));
        reservation.setPaymentOrderId(null);
        for (ReservationParticipantRequest item : request.participants()) {
            ReservationParticipant participant = new ReservationParticipant();
            participant.setReservation(reservation);
            participant.setFullName(item.fullName().trim());
            participant.setDocumentType(item.documentType());
            participant.setDocumentNumber(item.documentNumber().trim().toUpperCase(Locale.ROOT));
            participant.setAge(item.age());
            participant.setHeightCm(item.heightCm());
            participant.setLevel(item.level() == null || item.level().isBlank() ? null : item.level());
            reservation.getParticipants().add(participant);
        }
        ActivityReservation saved = reservationRepository.save(reservation);
        if (free) notifyReservationConfirmed(reservation.getContactEmail(), reservation.getContactName(), activity,
                occurrence, count, BigDecimal.ZERO);
        return ReservationResponse.from(saved, false);
    }

    @Transactional
    public Map<String, Object> createStripeCheckout(Long touristId, Long reservationId) {
        ActivityReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la reserva."));
        if (!reservation.getTourist().getId().equals(touristId)) throw new IllegalArgumentException("Esta reserva no te pertenece.");
        if (reservation.getPaymentStatus() != ReservationPaymentStatus.PENDING_PAYMENT
                && reservation.getPaymentStatus() != ReservationPaymentStatus.PAYMENT_RETRY_PENDING)
            throw new IllegalArgumentException("La reserva no está esperando un pago.");
        Instant now = Instant.now();
        if (reservation.getPaymentExpiresAt() == null || !reservation.getPaymentExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Expiró el tiempo de pago. La reserva venció.");
        }
        if (reservation.getPaymentOrderId() != null && reservation.getPaymentOrderId().startsWith("cs_")) {
            JsonNode existing = stripeGateway.retrieveCheckout(reservation.getPaymentOrderId());
            stripeGateway.validateCheckoutSession(reservation, existing, reservation.getPaymentOrderId(), false);
            if ("paid".equals(existing.path("payment_status").asText())) {
                completeStripePayment(reservation, existing);
                return Map.of("paymentStatus", reservation.getPaymentStatus().name(), "url", "");
            }
            if ("open".equals(existing.path("status").asText()))
            {
                String checkoutUrl = existing.path("url").asText();
                stripeGateway.validateCheckoutUrl(checkoutUrl);
                return Map.of("paymentStatus", reservation.getPaymentStatus().name(), "url", checkoutUrl);
            }
            if ("complete".equals(existing.path("status").asText()))
                throw new IllegalArgumentException("Stripe todavía está confirmando el pago. Revisá el estado en tus reservas antes de volver a intentarlo.");
        }
        StripeGatewayService.Checkout checkout = stripeGateway.createCheckout(reservation);
        reservation.setPaymentOrderId(checkout.id());
        return Map.of("paymentStatus", reservation.getPaymentStatus().name(), "url", checkout.url());
    }

    @Transactional
    public ReservationResponse verifyStripeCheckout(Long touristId, Long reservationId, String sessionId) {
        ActivityReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la reserva."));
        if (!reservation.getTourist().getId().equals(touristId)) throw new IllegalArgumentException("Esta reserva no te pertenece.");
        if (sessionId == null || !sessionId.equals(reservation.getPaymentOrderId()))
            throw new IllegalArgumentException("La sesión de pago no corresponde a esta reserva.");
        JsonNode session = stripeGateway.retrieveCheckout(sessionId);
        stripeGateway.validateCheckoutSession(reservation, session, sessionId, false);
        if ("paid".equals(session.path("payment_status").asText())) completeStripePayment(reservation, session);
        else if ((reservation.getPaymentStatus() == ReservationPaymentStatus.PENDING_PAYMENT
                || reservation.getPaymentStatus() == ReservationPaymentStatus.PAYMENT_RETRY_PENDING)
                && (reservation.getPaymentExpiresAt() == null || !reservation.getPaymentExpiresAt().isAfter(Instant.now()))) {
            reservation.setPaymentStatus(ReservationPaymentStatus.EXPIRED);
            reservation.setPaymentExpiresAt(null);
            stripeGateway.expireCheckout(sessionId);
            offerAvailablePlaces(reservation.getActivity().getId(), reservation.getActivityStart(), Instant.now());
        }
        return ReservationResponse.from(reservation, false);
    }

    @Transactional
    public Map<String, Object> cancelStripeCheckout(Long touristId, Long reservationId) {
        ActivityReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la reserva."));
        if (!reservation.getTourist().getId().equals(touristId)) throw new IllegalArgumentException("Esta reserva no te pertenece.");
        if (reservation.getPaymentStatus() != ReservationPaymentStatus.PENDING_PAYMENT
                && reservation.getPaymentStatus() != ReservationPaymentStatus.PAYMENT_RETRY_PENDING)
            throw new IllegalArgumentException("La reserva ya no tiene un pago pendiente.");
        String sessionId = reservation.getPaymentOrderId();
        if (sessionId == null || !sessionId.startsWith("cs_"))
            throw new IllegalArgumentException("No encontramos una sesión de pago para cancelar.");
        JsonNode session = stripeGateway.retrieveCheckout(sessionId);
        stripeGateway.validateCheckoutSession(reservation, session, sessionId, false);
        if ("paid".equals(session.path("payment_status").asText())) {
            completeStripePayment(reservation, session);
            return Map.of("cancelled", false, "paymentStatus", reservation.getPaymentStatus().name());
        }
        if ("open".equals(session.path("status").asText())) {
            stripeGateway.expireCheckout(sessionId);
            session = stripeGateway.retrieveCheckout(sessionId);
            stripeGateway.validateCheckoutSession(reservation, session, sessionId, false);
            if ("paid".equals(session.path("payment_status").asText())) {
                completeStripePayment(reservation, session);
                return Map.of("cancelled", false, "paymentStatus", reservation.getPaymentStatus().name());
            }
        }
        if (!"expired".equals(session.path("status").asText()))
            throw new IllegalArgumentException("Stripe todavía no confirmó la cancelación del pago. Revisá tus reservas antes de volver a intentarlo.");
        if (reservation.getPaymentExpiresAt() == null || !reservation.getPaymentExpiresAt().isAfter(Instant.now())) {
            reservation.setPaymentStatus(ReservationPaymentStatus.EXPIRED);
            reservation.setPaymentExpiresAt(null);
            offerAvailablePlaces(reservation.getActivity().getId(), reservation.getActivityStart(), Instant.now());
        }
        return Map.of("cancelled", true, "paymentStatus", reservation.getPaymentStatus().name());
    }

    private void completeStripePayment(ActivityReservation reservation, JsonNode session) {
        stripeGateway.validateCheckoutSession(reservation, session, reservation.getPaymentOrderId(), true);
        if (reservation.getPaymentStatus() == ReservationPaymentStatus.APPROVED) return;
        Instant now = Instant.now();
        if ((reservation.getPaymentStatus() != ReservationPaymentStatus.PENDING_PAYMENT
                && reservation.getPaymentStatus() != ReservationPaymentStatus.PAYMENT_RETRY_PENDING)
                || reservation.getPaymentExpiresAt() == null || !reservation.getPaymentExpiresAt().isAfter(now)) {
            String paymentIntent = session.path("payment_intent").asText("");
            stripeGateway.refundPaymentIntent(paymentIntent, reservation.getId());
            reservation.setPaymentStatus(ReservationPaymentStatus.EXPIRED);
            reservation.setPaymentExpiresAt(null);
            offerAvailablePlaces(reservation.getActivity().getId(), reservation.getActivityStart(), now);
            return;
        }
        reservation.setPaymentStatus(ReservationPaymentStatus.APPROVED);
        reservation.setPaymentExpiresAt(null);
        BigDecimal total = reservation.getActivity().getPrice().multiply(BigDecimal.valueOf(reservation.getParticipantCount()));
        notifyReservationConfirmed(reservation.getContactEmail(), reservation.getContactName(), reservation.getActivity(),
                reservation.getActivityStart(), reservation.getParticipantCount(), total);
    }

    @Transactional
    public void handleStripeWebhook(String eventType, JsonNode object) {
        if ("checkout.session.completed".equals(eventType) || "checkout.session.async_payment_succeeded".equals(eventType)) {
            String sessionId = object.path("id").asText();
            reservationRepository.findByPaymentOrderId(sessionId).ifPresent(reservation -> {
                if (reservation.getPaymentOrderId().equals(sessionId)
                        && reservation.getId().toString().equals(object.path("metadata").path("reservation_id").asText())
                        && "paid".equals(object.path("payment_status").asText())) completeStripePayment(reservation, object);
            });
        } else if ("checkout.session.expired".equals(eventType)) {
            reservationRepository.findByPaymentOrderId(object.path("id").asText()).ifPresent(reservation -> {
                stripeGateway.validateCheckoutSession(reservation, object, reservation.getPaymentOrderId(), false);
                if ("expired".equals(object.path("status").asText())
                        && !"paid".equals(object.path("payment_status").asText())
                        && (reservation.getPaymentStatus() == ReservationPaymentStatus.PENDING_PAYMENT
                        || reservation.getPaymentStatus() == ReservationPaymentStatus.PAYMENT_RETRY_PENDING)
                        && (reservation.getPaymentExpiresAt() == null || !reservation.getPaymentExpiresAt().isAfter(Instant.now()))) {
                    reservation.setPaymentStatus(ReservationPaymentStatus.EXPIRED);
                    reservation.setPaymentExpiresAt(null);
                    offerAvailablePlaces(reservation.getActivity().getId(), reservation.getActivityStart(), Instant.now());
                }
            });
        } else if ("checkout.session.async_payment_failed".equals(eventType)) {
            reservationRepository.findByPaymentOrderId(object.path("id").asText()).ifPresent(reservation -> {
                stripeGateway.validateCheckoutSession(reservation, object, reservation.getPaymentOrderId(), false);
                if (!"paid".equals(object.path("payment_status").asText())) markPaymentRetryPending(reservation);
            });
        } else if ("payment_intent.payment_failed".equals(eventType)) {
            String id = object.path("metadata").path("reservation_id").asText();
            try {
                reservationRepository.findById(Long.parseLong(id)).ifPresent(reservation -> {
                    if (reservation.getPaymentStatus() == ReservationPaymentStatus.PENDING_PAYMENT
                            && reservation.getPaymentExpiresAt() != null
                            && reservation.getPaymentExpiresAt().isAfter(Instant.now())
                            && reservation.getPaymentOrderId() != null
                            && reservation.getPaymentOrderId().startsWith("cs_")) {
                        JsonNode session = stripeGateway.retrieveCheckout(reservation.getPaymentOrderId());
                        stripeGateway.validateCheckoutSession(reservation, session, reservation.getPaymentOrderId(), false);
                        if (object.path("id").asText().equals(session.path("payment_intent").asText()))
                            markPaymentRetryPending(reservation);
                    }
                });
            } catch (NumberFormatException ignored) { }
        }
    }

    private void markPaymentRetryPending(ActivityReservation reservation) {
        if (reservation.getPaymentStatus() != ReservationPaymentStatus.PENDING_PAYMENT
                || reservation.getPaymentExpiresAt() == null
                || !reservation.getPaymentExpiresAt().isAfter(Instant.now())) return;
        reservation.setPaymentStatus(ReservationPaymentStatus.PAYMENT_RETRY_PENDING);
        reservation.setPaymentExpiresAt(Instant.now().plus(Duration.ofHours(2)));
    }

    @Transactional
    public ReservationResponse joinWaitlist(Long touristId, Long activityId, GroupReservationRequest request) {
        Tourist tourist = userRepository.findById(touristId).filter(Tourist.class::isInstance)
                .map(Tourist.class::cast).orElseThrow(() -> new IllegalArgumentException("Solo los turistas pueden anotarse."));
        Activity activity = activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la actividad."));
        LocalDateTime occurrence = request.activityStart();
        if (!activity.isAcceptingReservations() || activity.isArchived()) throw new IllegalArgumentException("Esta actividad ya no acepta reservas.");
        if (!isOccurrence(activity, occurrence) || !occurrence.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("Ese horario no está disponible.");
        if (occurrence.minusMinutes(activity.getBookingCutoffMinutes()).isBefore(LocalDateTime.now())) throw new IllegalArgumentException("El plazo para anotarse ya cerró.");
        if (reservationRepository.existsByActivity_IdAndTourist_IdAndActivityStart(activityId, touristId, occurrence))
            throw new IllegalArgumentException("Ya tenés una reserva o solicitud para ese horario.");
        int count = request.participants().size();
        long available = activity.getCapacity() - reservationRepository.countBookedParticipants(activityId, occurrence, Instant.now());
        if (available >= count) throw new IllegalArgumentException("Se liberaron cupos. Volvé y elegí ese horario para reservar directamente.");
        validateParticipants(activity, request.participants());
        ActivityReservation entry = createReservationData(tourist, activity, request);
        entry.setPaymentStatus(ReservationPaymentStatus.WAITLISTED);
        entry.setPaymentExpiresAt(null);
        return ReservationResponse.from(reservationRepository.save(entry), false);
    }

    private ActivityReservation createReservationData(Tourist tourist, Activity activity, GroupReservationRequest request) {
        ActivityReservation reservation = new ActivityReservation();
        reservation.setActivity(activity);
        reservation.setTourist(tourist);
        reservation.setActivityStart(request.activityStart());
        reservation.setParticipantCount(request.participants().size());
        reservation.setContactName(request.contactName().trim());
        reservation.setContactEmail(request.contactEmail().trim());
        reservation.setContactPhone(request.contactPhone().trim());
        for (ReservationParticipantRequest item : request.participants()) {
            ReservationParticipant participant = new ReservationParticipant();
            participant.setReservation(reservation);
            participant.setFullName(item.fullName().trim());
            participant.setDocumentType(item.documentType());
            participant.setDocumentNumber(item.documentNumber().trim().toUpperCase(Locale.ROOT));
            participant.setAge(item.age());
            participant.setHeightCm(item.heightCm());
            participant.setLevel(item.level() == null || item.level().isBlank() ? null : item.level());
            reservation.getParticipants().add(participant);
        }
        return reservation;
    }

    @Transactional
    public ReservationResponse answerWaitlistOffer(Long touristId, Long reservationId, boolean accept) {
        ActivityReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la solicitud."));
        if (!reservation.getTourist().getId().equals(touristId)) throw new IllegalArgumentException("Esta solicitud no te pertenece.");
        if (reservation.getPaymentStatus() != ReservationPaymentStatus.WAITLIST_OFFERED)
            throw new IllegalArgumentException("Esta oferta ya no está disponible.");
        Instant now = Instant.now();
        if (reservation.getPaymentExpiresAt() == null || !reservation.getPaymentExpiresAt().isAfter(now)) {
            reservation.setPaymentStatus(ReservationPaymentStatus.CANCELLED);
            reservation.setPaymentExpiresAt(null);
            offerAvailablePlaces(reservation.getActivity().getId(), reservation.getActivityStart(), now);
            return ReservationResponse.from(reservation, false);
        }
        if (!accept) {
            reservation.setPaymentStatus(ReservationPaymentStatus.CANCELLED);
            reservation.setPaymentExpiresAt(null);
            offerAvailablePlaces(reservation.getActivity().getId(), reservation.getActivityStart(), now);
            return ReservationResponse.from(reservation, false);
        }
        reservation.setPaymentStatus(ReservationPaymentStatus.PENDING_PAYMENT);
        reservation.setPaymentExpiresAt(now.plus(Duration.ofHours(2)));
        return ReservationResponse.from(reservation, false);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processWaitlists() {
        Instant now = Instant.now();
        List<ActivityReservation> watched = reservationRepository.findAllByPaymentStatusIn(List.of(
                ReservationPaymentStatus.WAITLIST_OFFERED, ReservationPaymentStatus.WAITLISTED,
                ReservationPaymentStatus.PENDING_PAYMENT, ReservationPaymentStatus.PAYMENT_RETRY_PENDING));
        for (ActivityReservation reservation : watched) {
            if (reservation.getPaymentStatus() == ReservationPaymentStatus.WAITLIST_OFFERED) {
                if (reservation.getPaymentExpiresAt() == null || !reservation.getPaymentExpiresAt().isAfter(now)) {
                    reservation.setPaymentStatus(ReservationPaymentStatus.CANCELLED);
                    reservation.setPaymentExpiresAt(null);
                } else if (!reservation.isWaitlistReminderSent() && reservation.getWaitlistOfferedAt() != null
                        && !reservation.getWaitlistOfferedAt().plus(Duration.ofHours(22)).isAfter(now)) {
                    reservation.setWaitlistReminderSent(true);
                    accountEmailService.waitlistOfferReminder(reservation.getContactEmail(), reservation.getContactName(), reservation.getActivity().getName());
                }
            } else if ((reservation.getPaymentStatus() == ReservationPaymentStatus.PENDING_PAYMENT
                    || reservation.getPaymentStatus() == ReservationPaymentStatus.PAYMENT_RETRY_PENDING)
                    && reservation.getPaymentExpiresAt() != null && !reservation.getPaymentExpiresAt().isAfter(now)) {
                stripeGateway.expireCheckout(reservation.getPaymentOrderId());
                reservation.setPaymentStatus(ReservationPaymentStatus.EXPIRED);
                reservation.setPaymentExpiresAt(null);
            }
        }
        Map<String, ActivityReservation> slots = new LinkedHashMap<>();
        watched.stream().filter(item -> item.getPaymentStatus() == ReservationPaymentStatus.WAITLISTED)
                .forEach(item -> slots.putIfAbsent(item.getActivity().getId() + "|" + item.getActivityStart(), item));
        for (ActivityReservation item : slots.values()) offerAvailablePlaces(item.getActivity().getId(), item.getActivityStart(), now);
    }

    private void offerAvailablePlaces(Long activityId, LocalDateTime occurrence, Instant now) {
        Activity activity = activityRepository.findByIdForUpdate(activityId).orElse(null);
        if (activity == null) return;
        long free = activity.getCapacity() - reservationRepository.countBookedParticipants(activityId, occurrence, now);
        if (free <= 0) return;
        List<ActivityReservation> queue = reservationRepository.findAllByActivity_IdAndActivityStartAndPaymentStatusOrderByReservedAtAsc(
                activityId, occurrence, ReservationPaymentStatus.WAITLISTED);
        for (ActivityReservation waiting : queue) {
            if (waiting.getParticipantCount() > free) continue;
            waiting.setPaymentStatus(ReservationPaymentStatus.WAITLIST_OFFERED);
            waiting.setWaitlistOfferedAt(now);
            waiting.setWaitlistReminderSent(false);
            waiting.setPaymentExpiresAt(now.plus(Duration.ofHours(24)));
            free -= waiting.getParticipantCount();
            accountEmailService.waitlistOffer(waiting.getContactEmail(), waiting.getContactName(), activity.getName(), occurrence, waiting.getParticipantCount());
            if (free == 0) break;
        }
    }

    @Transactional
    public ReservationResponse cancelReservation(Long touristId, Long reservationId) {
        ActivityReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la reserva."));
        if (!reservation.getTourist().getId().equals(touristId)) throw new IllegalArgumentException("Esta reserva no te pertenece.");
        if (reservation.getPaymentStatus() == ReservationPaymentStatus.CANCELLED
                || reservation.getPaymentStatus() == ReservationPaymentStatus.EXPIRED)
            throw new IllegalArgumentException("La reserva ya está cancelada o vencida.");
        if (reservation.getPaymentStatus() != ReservationPaymentStatus.WAITLISTED
                && reservation.getPaymentStatus() != ReservationPaymentStatus.WAITLIST_OFFERED
                && reservation.getActivityStart().minusMinutes(Optional.ofNullable(reservation.getActivity().getCancellationCutoffMinutes()).orElse(1440)).isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("El plazo para cancelar esta reserva ya cerró.");
        stripeGateway.expireCheckout(reservation.getPaymentOrderId());
        reservation.setPaymentStatus(ReservationPaymentStatus.CANCELLED);
        reservation.setPaymentExpiresAt(null);
        offerAvailablePlaces(reservation.getActivity().getId(), reservation.getActivityStart(), Instant.now());
        return ReservationResponse.from(reservation, false);
    }

    private void notifyReservationConfirmed(String email, String contactName, Activity activity,
                                            LocalDateTime occurrence, int participantCount, BigDecimal total) {
        String activityName = activity.getName();
        String city = activity.getCity();
        String location = activity.getLocation();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                accountEmailService.reservationConfirmed(email, contactName, activityName, city, location,
                        occurrence, participantCount, total);
            }
        });
    }

    private void validateParticipants(Activity activity, List<ReservationParticipantRequest> participants) {
        for (int index = 0; index < participants.size(); index++) {
            ReservationParticipantRequest person = participants.get(index);
            String who = "La persona " + (index + 1) + " ";
            if ((person.documentType() == DocumentType.CI && !person.documentNumber().matches("\\d{8}"))
                    || (person.documentType() == DocumentType.PASSPORT && !person.documentNumber().matches("[A-Za-z0-9]{6,9}")))
                throw new IllegalArgumentException(who + "debe ingresar un documento válido.");
            if (activity.getMinimumAge() != null && person.age() < activity.getMinimumAge())
                throw new IllegalArgumentException(who + "no cumple la edad mínima de " + activity.getMinimumAge() + " años.");
            if (activity.getMaximumAge() != null && person.age() > activity.getMaximumAge())
                throw new IllegalArgumentException(who + "supera la edad máxima de " + activity.getMaximumAge() + " años.");
            if (activity.getMinimumHeightCm() != null && activity.getMinimumHeightCm() > 0
                    && (person.heightCm() == null || person.heightCm() < activity.getMinimumHeightCm()))
                throw new IllegalArgumentException(who + "no cumple la altura mínima de " + activity.getMinimumHeightCm() + " cm.");
            if (activity.getMaximumHeightCm() != null
                    && (person.heightCm() == null || person.heightCm() > activity.getMaximumHeightCm()))
                throw new IllegalArgumentException(who + "supera la altura máxima de " + activity.getMaximumHeightCm() + " cm.");
            if (activity.getDifficulty() != null && !activity.getDifficulty().isBlank()) {
                if (person.level() == null || level(person.level()) < level(activity.getDifficulty()))
                    throw new IllegalArgumentException(who + "no tiene el nivel requerido (" + activity.getDifficulty() + ").");
            }
        }
    }

    private int level(String value) {
        return switch (value) { case "Principiante" -> 1; case "Intermedio" -> 2; case "Avanzado" -> 3; default -> 0; };
    }

    @Transactional
    public List<ReservationResponse> reservations(Long touristId) {
        Instant now = Instant.now();
        List<ActivityReservation> reservations = reservationRepository.findAllByTourist_IdOrderByActivityStartDesc(touristId);
        reservations.stream().filter(reservation -> (reservation.getPaymentStatus() == ReservationPaymentStatus.PENDING_PAYMENT
                        || reservation.getPaymentStatus() == ReservationPaymentStatus.PAYMENT_RETRY_PENDING)
                        && reservation.getPaymentExpiresAt() != null && !reservation.getPaymentExpiresAt().isAfter(now))
                .forEach(reservation -> { stripeGateway.expireCheckout(reservation.getPaymentOrderId()); reservation.setPaymentStatus(ReservationPaymentStatus.EXPIRED); reservation.setPaymentExpiresAt(null); });
        return reservations.stream()
                .map(reservation -> ReservationResponse.from(reservation,
                        reviewRepository.findByReservation_Id(reservation.getId()).isPresent())).toList();
    }

    @Transactional
    public ReservationResponse rescheduleReservation(Long touristId, Long reservationId, LocalDateTime newStart) {
        ActivityReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la reserva."));
        if (!reservation.getTourist().getId().equals(touristId)) throw new IllegalArgumentException("Esta reserva no te pertenece.");
        if (reservation.getPaymentStatus() != ReservationPaymentStatus.APPROVED)
            throw new IllegalArgumentException("Solo podés modificar una reserva confirmada.");
        if (!newStart.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("Elegí una fecha futura.");
        Activity activity = activityRepository.findByIdForUpdate(reservation.getActivity().getId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la actividad."));
        if (!isOccurrence(activity, newStart)) throw new IllegalArgumentException("Ese horario no pertenece a la actividad.");
        if (newStart.minusMinutes(activity.getBookingCutoffMinutes()).isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("El plazo para cambiar esta reserva ya cerró.");
        if (!newStart.equals(reservation.getActivityStart())) {
            long reserved = reservationRepository.countBookedParticipants(activity.getId(), newStart, Instant.now());
            if (reserved + reservation.getParticipantCount() > activity.getCapacity())
                throw new IllegalArgumentException("No quedan suficientes cupos en ese horario.");
            if (reservationRepository.existsByActivity_IdAndTourist_IdAndActivityStart(activity.getId(), touristId, newStart))
                throw new IllegalArgumentException("Ya tenés una reserva para ese horario.");
            reservation.setActivityStart(newStart);
        }
        return ReservationResponse.from(reservation, reviewRepository.findByReservation_Id(reservationId).isPresent());
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
        if (reservation.getPaymentStatus() != ReservationPaymentStatus.APPROVED) {
            throw new IllegalArgumentException("Solo podés opinar sobre una reserva confirmada y pagada.");
        }
        if (reviewRepository.findByReservation_Id(reservationId).isPresent()) {
            throw new IllegalArgumentException("Ya dejaste una opinión para esta reserva.");
        }
        if (request.rating().remainder(new BigDecimal("0.5")).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("La puntuación debe avanzar de media estrella en media estrella.");
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
        if (activity.isArchived()) throw new IllegalArgumentException("Esta actividad ya fue eliminada.");
        return activity;
    }

    private boolean hasPendingPayment(Long activityId) {
        return reservationRepository.findAllByActivity_IdOrderByActivityStartAsc(activityId).stream().anyMatch(this::paymentIsPending);
    }

    private boolean paymentIsPending(ActivityReservation reservation) {
        ReservationPaymentStatus status = reservation.getPaymentStatus();
        return (status == ReservationPaymentStatus.PENDING_PAYMENT || status == ReservationPaymentStatus.PAYMENT_RETRY_PENDING)
                && (reservation.getPaymentExpiresAt() == null || reservation.getPaymentExpiresAt().isAfter(Instant.now()));
    }

    private void validate(ActivityRequest request) {
        if (request.cancellationCutoffMinutes() <= request.bookingCutoffMinutes()) {
            throw new IllegalArgumentException("La fecha límite de cancelación debe ser anterior al cierre de reservas.");
        }
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
        activity.setBookingCutoffMinutes(request.bookingCutoffMinutes());
        activity.setCancellationCutoffMinutes(request.cancellationCutoffMinutes());
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
        double average = reviews.isEmpty() ? 0 : reviews.stream().mapToDouble(review -> review.getRating().doubleValue()).average().orElse(0);
        long recentReservations = reservationRepository.countByActivity_IdAndReservedAtAfter(activity.getId(), Instant.now().minus(30, ChronoUnit.DAYS));
        return ActivityResponse.from(activity, upcoming(activity), average, reviews.size(), recentReservations);
    }

    private List<LocalDateTime> upcoming(Activity activity) {
        if (activity.isArchived() || !activity.isAcceptingReservations()) return List.of();
        LocalDate today = LocalDate.now();
        LocalDate last = activity.getRepeatUntil() == null ? today.plusDays(90)
                : activity.getRepeatUntil().isBefore(today.plusDays(90)) ? activity.getRepeatUntil() : today.plusDays(90);
        LocalDate begin = activity.getFirstDate().isAfter(today) ? activity.getFirstDate() : today;
        if (activity.getRecurrence() == ActivityRecurrence.ONCE) last = activity.getFirstDate();
        List<LocalDateTime> result = new ArrayList<>();
        LocalDateTime bookableAfter = LocalDateTime.now().plusMinutes(activity.getBookingCutoffMinutes());
        for (LocalDate date = begin; !date.isAfter(last) && result.size() < 12; date = date.plusDays(1)) {
            if (occursOn(activity, date)) {
                for (LocalTime time : activity.getStartTimes()) {
                    LocalDateTime occurrence = date.atTime(time);
                    if (occurrence.isAfter(bookableAfter)) result.add(occurrence);
                    if (result.size() >= 12) break;
                }
            }
        }
        return result;
    }

    private boolean isOccurrence(Activity activity, LocalDateTime dateTime) {
        return activity.getStartTimes().contains(dateTime.toLocalTime()) && occursOn(activity, dateTime.toLocalDate());
    }

    // Público para que la API pública pueda saber qué actividades se hacen un día dado.
    public boolean occursOn(Activity activity, LocalDate date) {
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
