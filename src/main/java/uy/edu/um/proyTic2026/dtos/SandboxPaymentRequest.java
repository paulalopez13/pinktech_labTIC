package uy.edu.um.proyTic2026.dtos;

import jakarta.validation.constraints.NotNull;

public record SandboxPaymentRequest(@NotNull Boolean approved) { }
