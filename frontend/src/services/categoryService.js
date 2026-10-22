import { getCsrfToken } from "./authService";

const API_URL = `http://${window.location.hostname}:8080/api/categories`;

async function readResponse(response) {
    const text = await response.text();
    if (!text) return null;
    try { return JSON.parse(text); } catch { return { message: text }; }
}

async function request(url, options = {}) {
    const response = await fetch(url, { credentials: "include", ...options });
    const data = await readResponse(response);
    if (!response.ok) throw new Error(data?.message || "No se pudo completar la operación.");
    return data;
}

export async function getCategories() {
    return request(API_URL);
}

export async function createCategory(name) {
    const csrf = await getCsrfToken();
    return request(API_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
        body: JSON.stringify({ name }),
    });
}

export async function renameCategory(id, name) {
    const csrf = await getCsrfToken();
    return request(`${API_URL}/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
        body: JSON.stringify({ name }),
    });
}

export async function deleteCategory(id) {
    const csrf = await getCsrfToken();
    return request(`${API_URL}/${id}`, {
        method: "DELETE",
        headers: { [csrf.headerName]: csrf.token },
    });
}
