package uy.edu.um.proyTic2026.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uy.edu.um.proyTic2026.entities.Administrator;
import uy.edu.um.proyTic2026.entities.CompanyProvider;
import uy.edu.um.proyTic2026.entities.IndividualProvider;
import uy.edu.um.proyTic2026.entities.ProviderStatus;
import uy.edu.um.proyTic2026.entities.Tourist;
import uy.edu.um.proyTic2026.repositories.IndividualProviderRepository;
import uy.edu.um.proyTic2026.repositories.CompanyProviderRepository;
import uy.edu.um.proyTic2026.repositories.TouristRepository;
import uy.edu.um.proyTic2026.repositories.UserRepository;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-users.enabled", havingValue = "true")
public class DemoUsersInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final TouristRepository touristRepository;
    private final IndividualProviderRepository providerRepository;
    private final CompanyProviderRepository companyProviderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        createTourist();
        createApprovedProvider();
        createAdministrator();
        createApprovalExamples();
    }

    private void createTourist() {
        if (userRepository.existsByUsername("turista.demo")) return;

        Tourist tourist = new Tourist();
        tourist.setUsername("turista.demo");
        tourist.setEmail("turista.demo@uruguaymnatural.uy");
        tourist.setPassword(passwordEncoder.encode("Turista2026!"));
        tourist.setFirstName("Turista");
        tourist.setLastName("Demo");
        tourist.setCi("55555555");
        tourist.setAge(30);
        touristRepository.save(tourist);
    }

    private void createApprovedProvider() {
        if (userRepository.existsByUsername("prestador.demo")) return;

        IndividualProvider provider = new IndividualProvider();
        provider.setUsername("prestador.demo");
        provider.setEmail("prestador.demo@uruguaymnatural.uy");
        provider.setPassword(passwordEncoder.encode("Prestador2026!"));
        provider.setPhone("099123456");
        provider.setDepartment("Montevideo");
        provider.setCity("Montevideo");
        provider.setAddress("Dirección de demostración");
        provider.setFirstName("Prestador");
        provider.setLastName("Demo");
        provider.setCi("44444444");
        provider.setStatus(ProviderStatus.APPROVED);
        providerRepository.save(provider);
    }

    private void createAdministrator() {
        if (userRepository.existsByUsername("admin.demo")) return;

        Administrator administrator = new Administrator();
        administrator.setUsername("admin.demo");
        administrator.setEmail("admin.demo@uruguaymnatural.uy");
        administrator.setPassword(passwordEncoder.encode("Admin2026!"));
        userRepository.save(administrator);
    }

    private void createApprovalExamples() {
        createIndividual("demo.aceptado1", "aceptado1.demo@uruguaymnatural.uy", "Ana", "Pérez", "71000001", ProviderStatus.APPROVED);
        createIndividual("demo.aceptado2", "aceptado2.demo@uruguaymnatural.uy", "Bruno", "Silva", "71000002", ProviderStatus.APPROVED);
        createCompany("demo.aceptado3", "aceptado3.demo@uruguaymnatural.uy", "Senderos del Este", "219990001001", ProviderStatus.APPROVED);
        createIndividual("demo.rechazado1", "rechazado1.demo@uruguaymnatural.uy", "Carla", "Díaz", "71000003", ProviderStatus.REJECTED);
        createCompany("demo.rechazado2", "rechazado2.demo@uruguaymnatural.uy", "Rutas del Norte", "219990001002", ProviderStatus.REJECTED);

        createIndividual("demo.pendiente1", "pendiente1.demo@uruguaymnatural.uy", "Diego", "López", "71000004", ProviderStatus.PENDING);
        createIndividual("demo.pendiente2", "pendiente2.demo@uruguaymnatural.uy", "Elena", "Suárez", "71000005", ProviderStatus.PENDING);
        createCompany("demo.pendiente3", "pendiente3.demo@uruguaymnatural.uy", "Aventura Serrana", "219990001003", ProviderStatus.PENDING);
        createIndividual("demo.pendiente4", "pendiente4.demo@uruguaymnatural.uy", "Federico", "Costa", "71000006", ProviderStatus.PENDING);
        createCompany("demo.pendiente5", "pendiente5.demo@uruguaymnatural.uy", "Costa Natural", "219990001004", ProviderStatus.PENDING);
    }

    private void createIndividual(String username, String email, String firstName, String lastName,
                                  String ci, ProviderStatus status) {
        if (userRepository.existsByUsername(username)) return;
        IndividualProvider provider = new IndividualProvider();
        provider.setUsername(username);
        provider.setEmail(email);
        provider.setPassword(passwordEncoder.encode("Demo2026!"));
        provider.setPhone("099" + ci.substring(ci.length() - 6));
        provider.setDepartment("Montevideo");
        provider.setCity("Montevideo");
        provider.setAddress("Dirección de demostración " + ci.substring(ci.length() - 2));
        provider.setFirstName(firstName);
        provider.setLastName(lastName);
        provider.setCi(ci);
        provider.setStatus(status);
        providerRepository.save(provider);
    }

    private void createCompany(String username, String email, String companyName, String rut, ProviderStatus status) {
        if (userRepository.existsByUsername(username)) return;
        CompanyProvider company = new CompanyProvider();
        company.setUsername(username);
        company.setEmail(email);
        company.setPassword(passwordEncoder.encode("Demo2026!"));
        company.setPhone("098123456");
        company.setDepartment("Maldonado");
        company.setCity("Punta del Este");
        company.setAddress("Dirección de demostración");
        company.setCompanyName(companyName);
        company.setRut(rut);
        company.setStatus(status);
        companyProviderRepository.save(company);
    }
}
