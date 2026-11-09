package uy.edu.um.proyTic2026.dtos;

import java.time.LocalDateTime;

public record ActivityAvailabilityResponse(LocalDateTime activityStart, int remaining) { }
