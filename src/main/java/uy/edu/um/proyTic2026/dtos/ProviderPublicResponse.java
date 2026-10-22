package uy.edu.um.proyTic2026.dtos;

public record ProviderPublicResponse(Long id, String displayName, String providerType, String email, String phone,
                                     String department, String city, String address, String website, String instagram,
                                     String twitter, String whatsapp, String youtube, String facebook,
                                     double averageRating, long reviewCount) { }
