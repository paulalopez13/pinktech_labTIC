package uy.edu.um.proyTic2026.dtos;

// registered: el documento existe en la plataforma.
// verified: además fue aprobado por un administrador.
// El nombre y el id solo se muestran si está verificado, para no exponer solicitudes pendientes o rechazadas.

public record ProviderVerificationResponse(String document, boolean registered, boolean verified,
                                           Long providerId, String name, String providerType) { }
