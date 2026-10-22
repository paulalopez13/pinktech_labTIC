package uy.edu.um.proyTic2026.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "activity_favorites", uniqueConstraints =
        @UniqueConstraint(name = "uq_tourist_activity_favorite", columnNames = {"tourist_id", "activity_id"}))
@Getter @Setter @NoArgsConstructor
public class ActivityFavorite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tourist_id", nullable = false)
    private Tourist tourist;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
