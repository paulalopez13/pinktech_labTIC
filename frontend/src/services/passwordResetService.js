import { API_URL } from "./authService";

async function readResponse(response) {
    const text = await response.text();
    if (!text) return null;
    try { return JSON.parse(text); } catch { return { message: text }; }
}

export async function requestPasswordReset(email) {
    const response = await fetch(`${API_URL}/auth/password/forgot`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ email }),
    });
    const data = await readResponse(response);
    if (!response.ok) throw new Error(data?.message || "No se pudo procesar la solicitud.");
    return data;
}

export async function resetPassword({ token, password, confirmPassword }) {
    const response = await fetch(`${API_URL}/auth/password/reset`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ token, password, confirmPassword }),
    });
    const data = await readResponse(response);
    if (!response.ok) throw new Error(data?.message || "No se pudo cambiar la contraseña.");
    return data;
}
