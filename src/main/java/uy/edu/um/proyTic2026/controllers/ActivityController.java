package uy.edu.um.proyTic2026.controllers;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.dtos.ActivityRequest;
import uy.edu.um.proyTic2026.entities.Provider;
import uy.edu.um.proyTic2026.entities.ProviderStatus;
import uy.edu.um.proyTic2026.repositories.UserRepository;
import uy.edu.um.proyTic2026.services.ActivityService;

import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class ActivityController {
    private final ActivityService activityService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> list() { return ResponseEntity.ok(activityService.list()); }

    @GetMapping("/mine")
    public ResponseEntity<?> mine(HttpSession session) {
        Provider provider = provider(session, true);
        return provider == null ? forbidden() : ResponseEntity.ok(activityService.listForProvider(provider.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) { return run(() -> activityService.get(id)); }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<?> reviews(@PathVariable Long id) { return run(() -> activityService.reviews(id)); }

    @GetMapping("/{id}/availability")
    public ResponseEntity<?> availability(@PathVariable Long id, @RequestParam LocalDateTime activityStart) {
        return run(() -> Map.of("remaining", activityService.remainingCapacity(id, activityStart)));
    }

    @GetMapping("/providers/{providerId}")
    public ResponseEntity<?> publicProvider(@PathVariable Long providerId) {
        return run(() -> activityService.publicProvider(providerId));
    }

    @GetMapping("/{id}/photos/{photoId}")
    public ResponseEntity<?> photo(@PathVariable Long id, @PathVariable Long photoId) {
        try {
            var photo = activityService.photo(id, photoId);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(photo.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + photo.getFileName().replace("\"", "") + "\"")
                    .body(photo.getContent());
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(@Valid @RequestPart("activity") ActivityRequest request,
                                    @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
                                    HttpSession session) {
        Provider provider = provider(session, true);
        if (provider == null) return forbidden();
        return run(() -> activityService.create(provider.getId(), request, photos));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestPart("activity") ActivityRequest request,
                                    @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
                                    @RequestParam(value = "keepPhotoIds", required = false) List<Long> keepPhotoIds,
                                    HttpSession session) {
        Provider provider = provider(session, true);
        if (provider == null) return forbidden();
        return run(() -> activityService.update(provider.getId(), id, request, photos, keepPhotoIds));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        Provider provider = provider(session, true);
        if (provider == null) return forbidden();
        try {
            activityService.delete(provider.getId(), id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    private Provider provider(HttpSession session, boolean mustBeApproved) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long userId)) return null;
        Object user = userRepository.findById(userId).orElse(null);
        if (!(user instanceof Provider provider)) return null;
        if (mustBeApproved && provider.getStatus() != ProviderStatus.APPROVED) return null;
        return provider;
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Solo un prestador aprobado puede gestionar actividades."));
    }

    private <T> ResponseEntity<?> run(java.util.function.Supplier<T> action) {
        try { return ResponseEntity.ok(action.get()); }
        catch (IllegalArgumentException error) { return ResponseEntity.badRequest().body(Map.of("message", error.getMessage())); }
    }
}
