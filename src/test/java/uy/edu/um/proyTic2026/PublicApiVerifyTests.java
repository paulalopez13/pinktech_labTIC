package uy.edu.um.proyTic2026;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uy.edu.um.proyTic2026.dtos.ProviderVerificationResponse;
import uy.edu.um.proyTic2026.services.PublicApiService;

import static org.junit.jupiter.api.Assertions.*;

// Prueba "verificar emprendimiento" (API pública) con los prestadores de los datos de demostración.
@SpringBootTest
class PublicApiVerifyTests {

    @Autowired PublicApiService publicApiService;

    @Test
    void approvedIndividualIsVerifiedWithName() {
        ProviderVerificationResponse result = publicApiService.verify("44444444");

        assertTrue(result.registered());
        assertTrue(result.verified());
        assertEquals("Prestador Demo", result.name());
        assertEquals("Prestador independiente", result.providerType());
        assertNotNull(result.providerId());
    }

    @Test
    void approvedCompanyIsVerifiedByRut() {
        ProviderVerificationResponse result = publicApiService.verify("219990001001");

        assertTrue(result.verified());
        assertEquals("Senderos del Este", result.name());
        assertEquals("Empresa", result.providerType());
    }

    @Test
    void pendingProviderIsRegisteredButNotVerifiedAndHidesName() {
        ProviderVerificationResponse result = publicApiService.verify("71000004");

        assertTrue(result.registered());
        assertFalse(result.verified());
        assertNull(result.name());
        assertNull(result.providerId());
    }

    @Test
    void rejectedProviderIsNotVerified() {
        ProviderVerificationResponse result = publicApiService.verify("71000003");

        assertTrue(result.registered());
        assertFalse(result.verified());
        assertNull(result.name());
    }

    @Test
    void unknownDocumentIsNotRegistered() {
        ProviderVerificationResponse result = publicApiService.verify("12345678");

        assertFalse(result.registered());
        assertFalse(result.verified());
    }

    @Test
    void ignoresSpacesAroundTheDocument() {
        assertTrue(publicApiService.verify("  44444444  ").verified());
    }

    @Test
    void requiresDocument() {
        assertThrows(IllegalArgumentException.class, () -> publicApiService.verify(" "));
    }
}
