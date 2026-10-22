import { getCsrfToken } from "./authService";

const API_URL = `http://${window.location.hostname}:8080/api/admin/administrators`;

export async function createAdministrator(data) {
    const csrf = await getCsrfToken();
    let response;
    try {
        response = await fetch(API_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
            credentials: "include",
            body: JSON.stringify(data),
        });
    } catch {
        throw new Error("No se pudo conectar con el servidor.");
    }
    const raw = await response.text();
    let result;
    try { result = raw ? JSON.parse(raw) : null; } catch { result = { message: raw }; }
    if (!response.ok) throw new Error(result?.message || "No se pudo crear el administrador.");
    return result;
}
