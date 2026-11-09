package uy.edu.um.proyTic2026.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.edu.um.proyTic2026.entities.ActivityReservation;
import uy.edu.um.proyTic2026.entities.ReservationPaymentStatus;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ActivityReservationRepository extends JpaRepository<ActivityReservation, Long> {
    long countByActivity_IdAndActivityStart(Long activityId, LocalDateTime activityStart);
    @Query("select coalesce(sum(coalesce(r.participantCount, 1)), 0) from ActivityReservation r where r.activity.id = :activityId and r.activityStart = :activityStart and (r.paymentStatus is null or r.paymentStatus = uy.edu.um.proyTic2026.entities.ReservationPaymentStatus.APPROVED or ((r.paymentStatus = uy.edu.um.proyTic2026.entities.ReservationPaymentStatus.PENDING_PAYMENT or r.paymentStatus = uy.edu.um.proyTic2026.entities.ReservationPaymentStatus.PAYMENT_RETRY_PENDING or r.paymentStatus = uy.edu.um.proyTic2026.entities.ReservationPaymentStatus.WAITLIST_OFFERED) and r.paymentExpiresAt > :now))")
    long countBookedParticipants(@Param("activityId") Long activityId, @Param("activityStart") LocalDateTime activityStart,
                                 @Param("now") Instant now);
    @Query("select r.activityStart, sum(coalesce(r.participantCount, 1)) from ActivityReservation r "
            + "where r.activity.id = :activityId and r.activityStart between :from and :to "
            + "and (r.paymentStatus is null or r.paymentStatus = uy.edu.um.proyTic2026.entities.ReservationPaymentStatus.APPROVED "
            + "or (r.paymentStatus in :pendingStatuses and r.paymentExpiresAt > :now)) group by r.activityStart")
    List<Object[]> sumBookedParticipantsByActivityAndStartBetween(@Param("activityId") Long activityId,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
            @Param("now") Instant now, @Param("pendingStatuses") List<ReservationPaymentStatus> pendingStatuses);
    long countByActivity_IdAndReservedAtAfter(Long activityId, Instant reservedAfter);
    boolean existsByActivity_Id(Long activityId);
    boolean existsByActivity_IdAndTourist_IdAndActivityStart(Long activityId, Long touristId, LocalDateTime activityStart);
    Optional<ActivityReservation> findByActivity_IdAndTourist_IdAndActivityStart(Long activityId, Long touristId, LocalDateTime activityStart);
    boolean existsByActivity_IdAndTourist_Id(Long activityId, Long touristId);
    List<ActivityReservation> findAllByTourist_IdOrderByActivityStartDesc(Long touristId);
    List<ActivityReservation> findAllByActivity_IdOrderByActivityStartAsc(Long activityId);
    // Todas las reservas de las actividades de un prestador, de la más próxima a la más lejana.
    List<ActivityReservation> findAllByActivity_Provider_IdOrderByActivityStartAsc(Long providerId);
    List<ActivityReservation> findAllByPaymentStatusIn(List<uy.edu.um.proyTic2026.entities.ReservationPaymentStatus> statuses);
    List<ActivityReservation> findAllByActivity_IdAndActivityStartAndPaymentStatusOrderByReservedAtAsc(Long activityId, LocalDateTime activityStart, uy.edu.um.proyTic2026.entities.ReservationPaymentStatus status);
    Optional<ActivityReservation> findByPaymentOrderId(String paymentOrderId);
}
