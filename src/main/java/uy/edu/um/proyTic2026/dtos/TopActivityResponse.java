package uy.edu.um.proyTic2026.dtos;

// Una actividad del ranking mensual, con el promedio y la cantidad de opiniones de ese mes.
public record TopActivityResponse(int position, double monthAverageRating, long monthReviewCount,
                                  ActivityResponse activity) { }
