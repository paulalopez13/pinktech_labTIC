import { getCsrfToken } from "./authService";

const API_URL = `http://${window.location.hostname}:8080/api/admin/providers`;

async function readResponse(response) {
    const text = await response.text();
    if (!text) return null;
    try { return JSON.parse(text); } catch { return { message: text }; }
}

export async function getProviderApplications() {
    const response = await fetch(API_URL, { credentials: "include" });
    const data = await readResponse(response);
    if (!response.ok) throw new Error(data?.message || "No se pudieron cargar las solicitudes.");
    return data;
}

export async function decideProviderApplication(id, decision) {
    const csrf = await getCsrfToken();
    const response = await fetch(`${API_URL}/${id}/${decision}`, {
        method: "POST",
        headers: { [csrf.headerName]: csrf.token },
        credentials: "include",
    });
    const data = await readResponse(response);
    if (!response.ok) throw new Error(data?.message || "No se pudo actualizar la solicitud.");
    return data;
}
