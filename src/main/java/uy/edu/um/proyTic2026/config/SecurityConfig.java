package uy.edu.um.proyTic2026.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Usa el @CrossOrigin del controller también en los filtros de seguridad.
                .cors(cors -> {})
                // El registro es público y no usa una sesión iniciada en el navegador.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                "/api/providers/companies/register", "/api/providers/individuals/register",
                                "/api/tourists/register", "/api/auth/login", "/api/auth/logout",
                                "/api/auth/password/forgot", "/api/auth/password/reset"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/providers/companies/register",
                                "/api/providers/individuals/register", "/api/tourists/register",
                                "/api/auth/login", "/api/auth/logout", "/api/auth/password/forgot",
                                "/api/auth/password/reset").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me", "/api/auth/csrf").permitAll()
                        // API pública para otras plataformas: solo lectura y sin iniciar sesión.
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers("/api/profile", "/api/profile/**").permitAll()
                        .requestMatchers("/api/admin/providers", "/api/admin/providers/**",
                                "/api/admin/administrators", "/api/admin/administrators/**",
                                "/api/categories", "/api/categories/**",
                                "/api/activities", "/api/activities/**", "/api/favorites/**",
                                "/api/reservations", "/api/reservations/**").permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
