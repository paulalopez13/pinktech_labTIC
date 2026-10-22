package uy.edu.um.proyTic2026.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reservation_participants")
@Getter
@Setter
@NoArgsConstructor
public class ReservationParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private ActivityReservation reservation;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private DocumentType documentType = DocumentType.CI;

    @Column(length = 9)
    private String documentNumber;

    @Column(nullable = false)
    private Integer age;

    private Integer heightCm;

    @Column(length = 20)
    private String level;
}
