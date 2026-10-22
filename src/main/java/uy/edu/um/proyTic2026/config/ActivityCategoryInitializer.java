package uy.edu.um.proyTic2026.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uy.edu.um.proyTic2026.entities.ActivityCategory;
import uy.edu.um.proyTic2026.repositories.ActivityCategoryRepository;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ActivityCategoryInitializer {
    private final ActivityCategoryRepository categoryRepository;

    @Bean
    ApplicationRunner seedActivityCategories() {
        return args -> {
            if (categoryRepository.count() == 0) {
                List<String> names = List.of(
                        "Historia", "Deportes acuáticos", "Familiar", "Tranquilo",
                        "Gastronomía", "Aventura", "Naturaleza", "Cultural");
                categoryRepository.saveAll(names.stream().map(name -> {
                    ActivityCategory category = new ActivityCategory();
                    category.setName(name);
                    return category;
                }).toList());
            }
        };
    }
}
