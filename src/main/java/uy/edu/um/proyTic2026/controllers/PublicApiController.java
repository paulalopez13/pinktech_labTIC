package uy.edu.um.proyTic2026.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Listado de emprendimientos", description = "Devuelve todos los emprendimientos aprobados por un administrador. No recibe parámetros; la respuesta es una lista que puede estar vacía.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "[{\"id\":12,\"displayName\":\"Aventura del Este\",\"providerType\":\"Empresa\",\"department\":\"Maldonado\",\"city\":\"Punta del Este\",\"averageRating\":4.5,\"reviewCount\":8}]"))),
            @ApiResponse(responseCode = "500", description = "Error inesperado al consultar los emprendimientos")
    })
    @GetMapping("/providers")
    public ResponseEntity<?> providers() {
        return ResponseEntity.ok(publicApiService.providers());
    }

    // GET /api/public/providers/verify?document=44444444
    // Acepta un RUT (empresa) o una cédula (prestador particular).
    @Operation(summary = "Verificar emprendimiento", description = "Indica si un RUT (empresa) o una cédula (prestador particular) está registrado y aprobado. El nombre solo se muestra si está aprobado. Ejemplo: /api/public/providers/verify?document=44444444.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verificación completada, incluso si no se encontró el documento", content = @Content(mediaType = "application/json", examples = {
                    @ExampleObject(name = "Aprobado", value = "{\"document\":\"44444444\",\"registered\":true,\"verified\":true,\"providerId\":12,\"displayName\":\"Aventura del Este\",\"providerType\":\"Empresa\"}"),
                    @ExampleObject(name = "No encontrado", value = "{\"document\":\"44444444\",\"registered\":false,\"verified\":false,\"providerId\":null,\"displayName\":null,\"providerType\":null}")
            })),
            @ApiResponse(responseCode = "400", description = "Falta el documento", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\":\"Indicá el RUT o la cédula del emprendimiento.\"}"))),
            @ApiResponse(responseCode = "500", description = "Error inesperado al consultar el documento")
    })
    @GetMapping("/providers/verify")
    public ResponseEntity<?> verify(@Parameter(description = "RUT o cédula", example = "44444444") @RequestParam String document) {
        try {
            return ResponseEntity.ok(publicApiService.verify(document));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", error.getMessage()));
        }
    }

    // GET /api/public/activities?department=Maldonado&city=Punta del Este&date=2026-11-02
    // La localidad es opcional. Si no se manda la fecha, se usa la de hoy.
    @Operation(summary = "Actividades de un día", description = "Actividades con salidas en la fecha indicada, en un departamento y opcionalmente una localidad. No distingue mayúsculas ni tildes. Ejemplo: /api/public/activities?department=Maldonado&city=Punta%20del%20Este&date=2026-11-02.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actividades obtenidas; la lista puede estar vacía", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "[{\"id\":31,\"name\":\"Avistamiento de ballenas\",\"department\":\"Maldonado\",\"city\":\"Punta del Este\",\"price\":1200}]"))),
            @ApiResponse(responseCode = "400", description = "Falta el departamento o la fecha no tiene formato AAAA-MM-DD", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\":\"Indicá el departamento.\"}"))),
            @ApiResponse(responseCode = "500", description = "Error inesperado al consultar actividades")
    })
    @GetMapping("/activities")
    public ResponseEntity<?> activitiesOn(@Parameter(description = "Nombre del departamento uruguayo", example = "Maldonado") @RequestParam String department,
                                          @Parameter(description = "Localidad opcional; debe pertenecer al departamento indicado", example = "Punta del Este") @RequestParam(required = false) String city,
                                          @Parameter(description = "Fecha AAAA-MM-DD (opcional, por defecto hoy)", example = "2026-11-02")
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            return ResponseEntity.ok(publicApiService.activitiesOn(date == null ? LocalDate.now() : date, department, city));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", error.getMessage()));
        }
    }

    // GET /api/public/activities/top?department=Maldonado&month=2026-10
    // Si no se manda el mes, se usa el actual.
    @Operation(summary = "Top 3 del mes", description = "Las 3 actividades mejor puntuadas de un departamento, contando las salidas realizadas en el mes indicado. Si empatan en promedio, gana la que tiene más opiniones. Ejemplo: /api/public/activities/top?department=Maldonado&month=2026-10.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking obtenido; puede contener menos de tres actividades", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "[{\"position\":1,\"monthAverageRating\":4.8,\"monthReviewCount\":12,\"activity\":{\"id\":31,\"name\":\"Avistamiento de ballenas\",\"department\":\"Maldonado\",\"city\":\"Punta del Este\"}}]"))),
            @ApiResponse(responseCode = "400", description = "Falta el departamento o el mes no tiene formato AAAA-MM", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\":\"Indicá el departamento.\"}"))),
            @ApiResponse(responseCode = "500", description = "Error inesperado al calcular el ranking")
    })
    @GetMapping("/activities/top")
    public ResponseEntity<?> topRated(@Parameter(description = "Nombre del departamento uruguayo", example = "Maldonado") @RequestParam String department,
                                      @Parameter(description = "Mes AAAA-MM (opcional, por defecto el actual)", example = "2026-10")
                                      @RequestParam(required = false)
                                      @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        try {
            return ResponseEntity.ok(publicApiService.topRated(department, month == null ? YearMonth.now() : month));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", error.getMessage()));
        }
    }

    // GET /api/public/activities/search?q=ballenas
    @Operation(summary = "Buscar actividades", description = "Busca la palabra en el nombre, la descripción, la ciudad, el departamento y las categorías. No distingue mayúsculas ni tildes. Ejemplo: /api/public/activities/search?q=ballenas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Búsqueda completada; la lista puede estar vacía", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "[{\"id\":31,\"name\":\"Avistamiento de ballenas\",\"department\":\"Maldonado\",\"city\":\"Punta del Este\"}]"))),
            @ApiResponse(responseCode = "400", description = "La búsqueda debe tener al menos dos letras", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\":\"Ingresá una palabra de al menos 2 letras.\"}"))),
            @ApiResponse(responseCode = "500", description = "Error inesperado al buscar actividades")
    })
    @GetMapping("/activities/search")
    public ResponseEntity<?> search(@Parameter(description = "Palabra clave (al menos 2 letras)", example = "ballenas") @RequestParam("q") String keyword) {
        try {
            return ResponseEntity.ok(publicApiService.search(keyword));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", error.getMessage()));
        }
    }
}
