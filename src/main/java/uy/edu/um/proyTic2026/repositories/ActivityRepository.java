package uy.edu.um.proyTic2026.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.edu.um.proyTic2026.entities.Activity;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findAllByOrderByNameAsc();
    List<Activity> findAllByProvider_IdOrderByNameAsc(Long providerId);
    List<Activity> findAllByCategories_Id(Long categoryId);
    boolean existsByNameIgnoreCase(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Activity a where a.id = :id")
    Optional<Activity> findByIdForUpdate(@Param("id") Long id);
}
