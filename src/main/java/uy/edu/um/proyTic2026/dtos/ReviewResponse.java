package uy.edu.um.proyTic2026.dtos;

import uy.edu.um.proyTic2026.entities.ActivityReview;
import java.time.Instant;

public record ReviewResponse(Long id, String username, int rating, String comment, Instant createdAt) {
    public static ReviewResponse from(ActivityReview review) {
        return new ReviewResponse(review.getId(), review.getReservation().getTourist().getUsername(),
                review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
