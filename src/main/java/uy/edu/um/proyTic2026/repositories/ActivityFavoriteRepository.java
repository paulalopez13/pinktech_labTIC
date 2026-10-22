package uy.edu.um.proyTic2026.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyTic2026.entities.ActivityFavorite;
import java.util.List;

public interface ActivityFavoriteRepository extends JpaRepository<ActivityFavorite, Long> {
    boolean existsByTourist_IdAndActivity_Id(Long touristId, Long activityId);
    List<ActivityFavorite> findAllByTourist_IdOrderByCreatedAtDesc(Long touristId);
    void deleteByTourist_IdAndActivity_Id(Long touristId, Long activityId);
}
