package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewRequest(@Min(1) @Max(5) int rating, @NotBlank @Size(max = 1500) String comment) { }
