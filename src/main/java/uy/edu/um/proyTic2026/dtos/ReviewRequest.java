package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewRequest(@DecimalMin("0.5") @DecimalMax("5.0") @Digits(integer = 1, fraction = 1) java.math.BigDecimal rating,
                            @NotBlank @Size(max = 1500) String comment) { }
