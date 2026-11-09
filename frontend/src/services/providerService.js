const API_URL = "/api";
const COMPANY_REGISTRATION_URL = `${API_URL}/providers/companies/register`;

export async function registerCompany(companyData) {
    return registerProvider(COMPANY_REGISTRATION_URL, companyData);
}

export async function registerIndividual(individualData) {
    return registerProvider(`${API_URL}/providers/individuals/register`, individualData);
}

export async function registerTourist(touristData) {
    return registerProvider(`${API_URL}/tourists/register`, touristData);
}

async function registerProvider(url, providerData) {
    let response;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 20000);
    try {
        response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(providerData),
            signal: controller.signal,
        });
    } catch (error) {
        if (error.name === "AbortError") {
            throw new Error("El servidor está demorando en responder. Revisá que Spring Boot esté iniciado e intentá nuevamente.");
        }
        throw new Error(
            "No se pudo conectar con el servidor. Verificá que la aplicación esté iniciada."
        );
    } finally {
        clearTimeout(timeout);
    }

    const responseText = await response.text();

    let responseData = null;

    if (responseText) {
        try {
            responseData = JSON.parse(responseText);
        } catch {
            responseData = responseText;
        }
    }

    if (!response.ok) {
        let errorMessage = "No se pudo completar el registro.";

        if (typeof responseData === "string") {
            errorMessage = responseData;
        } else if (responseData?.message) {
            errorMessage = responseData.message;
        } else if (responseData) {
            const validationMessages = Object.values(responseData);

            if (validationMessages.length > 0) {
                errorMessage = validationMessages.join(" ");
            }
        }

        throw new Error(errorMessage);
    }

    return responseData;
}
