package uy.edu.um.proyTic2026.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.edu.um.proyTic2026.entities.ActivityReview;

import java.util.List;
import java.util.Optional;

public interface ActivityReviewRepository extends JpaRepository<ActivityReview, Long> {
    List<ActivityReview> findAllByReservation_Activity_IdOrderByCreatedAtDesc(Long activityId);
    Optional<ActivityReview> findByReservation_Id(Long reservationId);

    @Query("select avg(review.rating) from ActivityReview review where review.reservation.activity.provider.id = :providerId")
    Double findAverageRatingByProviderId(@Param("providerId") Long providerId);

    @Query("select count(review) from ActivityReview review where review.reservation.activity.provider.id = :providerId")
    long countByProviderId(@Param("providerId") Long providerId);
}
