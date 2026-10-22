package uy.edu.um.proyTic2026.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false, columnDefinition = "integer default 1")
    private Integer participantCount = 1;

    @Column(length = 100)
    private String contactName;

    @Column(length = 100)
    private String contactEmail;

    @Column(length = 30)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24, columnDefinition = "varchar(24) default 'APPROVED'")
    private ReservationPaymentStatus paymentStatus = ReservationPaymentStatus.APPROVED;

    @Column(length = 64)
    private String paymentOrderId;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationParticipant> participants = new ArrayList<>();
}
