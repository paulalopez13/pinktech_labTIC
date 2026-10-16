package uy.edu.um.proyTic2026;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import uy.edu.um.proyTic2026.entities.ProviderStatus;
import uy.edu.um.proyTic2026.repositories.CompanyProviderRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CompanyRegistrationIntegrationTests {
    @Autowired Environment environment;
    @Autowired CompanyProviderRepository repository;
    @Autowired PasswordEncoder passwordEncoder;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpRequest.Builder request() {
        return HttpRequest.newBuilder(URI.create("http://localhost:"
                + environment.getProperty("local.server.port")
                + "/api/providers/companies/register"))
                .header("Origin", "http://localhost:5173");
    }

    private HttpResponse<String> register(String body) throws Exception {
        return client.send(request().header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void registrationAllowsCorsAndSavesPendingWithHashedPassword() throws Exception {
        var preflight = client.send(request()
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, preflight.statusCode());
        assertEquals("http://localhost:5173",
                preflight.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());

        String body = """
                {"username":"empresa_prueba","email":"prueba@example.com",
                "password":"Prueba1234","phone":"099123456","department":"Montevideo",
                "city":"Montevideo","address":"Calle 123","companyName":"Empresa prueba",
                "rut":"001234567890"}
                """;
        var response = register(body);
        assertEquals(201, response.statusCode(), response.body());
        assertEquals("http://localhost:5173",
                response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        assertFalse(response.body().contains("Prueba1234"));
        assertFalse(response.body().contains("$2"));
        var saved = repository.findAll().stream()
                .filter(company -> company.getUsername().equals("empresa_prueba"))
                .findFirst().orElseThrow();
        assertEquals(ProviderStatus.PENDING, saved.getStatus());
        assertNotEquals("Prueba1234", saved.getPassword());
        assertTrue(passwordEncoder.matches("Prueba1234", saved.getPassword()));

        var duplicateUsername = register(body);
        assertEquals(400, duplicateUsername.statusCode());
        assertTrue(duplicateUsername.body().contains("nombre de usuario"));
        var duplicateEmail = register(body.replace("empresa_prueba", "otra_empresa"));
        assertEquals(400, duplicateEmail.statusCode());
        assertTrue(duplicateEmail.body().contains("email"));
        var duplicateRut = register(body.replace("empresa_prueba", "otra_empresa")
                .replace("prueba@example.com", "otra@example.com"));
        assertEquals(400, duplicateRut.statusCode());
        assertTrue(duplicateRut.body().contains("RUT"));

        var invalid = register(body.replace("Prueba1234", "corta"));
        assertEquals(400, invalid.statusCode());
        assertTrue(invalid.body().contains("password"));
    }
}
