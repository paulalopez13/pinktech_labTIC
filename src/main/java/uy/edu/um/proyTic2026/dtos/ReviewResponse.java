package uy.edu.um.proyTic2026.dtos;

import uy.edu.um.proyTic2026.entities.ActivityReview;
import java.time.Instant;
import java.math.BigDecimal;

public record ReviewResponse(Long id, String username, BigDecimal rating, String comment, Instant createdAt) {
    public static ReviewResponse from(ActivityReview review) {
        return new ReviewResponse(review.getId(), review.getReservation().getTourist().getUsername(),
                review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
