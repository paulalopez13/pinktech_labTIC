package uy.edu.um.proyTic2026.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyTic2026.services.PublicApiService;

import java.time.LocalDate;
import java.time.YearMonth;

// API abierta para que otras plataformas consulten la oferta de Uruguay Mnatural.
// Solo tiene consultas (GET), no necesita sesión y acepta pedidos desde cualquier origen.
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicApiController {
    private final PublicApiService publicApiService;

    // GET /api/public/providers
    @GetMapping("/providers")
    public ResponseEntity<?> providers() {
        return ResponseEntity.ok(publicApiService.providers());
    }

    // GET /api/public/providers/verify?document=44444444
    // Acepta un RUT (empresa) o una cédula (prestador particular).
    @GetMapping("/providers/verify")
    public ResponseEntity<?> verify(@RequestParam String document) {
        return ResponseEntity.ok(publicApiService.verify(document));
    }

    // GET /api/public/activities?department=Maldonado&city=Punta del Este&date=2026-11-02
    // La localidad es opcional. Si no se manda la fecha, se usa la de hoy.
    @GetMapping("/activities")
    public ResponseEntity<?> activitiesOn(@RequestParam String department,
                                          @RequestParam(required = false) String city,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(publicApiService.activitiesOn(date == null ? LocalDate.now() : date, department, city));
    }

    // GET /api/public/activities/top?department=Maldonado&month=2026-10
    // Si no se manda el mes, se usa el actual.
    @GetMapping("/activities/top")
    public ResponseEntity<?> topRated(@RequestParam String department,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(publicApiService.topRated(department, month == null ? YearMonth.now() : month));
    }

    // GET /api/public/activities/search?q=ballenas
    @GetMapping("/activities/search")
    public ResponseEntity<?> search(@RequestParam("q") String keyword) {
        return ResponseEntity.ok(publicApiService.search(keyword));
    }
}
