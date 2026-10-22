package uy.edu.um.proyTic2026.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyTic2026.entities.ActivityCategory;

import java.util.List;

public interface ActivityCategoryRepository extends JpaRepository<ActivityCategory, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    List<ActivityCategory> findAllByOrderByNameAsc();
}
