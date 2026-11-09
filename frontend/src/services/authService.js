export const API_URL = "/api";

export async function loginUser(credentials) {
    let response;
    try {
        response = await fetch(`${API_URL}/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(credentials),
            credentials: "include",
        });
    } catch {
        throw new Error("No se pudo conectar con el servidor. Verificá que la aplicación esté iniciada.");
    }

    const responseData = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(responseData.message || "No se pudo iniciar sesión.");
    }
    return responseData;
}

export async function getCurrentUser() {
    const response = await fetch(`${API_URL}/auth/me`, { credentials: "include" });
    if (!response.ok) throw new Error("No hay una sesión activa");
    return response.json();
}

export async function logoutUser() {
    await fetch(`${API_URL}/auth/logout`, { method: "POST", credentials: "include" });
}

export async function getCsrfToken() {
    const response = await fetch(`${API_URL}/auth/csrf`, { credentials: "include" });
    if (!response.ok) throw new Error("No se pudo validar la sesión. Volvé a iniciar sesión.");
    return response.json();
}
