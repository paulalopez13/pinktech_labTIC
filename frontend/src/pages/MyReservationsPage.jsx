import { useEffect, useMemo, useState } from "react";
import { answerWaitlistOffer, cancelReservation, createStripeCheckout, getActivities, getMyReservations, rescheduleReservation, submitActivityReview } from "../services/activityService";
import BrandLogo from "../components/BrandLogo";
import RatingStars from "../components/RatingStars";

const priceText = value => Number(value) === 0 ? "Gratis" : `$U ${new Intl.NumberFormat("es-UY", { maximumFractionDigits: 2 }).format(Number(value))}`;
const dateText = value => new Date(value).toLocaleString("es-UY", { dateStyle: "medium", timeStyle: "short" });

export default function MyReservationsPage({ onBack, onOpenActivity }) {
    const [reservations, setReservations] = useState([]);
    const [activities, setActivities] = useState([]);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(true);
    const [pastRange, setPastRange] = useState("all");
    const [editingId, setEditingId] = useState(null);
    const [newDate, setNewDate] = useState("");
    const [busyId, setBusyId] = useState(null);
    const [now, setNow] = useState(Date.now());
    const [reviewing, setReviewing] = useState(null);
    const [reviewRating, setReviewRating] = useState(5);
    const [reviewComment, setReviewComment] = useState("");

    useEffect(() => {
        let active = true;
        Promise.all([getMyReservations(), getActivities()]).then(([items, allActivities]) => {
            if (active) { setReservations(items); setActivities(allActivities); }
        }).catch(loadError => active && setError(loadError.message))
            .finally(() => active && setLoading(false));
        const timer = window.setInterval(() => setNow(Date.now()), 1000);
        return () => { active = false; window.clearInterval(timer); };
    }, []);

    const upcoming = reservations.filter(item => new Date(item.activityStart).getTime() > now);
    const past = useMemo(() => {
        const rangeMs = { "10d": 10 * 86400000, "1m": 30 * 86400000, "3m": 90 * 86400000, all: Infinity }[pastRange];
        return reservations.filter(item => {
            const start = new Date(item.activityStart).getTime();
            return start <= now && now - start <= rangeMs;
        });
    }, [reservations, now, pastRange]);

    function remainingLabel(reservation) {
        if (!reservation.paymentExpiresAt) return "";
        const seconds = Math.max(0, Math.floor((new Date(reservation.paymentExpiresAt).getTime() - now) / 1000));
        if (!seconds) return "Expiró el tiempo de pago";
        return `${String(Math.floor(seconds / 3600)).padStart(2, "0")} h ${String(Math.floor((seconds % 3600) / 60)).padStart(2, "0")} min`;
    }
    function statusLabel(reservation) {
        const expired = ["PENDING_PAYMENT", "PAYMENT_RETRY_PENDING"].includes(reservation.paymentStatus) && reservation.paymentExpiresAt && new Date(reservation.paymentExpiresAt).getTime() <= now;
        if (expired || reservation.paymentStatus === "EXPIRED") return "Pago expirado · cupos liberados";
        if (reservation.paymentStatus === "APPROVED") return "Confirmada";
        if (reservation.paymentStatus === "PENDING_PAYMENT") return `Pago pendiente · quedan ${remainingLabel(reservation)}`;
        if (reservation.paymentStatus === "PAYMENT_RETRY_PENDING") return `Confirmada temporalmente · quedan ${remainingLabel(reservation)} para pagar`;
        if (reservation.paymentStatus === "WAITLISTED") return "En lista de espera";
        if (reservation.paymentStatus === "WAITLIST_OFFERED") return `Lugar disponible · respondé en ${remainingLabel(reservation)}`;
        if (reservation.paymentStatus === "CANCELLED") return "Cancelada";
        return reservation.paymentStatus;
    }
    async function pay(reservation) {
        setBusyId(reservation.id); setError("");
        try {
            const result = await createStripeCheckout(reservation.id);
            if (result.url) window.location.assign(result.url);
            else if (result.paymentStatus === "APPROVED") setReservations(items => items.map(item => item.id === reservation.id ? { ...item, paymentStatus: "APPROVED" } : item));
        } catch (payError) { setError(payError.message); }
        finally { setBusyId(null); }
    }
    async function cancel(reservation) {
        if (!window.confirm("¿Querés cancelar esta reserva? Los cupos se liberarán.")) return;
        setBusyId(reservation.id); setError("");
        try {
            const updated = await cancelReservation(reservation.id);
            setReservations(items => items.map(item => item.id === updated.id ? updated : item));
        } catch (cancelError) { setError(cancelError.message); }
        finally { setBusyId(null); }
    }
    async function answerOffer(reservation, accept) {
        setBusyId(reservation.id); setError("");
        try {
            const updated = await answerWaitlistOffer(reservation.id, accept);
            setReservations(items => items.map(item => item.id === updated.id ? updated : item));
        } catch (answerError) { setError(answerError.message); }
        finally { setBusyId(null); }
    }
    async function saveDate(reservation) {
        if (!newDate) return;
        setBusyId(reservation.id); setError("");
        try {
            const updated = await rescheduleReservation(reservation.id, newDate);
            setReservations(items => items.map(item => item.id === updated.id ? updated : item));
            setEditingId(null); setNewDate("");
        } catch (changeError) { setError(changeError.message); }
        finally { setBusyId(null); }
    }

    async function submitReview() {
        if (!reviewing || !reviewComment.trim()) { setError("Escribí un comentario antes de confirmar."); return; }
        setBusyId(reviewing.id); setError("");
        try {
            await submitActivityReview(reviewing.id, { rating: reviewRating, comment: reviewComment.trim() });
            setReservations(items => items.map(item => item.id === reviewing.id ? { ...item, reviewed: true } : item));
            setReviewing(null); setReviewComment(""); setReviewRating(5);
        } catch (reviewError) { setError(reviewError.message); }
        finally { setBusyId(null); }
    }

    async function openActivity(reservation) {
        if (!onOpenActivity) {
            if (activities.some(activity => activity.id === reservation.activityId)) return;
            setReviewing({ unavailable: true, activityName: reservation.activityName });
            return;
        }
        try {
            const opened = await onOpenActivity(reservation.activityId);
            if (!opened) setReviewing({ unavailable: true, activityName: reservation.activityName });
        } catch {
            setReviewing({ unavailable: true, activityName: reservation.activityName });
        }
    }

    function reservationCard(reservation) {
        const status = statusLabel(reservation);
        const isUpcoming = new Date(reservation.activityStart).getTime() > now;
        const isPending = ["PENDING_PAYMENT", "PAYMENT_RETRY_PENDING"].includes(reservation.paymentStatus);
        const isWaitlisted = reservation.paymentStatus === "WAITLISTED";
        const isWaitlistOffered = reservation.paymentStatus === "WAITLIST_OFFERED";
        const cancellationClosed = !isWaitlisted && !isWaitlistOffered
            && now >= new Date(reservation.activityStart).getTime() - Number(reservation.cancellationCutoffMinutes || 0) * 60000;
        const isExpired = status.startsWith("Pago expirado");
        const isConfirmed = reservation.paymentStatus === "APPROVED";
        const choices = activities.find(activity => activity.id === reservation.activityId)?.upcomingOccurrences || [];
        const photo = activities.find(activity => activity.id === reservation.activityId)?.photos?.[0];
        return <article className={`reservation-summary-card${isPending && !isExpired ? " pending-payment" : ""}`} key={reservation.id}>
            <button className="reservation-card-photo reservation-activity-open" type="button" onClick={() => openActivity(reservation)} aria-label={`Abrir actividad ${reservation.activityName}`}>{photo
                ? <img src={`${photo.url}`} alt="" loading="lazy" />
                : <div className="reservation-card-photo-placeholder"><BrandLogo /></div>}</button>
            <div className="reservation-card-main"><h2><button className="reservation-activity-title" type="button" onClick={() => openActivity(reservation)}>{reservation.activityName}</button></h2>
                <p>{reservation.city}, {reservation.department} · {reservation.location}</p>
                <p>{dateText(reservation.activityStart)}</p>
                {(isPending || isWaitlisted || isWaitlistOffered) && <strong className={`reservation-status ${isExpired ? "expired" : "pending"}`}>{status}</strong>}
                {!isPending && <strong className={`reservation-status ${isConfirmed ? "confirmed" : "expired"}`}>{status}</strong>}
                {!isUpcoming && isConfirmed && reservation.reviewAllowed && !reservation.reviewed && <button className="reservation-review-link" type="button" onClick={() => { setReviewing(reservation); setReviewRating(5); setReviewComment(""); }}>Dejar reseña</button>}
                {editingId === reservation.id && <div className="reservation-reschedule-form"><label>Nuevo día y horario<select value={newDate} onChange={event => setNewDate(event.target.value)}><option value="">Elegí un horario</option>{choices.map(slot => <option key={slot} value={slot}>{dateText(slot)}</option>)}</select></label><button type="button" disabled={busyId === reservation.id || !newDate} onClick={() => saveDate(reservation)}>Guardar</button><button type="button" onClick={() => setEditingId(null)}>Cerrar</button></div>}
            </div>
            <div className="reservation-card-meta"><div className="reservation-participant-count" aria-label={`${reservation.participantCount} personas`}><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="7" r="4"/><path d="M4.5 21v-2.2a7.5 7.5 0 0 1 15 0V21z"/></svg><span>{reservation.participantCount}</span></div><strong className="reservation-card-price">{priceText(reservation.total)}</strong>
                {isConfirmed && isUpcoming && <><button type="button" disabled={busyId === reservation.id} onClick={() => { setEditingId(editingId === reservation.id ? null : reservation.id); setNewDate(""); }}>Modificar</button>{!cancellationClosed && <button type="button" className="reservation-cancel-button" disabled={busyId === reservation.id} onClick={() => cancel(reservation)}>Cancelar</button>}</>}
                {isPending && !isExpired && isUpcoming && <><button type="button" disabled={busyId === reservation.id} onClick={() => pay(reservation)}>{busyId === reservation.id ? "Procesando…" : "Pagar"}</button>{!cancellationClosed && <button type="button" className="reservation-cancel-button" disabled={busyId === reservation.id} onClick={() => cancel(reservation)}>Cancelar</button>}</>}
                {isWaitlisted && <button type="button" className="reservation-cancel-button" disabled={busyId === reservation.id} onClick={() => cancel(reservation)}>Salir de la lista</button>}
                {isWaitlistOffered && <><button type="button" disabled={busyId === reservation.id} onClick={() => answerOffer(reservation, true)}>Aceptar lugar</button><button type="button" className="reservation-cancel-button" disabled={busyId === reservation.id} onClick={() => answerOffer(reservation, false)}>Rechazar</button></>}
            </div>
        </article>;
    }

    return <main className="registration-page form-page activity-entry-page"><button className="back-button" type="button" onClick={onBack}>← Menú</button>
        <section className="form-card reservations-page"><div className="form-header"><h1>Mis reservas</h1><p>Consultá los próximos planes y las actividades que ya hiciste.</p></div>
            {error && <div className="form-message error" role="alert">{error}</div>}
            {loading ? <p>Cargando reservas…</p> : <>
                <section className="reservation-section"><h2>Próximas reservas</h2>{upcoming.length ? <div className="reservation-list">{upcoming.map(reservationCard)}</div> : <p className="provider-empty-state">No tenés próximas reservas.</p>}</section>
                <section className="reservation-section"><div className="reservation-past-heading"><h2>Reservas pasadas</h2><label>Mostrar<select value={pastRange} onChange={event => setPastRange(event.target.value)}><option value="10d">Últimos 10 días</option><option value="1m">Último mes</option><option value="3m">Últimos 3 meses</option><option value="all">Todo el historial</option></select></label></div>
                    {past.length ? <div className="reservation-list">{past.map(reservationCard)}</div> : <p className="provider-empty-state">No hay reservas en este período.</p>}
                </section>
            </>}
        </section>
        {reviewing?.unavailable && <div className="review-modal-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) setReviewing(null); }}><section className="review-modal" role="dialog" aria-modal="true" aria-labelledby="review-unavailable-title"><button className="review-modal-close" type="button" onClick={() => setReviewing(null)} aria-label="Cerrar">×</button><h2 id="review-unavailable-title">Actividad no disponible</h2><p>“{reviewing.activityName}” ya no está publicada o no se puede abrir.</p><button className="submit-button" type="button" onClick={() => setReviewing(null)}>Entendido</button></section></div>}
        {reviewing && !reviewing.unavailable && <div className="review-modal-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) setReviewing(null); }}><section className="review-modal" role="dialog" aria-modal="true" aria-labelledby="review-modal-title"><button className="review-modal-close" type="button" onClick={() => setReviewing(null)} aria-label="Cerrar">×</button><h2 id="review-modal-title">Tu reseña</h2><p>{reviewing.activityName}</p><label className="review-rating-label">Tu puntuación<RatingStars rating={reviewRating} onChange={setReviewRating} label="Elegí una puntuación de media estrella a cinco estrellas" /><span>{reviewRating.toFixed(1)} / 5</span></label><label className="form-field">Comentario<textarea rows="5" maxLength="1500" value={reviewComment} onChange={event => setReviewComment(event.target.value)} placeholder="Contá cómo fue tu experiencia" /></label><small className="review-character-count">{reviewComment.length}/1500</small><div className="review-modal-actions"><button type="button" className="activity-step-back" onClick={() => { setReviewing(null); setError(""); }}>Cancelar</button><button type="button" className="submit-button" disabled={busyId === reviewing.id || !reviewComment.trim()} onClick={submitReview}>{busyId === reviewing.id ? "Enviando…" : "Confirmar"}</button></div></section></div>}
    </main>;
}
