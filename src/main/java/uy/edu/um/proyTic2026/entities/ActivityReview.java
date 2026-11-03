package uy.edu.um.proyTic2026.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.math.BigDecimal;

@Entity
@Table(name = "activity_reviews", uniqueConstraints =
        @UniqueConstraint(name = "uq_review_per_reservation", columnNames = "reservation_id"))
@Getter
@Setter
@NoArgsConstructor
public class ActivityReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private ActivityReservation reservation;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(nullable = false, length = 1500)
    private String comment;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
