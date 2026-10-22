package uy.edu.um.proyTic2026.dtos;

import uy.edu.um.proyTic2026.entities.ActivityCategory;

public record ActivityCategoryResponse(Long id, String name) {
    public static ActivityCategoryResponse from(ActivityCategory category) {
        return new ActivityCategoryResponse(category.getId(), category.getName());
    }
}
