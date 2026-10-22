package uy.edu.um.proyTic2026.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyTic2026.entities.ActivityReservation;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;

public interface ActivityReservationRepository extends JpaRepository<ActivityReservation, Long> {
    long countByActivity_IdAndActivityStart(Long activityId, LocalDateTime activityStart);
    long countByActivity_IdAndReservedAtAfter(Long activityId, Instant reservedAfter);
    boolean existsByActivity_Id(Long activityId);
    boolean existsByActivity_IdAndTourist_IdAndActivityStart(Long activityId, Long touristId, LocalDateTime activityStart);
    List<ActivityReservation> findAllByTourist_IdOrderByActivityStartDesc(Long touristId);
    List<ActivityReservation> findAllByActivity_IdOrderByActivityStartAsc(Long activityId);
}
