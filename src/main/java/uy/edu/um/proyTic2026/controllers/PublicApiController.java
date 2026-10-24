package uy.edu.um.proyTic2026.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "API pública", description = "Consultas abiertas para otras plataformas. No requieren iniciar sesión.")
public class PublicApiController {
    private final PublicApiService publicApiService;

    // GET /api/public/providers
    @Operation(summary = "Listado de emprendimientos", description = "Devuelve todos los emprendimientos aprobados por un administrador.")
    @GetMapping("/providers")
    public ResponseEntity<?> providers() {
        return ResponseEntity.ok(publicApiService.providers());
    }

    // GET /api/public/providers/verify?document=44444444
    // Acepta un RUT (empresa) o una cédula (prestador particular).
    @Operation(summary = "Verificar emprendimiento", description = "Indica si un RUT (empresa) o una cédula (prestador particular) está registrado y aprobado. El nombre solo se muestra si está aprobado.")
    @GetMapping("/providers/verify")
    public ResponseEntity<?> verify(@Parameter(description = "RUT o cédula", example = "44444444") @RequestParam String document) {
        return ResponseEntity.ok(publicApiService.verify(document));
    }

    // GET /api/public/activities?department=Maldonado&city=Punta del Este&date=2026-11-02
    // La localidad es opcional. Si no se manda la fecha, se usa la de hoy.
    @Operation(summary = "Actividades de un día", description = "Actividades con salidas en la fecha indicada, en un departamento y opcionalmente una localidad. No distingue mayúsculas ni tildes.")
    @GetMapping("/activities")
    public ResponseEntity<?> activitiesOn(@Parameter(example = "Maldonado") @RequestParam String department,
                                          @Parameter(description = "Localidad (opcional)") @RequestParam(required = false) String city,
                                          @Parameter(description = "Fecha AAAA-MM-DD (opcional, por defecto hoy)", example = "2026-11-02")
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(publicApiService.activitiesOn(date == null ? LocalDate.now() : date, department, city));
    }

    // GET /api/public/activities/top?department=Maldonado&month=2026-10
    // Si no se manda el mes, se usa el actual.
    @Operation(summary = "Top 3 del mes", description = "Las 3 actividades mejor puntuadas de un departamento, contando las salidas realizadas en el mes indicado. Si empatan en promedio, gana la que tiene más opiniones.")
    @GetMapping("/activities/top")
    public ResponseEntity<?> topRated(@Parameter(example = "Maldonado") @RequestParam String department,
                                      @Parameter(description = "Mes AAAA-MM (opcional, por defecto el actual)", example = "2026-10")
                                      @RequestParam(required = false)
                                      @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(publicApiService.topRated(department, month == null ? YearMonth.now() : month));
    }

    // GET /api/public/activities/search?q=ballenas
    @Operation(summary = "Buscar actividades", description = "Busca la palabra en el nombre, la descripción, la ciudad, el departamento y las categorías. No distingue mayúsculas ni tildes.")
    @GetMapping("/activities/search")
    public ResponseEntity<?> search(@Parameter(description = "Palabra clave (al menos 2 letras)", example = "ballenas") @RequestParam("q") String keyword) {
        return ResponseEntity.ok(publicApiService.search(keyword));
    }
}
