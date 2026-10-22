package uy.edu.um.proyTic2026.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyTic2026.entities.Administrator;
import uy.edu.um.proyTic2026.repositories.UserRepository;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.initial-admin.enabled", havingValue = "true", matchIfMissing = true)
public class InitialAdministratorInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial-admin.username:admin.inicial}")
    private String username;

    @Value("${app.initial-admin.email:admin.inicial@uruguaymnatural.uy}")
    private String email;

    @Value("${app.initial-admin.password:AdminUruguay2026!}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(username)) return;
        Administrator administrator = new Administrator();
        administrator.setUsername(username);
        administrator.setEmail(email);
        administrator.setPassword(passwordEncoder.encode(password));
        userRepository.save(administrator);
    }
}
