package uy.edu.um.proyTic2026.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String department;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false, length = 180)
    private String location;

    @Column(nullable = false, length = 3000)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "activity_category_links",
            joinColumns = @JoinColumn(name = "activity_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<ActivityCategory> categories = new HashSet<>();

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ActivityPhoto> photos = new ArrayList<>();

    @Column(nullable = false)
    private LocalDate firstDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityRecurrence recurrence = ActivityRecurrence.ONCE;

    private LocalDate repeatUntil;

    @ElementCollection
    @CollectionTable(name = "activity_start_times", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "start_time", nullable = false)
    @OrderColumn(name = "time_order")
    private List<LocalTime> startTimes = new ArrayList<>();

    @ElementCollection(targetClass = DayOfWeek.class)
    @CollectionTable(name = "activity_weekdays", joinColumns = @JoinColumn(name = "activity_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "weekday", nullable = false, length = 10)
    private Set<DayOfWeek> weekdays = new HashSet<>();

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private Integer bookingCutoffHours = 0;

    @Column(length = 500)
    private String equipment;

    private Integer minimumAge;
    private Integer maximumAge;
    private Integer minimumHeightCm;
    private Integer maximumHeightCm;

    @Column(length = 30)
    private String difficulty;

    @Column(length = 1000)
    private String otherRequirements;
}
