package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivityCategoryRequest(@NotBlank @Size(max = 60) String name) { }
