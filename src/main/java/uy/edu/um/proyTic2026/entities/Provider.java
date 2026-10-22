package uy.edu.um.proyTic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


@Entity
@Table(name = "providers")
@Getter
@Setter
@NoArgsConstructor

public abstract class Provider extends User {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProviderStatus status = ProviderStatus.PENDING;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String phone;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String department;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String city;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String address;

    @Size(max = 250)
    @Column(length = 250)
    private String website;

    @Size(max = 100)
    @Column(length = 100)
    private String instagram;

    @Size(max = 100)
    @Column(length = 100)
    private String twitter;

    @Size(max = 30)
    @Column(length = 30)
    private String whatsapp;

    @Size(max = 100)
    @Column(length = 100)
    private String youtube;

    @Size(max = 100)
    @Column(length = 100)
    private String facebook;

}
