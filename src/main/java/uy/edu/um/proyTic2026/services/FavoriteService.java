package uy.edu.um.proyTic2026.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.dtos.ActivityResponse;
import uy.edu.um.proyTic2026.entities.*;
import uy.edu.um.proyTic2026.repositories.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final ActivityFavoriteRepository activityFavorites;
    private final ActivityRepository activities;
    private final UserRepository users;
    private final ActivityService activityService;

    @Transactional(readOnly = true)
    public List<Long> activityIds(Long touristId) {
        return activityFavorites.findAllByTourist_IdOrderByCreatedAtDesc(touristId).stream()
                .map(favorite -> favorite.getActivity().getId()).toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> activities(Long touristId) {
        return activityFavorites.findAllByTourist_IdOrderByCreatedAtDesc(touristId).stream()
                .map(favorite -> activityService.get(favorite.getActivity().getId())).toList();
    }

    @Transactional
    public void addActivity(Long touristId, Long activityId) {
        if (activityFavorites.existsByTourist_IdAndActivity_Id(touristId, activityId)) return;
        Tourist tourist = tourist(touristId);
        Activity activity = activities.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la actividad."));
        ActivityFavorite favorite = new ActivityFavorite();
        favorite.setTourist(tourist); favorite.setActivity(activity);
        activityFavorites.save(favorite);
    }

    @Transactional
    public void removeActivity(Long touristId, Long activityId) {
        activityFavorites.deleteByTourist_IdAndActivity_Id(touristId, activityId);
    }

    private Tourist tourist(Long id) {
        return users.findById(id).filter(Tourist.class::isInstance).map(Tourist.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("Iniciá sesión como turista para guardar favoritos."));
    }
}
