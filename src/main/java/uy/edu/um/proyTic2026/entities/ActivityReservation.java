package uy.edu.um.proyTic2026.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.Instant;

@Entity
@Table(name = "activity_reservations", uniqueConstraints =
        @UniqueConstraint(name = "uq_activity_reservation_person_occurrence", columnNames = {"activity_id", "tourist_id", "activity_start"}))
@Getter
@Setter
@NoArgsConstructor
public class ActivityReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tourist_id", nullable = false)
    private Tourist tourist;

    @Column(name = "activity_start", nullable = false)
    private LocalDateTime activityStart;

    @Column(nullable = false)
    private Instant reservedAt = Instant.now();
}
