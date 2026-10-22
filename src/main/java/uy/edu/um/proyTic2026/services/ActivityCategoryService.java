package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.ActivityCategoryRequest;
import uy.edu.um.proyTic2026.dtos.ActivityCategoryResponse;
import uy.edu.um.proyTic2026.entities.ActivityCategory;
import uy.edu.um.proyTic2026.entities.Activity;
import uy.edu.um.proyTic2026.repositories.ActivityRepository;
import uy.edu.um.proyTic2026.repositories.ActivityCategoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityCategoryService {
    private final ActivityCategoryRepository categoryRepository;
    private final ActivityRepository activityRepository;

    @Transactional(readOnly = true)
    public List<ActivityCategoryResponse> list() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(ActivityCategoryResponse::from).toList();
    }

    @Transactional
    public ActivityCategoryResponse create(ActivityCategoryRequest request) {
        String name = normalizedName(request.name());
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre.");
        }
        ActivityCategory category = new ActivityCategory();
        category.setName(name);
        return ActivityCategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public ActivityCategoryResponse rename(Long id, ActivityCategoryRequest request) {
        ActivityCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró esa categoría."));
        String name = normalizedName(request.name());
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre.");
        }
        category.setName(name);
        return ActivityCategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        ActivityCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró esa categoría."));
        List<Activity> linkedActivities = activityRepository.findAllByCategories_Id(id);
        linkedActivities.forEach(activity -> activity.getCategories().remove(category));
        activityRepository.saveAll(linkedActivities);
        activityRepository.flush();
        categoryRepository.delete(category);
    }

    private String normalizedName(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }
}
