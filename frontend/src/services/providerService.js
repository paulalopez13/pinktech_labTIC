const API_URL = `http://${window.location.hostname}:8080/api`;
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
    try {
        response = await fetch(url, {
            method: "POST",

            headers: {
                "Content-Type": "application/json",
            },

            body: JSON.stringify(providerData),
        });
    } catch {
        throw new Error(
            "No se pudo conectar con el servidor. Verificá que Spring Boot esté ejecutándose en http://localhost:8080."
        );
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
