package uy.edu.um.proyTic2026.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.edu.um.proyTic2026.entities.ActivityReservation;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;

public interface ActivityReservationRepository extends JpaRepository<ActivityReservation, Long> {
    long countByActivity_IdAndActivityStart(Long activityId, LocalDateTime activityStart);
    @Query("select coalesce(sum(coalesce(r.participantCount, 1)), 0) from ActivityReservation r where r.activity.id = :activityId and r.activityStart = :activityStart and (r.paymentStatus is null or r.paymentStatus = uy.edu.um.proyTic2026.entities.ReservationPaymentStatus.APPROVED or (r.paymentStatus = uy.edu.um.proyTic2026.entities.ReservationPaymentStatus.PENDING_PAYMENT and r.reservedAt >= :pendingAfter))")
    long countBookedParticipants(@Param("activityId") Long activityId, @Param("activityStart") LocalDateTime activityStart,
                                 @Param("pendingAfter") Instant pendingAfter);
    long countByActivity_IdAndReservedAtAfter(Long activityId, Instant reservedAfter);
    boolean existsByActivity_Id(Long activityId);
    boolean existsByActivity_IdAndTourist_IdAndActivityStart(Long activityId, Long touristId, LocalDateTime activityStart);
    boolean existsByActivity_IdAndTourist_Id(Long activityId, Long touristId);
    List<ActivityReservation> findAllByTourist_IdOrderByActivityStartDesc(Long touristId);
    List<ActivityReservation> findAllByActivity_IdOrderByActivityStartAsc(Long activityId);
}
