package uy.edu.um.proyTic2026.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "activity_photos")
@Getter
@Setter
@NoArgsConstructor
public class ActivityPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 40)
    private String contentType;

    @Lob
    @Column(nullable = false)
    private byte[] content;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
