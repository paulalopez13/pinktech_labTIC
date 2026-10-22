import { useEffect, useState } from "react";
import { reserveActivity, getActivityReviews, getActivityAvailability } from "../services/activityService";

const weekdayLabels = { MONDAY: "lunes", TUESDAY: "martes", WEDNESDAY: "miércoles", THURSDAY: "jueves", FRIDAY: "viernes", SATURDAY: "sábado", SUNDAY: "domingo" };
const recurrenceLabels = { ONCE: "Una sola vez", DAILY: "Diariamente", WEEKLY: "Semanalmente", BIWEEKLY: "Cada 2 semanas", MONTHLY: "Mensualmente", SEASONAL: "Por temporada" };
const dateTime = value => new Date(value).toLocaleString("es-UY", { dateStyle: "medium", timeStyle: "short" });
const priceText = value => Number(value) === 0 ? "Gratis" : `$U ${new Intl.NumberFormat("es-UY", { maximumFractionDigits: 2 }).format(Number(value))}`;

export default function ActivityDetailPage({ activity, currentUser, isOwner = false, favorite = false, onToggleFavorite, onProviderClick, onCategoryClick, onBack, onLogin, onReserved, onEdit, onAddActivity }) {
    const [photoIndex, setPhotoIndex] = useState(0);
    const [selectedTime, setSelectedTime] = useState(activity.upcomingOccurrences?.[0] || "");
    const [reviews, setReviews] = useState([]);
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);
    const [reviewsLoaded, setReviewsLoaded] = useState(false);
    const [availableSpots, setAvailableSpots] = useState(null);
    const [availabilityLoading, setAvailabilityLoading] = useState(false);
    const photos = activity.photos || [];
    const selectedPhoto = photos[Math.min(photoIndex, Math.max(photos.length - 1, 0))];
    const selectedDays = (activity.weekdays || []).map(day => weekdayLabels[day]).filter(Boolean);
    const recurrence = `${recurrenceLabels[activity.recurrence] || ""}${selectedDays.length ? ` · ${selectedDays.join(", ")}` : ""}`;
    const hasRequirements = Boolean(activity.equipment || activity.otherRequirements || Number(activity.minimumAge) > 0
        || activity.maximumAge != null || Number(activity.minimumHeightCm) > 0 || activity.maximumHeightCm != null || activity.difficulty);

    useEffect(() => {
        let active = true;
        getActivityReviews(activity.id)
            .then(data => { if (active) setReviews(data); })
            .catch(() => {})
            .finally(() => { if (active) setReviewsLoaded(true); });
        return () => { active = false; };
    }, [activity.id]);

    useEffect(() => {
        if (!selectedTime) { setAvailableSpots(null); return; }
        let active = true;
        setAvailabilityLoading(true);
        getActivityAvailability(activity.id, selectedTime).then(data => active && setAvailableSpots(data.remaining))
            .catch(() => active && setAvailableSpots(null))
            .finally(() => active && setAvailabilityLoading(false));
        return () => { active = false; };
    }, [activity.id, selectedTime]);

    async function handleReserve() {
        if (!selectedTime) return;
        setBusy(true);
        setError("");
        try {
            await reserveActivity(activity.id, selectedTime);
            setAvailableSpots(value => value == null ? value : Math.max(0, value - 1));
            onReserved?.("¡Listo! El cupo quedó reservado.");
        } catch (reserveError) {
            setError(reserveError.message);
        } finally {
            setBusy(false);
        }
    }

    return <main className="registration-page form-page activity-entry-page">
        <button className="back-button" type="button" onClick={onBack}>← Volver</button>
        <article className="activity-detail-card">
            <div className="activity-preview-gallery activity-detail-gallery">
                {selectedPhoto
                    ? <img src={`http://${window.location.hostname}:8080${selectedPhoto.url}`} alt={selectedPhoto.fileName || activity.name} />
                    : <div className="activity-detail-no-photo"><span aria-hidden="true">✳</span><p>Esta actividad todavía no tiene fotos.</p></div>}
                {photos.length > 1 && <>
                    <button className="gallery-arrow previous" type="button" aria-label="Foto anterior"
                        onClick={() => setPhotoIndex(index => (index - 1 + photos.length) % photos.length)}>‹</button>
                    <button className="gallery-arrow next" type="button" aria-label="Foto siguiente"
                        onClick={() => setPhotoIndex(index => (index + 1) % photos.length)}>›</button>
                    <span className="gallery-counter">{Math.min(photoIndex, photos.length - 1) + 1} / {photos.length}</span>
                </>}
                {!isOwner && (currentUser?.role === "TOURIST" || !currentUser) && <button type="button"
                    className={`activity-detail-heart ${favorite ? "active" : ""}`} onClick={onToggleFavorite}
                    aria-label={favorite ? "Quitar actividad de favoritos" : "Iniciar sesión para guardar actividad"}>{favorite ? "♥" : "♡"}</button>}
            </div>

            <div className="activity-detail-heading">
                <h1>{activity.name}</h1>
                <p>{activity.city}, {activity.department} · {activity.location}</p>
            </div>
            <p className="activity-detail-description">{activity.description}</p>
            <div className="activity-provider-row">
                <button type="button" className="activity-provider-link" onClick={onProviderClick} disabled={!activity.providerId}>
                    <strong>{activity.providerName}</strong><small>Ver perfil del prestador →</small>
                </button>
            </div>
            <div className="activity-preview-tags">
                {activity.categories.map(category => <button type="button" key={category.id} onClick={() => onCategoryClick?.(category.name)}>{category.name}</button>)}
            </div>
            <div className="activity-preview-highlights">
                <p><strong>{availabilityLoading ? "…" : availableSpots ?? activity.capacity}</strong><span>cupos disponibles para este horario</span></p>
                <p><strong>{priceText(activity.price)}</strong><span>por persona</span></p>
            </div>

            <div className="activity-preview-grid">
                <p><strong>Frecuencia:</strong> {recurrence}{activity.repeatUntil ? ` hasta ${new Date(`${activity.repeatUntil}T12:00:00`).toLocaleDateString("es-UY")}` : ""}</p>
                <p><strong>Horarios:</strong> {activity.startTimes.join(" · ")}</p>
                <p><strong>Duración:</strong> {activity.durationMinutes >= 60 ? `${Math.floor(activity.durationMinutes / 60)} h${activity.durationMinutes % 60 ? ` ${activity.durationMinutes % 60} min` : ""}` : `${activity.durationMinutes} min`}</p>
                <p><strong>Reservas cierran:</strong> {activity.bookingCutoffHours} horas antes.</p>
            </div>

            {hasRequirements && <section className="activity-preview-requirements">
                <h2>Requisitos para participar</h2>
                {Number(activity.minimumAge) > 0 && <p><strong>Edad mínima:</strong> {activity.minimumAge} años.</p>}
                {activity.maximumAge != null && <p><strong>Edad máxima:</strong> {activity.maximumAge} años.</p>}
                {Number(activity.minimumHeightCm) > 0 && <p><strong>Altura mínima:</strong> {activity.minimumHeightCm} cm.</p>}
                {activity.maximumHeightCm != null && <p><strong>Altura máxima:</strong> {activity.maximumHeightCm} cm.</p>}
                {activity.difficulty && <p><strong>Nivel requerido:</strong> {activity.difficulty}.</p>}
                {activity.equipment && <p><strong>Equipamiento necesario:</strong> {activity.equipment}</p>}
                {activity.otherRequirements && <p><strong>Otras indicaciones:</strong> {activity.otherRequirements}</p>}
            </section>}

            {isOwner ? <div className="activity-detail-actions">
                <button className="submit-button" type="button" onClick={() => onEdit?.(activity)}>Editar actividad</button>
                <button className="activity-step-back" type="button" onClick={onAddActivity}>Agregar otra actividad</button>
            </div> : currentUser?.role === "TOURIST" && activity.upcomingOccurrences?.length > 0 ? <div className="activity-booking-row">
                <label className="form-field">Elegí fecha y hora
                    <select value={selectedTime} onChange={event => setSelectedTime(event.target.value)}>
                        {activity.upcomingOccurrences.map(occurrence => <option key={occurrence} value={occurrence}>{dateTime(occurrence)}</option>)}
                    </select>
                </label>
                <button className="submit-button" type="button" disabled={busy} onClick={handleReserve}>{busy ? "Reservando…" : "Reservar cupo"}</button>
            </div> : null}
            {!isOwner && currentUser?.role === "TOURIST" && <button className={`activity-save-action ${favorite ? "active" : ""}`} type="button" onClick={onToggleFavorite}>
                {favorite ? "♥ Guardada en favoritos" : "♡ Guardar actividad en favoritos"}</button>}
            {!isOwner && !currentUser && <button className="submit-button activity-login-action" type="button" onClick={onLogin}>
                Iniciá sesión para reservar o guardar la actividad</button>}
            {error && <div className="form-message error" role="alert">{error}</div>}

            <section className="activity-reviews">
                <h2>Opiniones de quienes participaron</h2>
                {!reviewsLoaded ? <p>Cargando opiniones…</p> : reviews.length === 0 ? <p>Todavía no hay comentarios.</p> : reviews.map(review => <article className="activity-review-card" key={review.id}>
                    <div><strong>{review.username}</strong><span aria-label={`${review.rating} de 5 estrellas`}>{"★".repeat(review.rating)}{"☆".repeat(5 - review.rating)}</span></div>
                    <p>{review.comment}</p>
                </article>)}
            </section>
        </article>
    </main>;
}
