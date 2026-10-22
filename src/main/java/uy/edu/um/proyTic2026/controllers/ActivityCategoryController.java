package uy.edu.um.proyTic2026.controllers;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.dtos.ActivityCategoryRequest;
import uy.edu.um.proyTic2026.entities.Administrator;
import uy.edu.um.proyTic2026.repositories.UserRepository;
import uy.edu.um.proyTic2026.services.ActivityCategoryService;

import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class ActivityCategoryController {
    private final ActivityCategoryService categoryService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(categoryService.list());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ActivityCategoryRequest request, HttpSession session) {
        if (!isAdministrator(session)) return forbidden();
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> rename(@PathVariable Long id, @Valid @RequestBody ActivityCategoryRequest request,
                                   HttpSession session) {
        if (!isAdministrator(session)) return forbidden();
        try {
            return ResponseEntity.ok(categoryService.rename(id, request));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        if (!isAdministrator(session)) return forbidden();
        try {
            categoryService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    private boolean isAdministrator(HttpSession session) {
        Object id = session.getAttribute("userId");
        return id instanceof Long userId && userRepository.findById(userId).orElse(null) instanceof Administrator;
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Solo un administrador puede gestionar las categorías."));
    }
}
