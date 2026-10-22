import { getCsrfToken } from "./authService";

const API_URL = `http://${window.location.hostname}:8080/api`;

async function readResponse(response) {
    const text = await response.text();
    if (!text) return null;
    try { return JSON.parse(text); } catch { return { message: text }; }
}

async function request(path, options = {}) {
    let response;
    try {
        response = await fetch(`${API_URL}${path}`, { credentials: "include", ...options });
    } catch {
        throw new Error("No se pudo conectar con el servidor. Verificá que Spring Boot esté ejecutándose en http://localhost:8080.");
    }
    const data = await readResponse(response);
    if (!response.ok) throw new Error(data?.message || "No se pudo completar la operación.");
    return data;
}

async function mutation(path, method, body) {
    const csrf = await getCsrfToken();
    return request(path, {
        method,
        headers: { [csrf.headerName]: csrf.token, ...(body ? { "Content-Type": "application/json" } : {}) },
        ...(body ? { body: JSON.stringify(body) } : {}),
    });
}

export const getActivities = () => request("/activities");
export const getMyActivities = () => request("/activities/mine");
export const getMyReservations = () => request("/reservations/mine");
export const getActivityReviews = id => request(`/activities/${id}/reviews`);
export const getActivityAvailability = (id, activityStart) => request(`/activities/${id}/availability?activityStart=${encodeURIComponent(activityStart)}`);
export const getPublicProvider = id => request(`/activities/providers/${id}`);
export const getFavoriteActivityIds = () => request("/favorites/activities/ids");
export const getFavoriteActivities = () => request("/favorites/activities");
export const saveFavoriteActivity = id => mutation(`/favorites/activities/${id}`, "POST");
export const removeFavoriteActivity = id => mutation(`/favorites/activities/${id}`, "DELETE");
async function multipartMutation(path, method, activity, photos = [], keepPhotoIds = []) {
    const csrf = await getCsrfToken();
    const form = new FormData();
    form.append("activity", new Blob([JSON.stringify(activity)], { type: "application/json" }));
    photos.forEach(photo => form.append("photos", photo));
    keepPhotoIds.forEach(id => form.append("keepPhotoIds", String(id)));
    return request(path, { method, headers: { [csrf.headerName]: csrf.token }, body: form });
}

export const createActivity = (activity, photos) => multipartMutation("/activities", "POST", activity, photos);
export const updateActivity = (id, activity, photos, keepPhotoIds) => multipartMutation(`/activities/${id}`, "PUT", activity, photos, keepPhotoIds);
export const deleteActivity = id => mutation(`/activities/${id}`, "DELETE");
export const reserveActivitySandbox = (id, reservation) => mutation(`/reservations/activities/${id}/sandbox-payment`, "POST", reservation);
export const submitActivityReview = (reservationId, review) => mutation(`/reservations/${reservationId}/review`, "POST", review);
