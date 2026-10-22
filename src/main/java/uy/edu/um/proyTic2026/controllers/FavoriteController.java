package uy.edu.um.proyTic2026.controllers;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.entities.Tourist;
import uy.edu.um.proyTic2026.repositories.UserRepository;
import uy.edu.um.proyTic2026.services.FavoriteService;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class FavoriteController {
    private final FavoriteService favoriteService;
    private final UserRepository users;

    @GetMapping("/activities/ids")
    public ResponseEntity<?> activityIds(HttpSession session) {
        Tourist tourist = tourist(session);
        return tourist == null ? forbidden() : ResponseEntity.ok(favoriteService.activityIds(tourist.getId()));
    }
    @GetMapping("/activities")
    public ResponseEntity<?> activities(HttpSession session) {
        Tourist tourist = tourist(session);
        return tourist == null ? forbidden() : ResponseEntity.ok(favoriteService.activities(tourist.getId()));
    }
    @PostMapping("/activities/{activityId}")
    public ResponseEntity<?> addActivity(@PathVariable Long activityId, HttpSession session) {
        Tourist tourist = tourist(session);
        if (tourist == null) return forbidden();
        try { favoriteService.addActivity(tourist.getId(), activityId); return ResponseEntity.noContent().build(); }
        catch (IllegalArgumentException error) { return ResponseEntity.badRequest().body(Map.of("message", error.getMessage())); }
    }
    @DeleteMapping("/activities/{activityId}")
    public ResponseEntity<?> removeActivity(@PathVariable Long activityId, HttpSession session) {
        Tourist tourist = tourist(session);
        if (tourist == null) return forbidden();
        favoriteService.removeActivity(tourist.getId(), activityId); return ResponseEntity.noContent().build();
    }
    private Tourist tourist(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long userId)) return null;
        return users.findById(userId).filter(Tourist.class::isInstance).map(Tourist.class::cast).orElse(null);
    }
    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Iniciá sesión como turista para consultar tus favoritos."));
    }
}
