package uy.edu.um.proyTic2026.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Título y descripción que se muestran arriba de la documentación (/swagger-ui.html).
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI uruguayMnaturalOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Uruguay Mnatural API")
                .version("1.0")
                .description("API de la plataforma de turismo Uruguay Mnatural. "
                        + "La sección \"API pública\" es la que pueden usar otras plataformas, sin iniciar sesión."));
    }
}
