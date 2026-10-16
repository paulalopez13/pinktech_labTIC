import { getCsrfToken } from "./authService";

const PROFILE_URL = `http://${window.location.hostname}:8080/api/profile`;

async function readResponse(response) {
    const text = await response.text();
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch {
        return { message: text };
    }
}

export async function getProfile() {
    const response = await fetch(PROFILE_URL, { credentials: "include" });
    const data = await readResponse(response);
    if (!response.ok) throw new Error(data?.message || "No se pudo cargar el perfil.");
    return data;
}

export async function updateProfile(profile) {
    const csrf = await getCsrfToken();
    const response = await fetch(PROFILE_URL, {
        method: "PUT",
        headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
        credentials: "include",
        body: JSON.stringify(profile),
    });
    const data = await readResponse(response);
    if (!response.ok) throw new Error(data?.message || "No se pudo guardar el perfil.");
    return data;
}
