import { useEffect, useState } from "react";
import { getMyReservations } from "../services/activityService";

export default function MyReservationsPage({ onBack }) {
    const [reservations, setReservations] = useState([]);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(true);
    useEffect(() => {
        let active = true;
        getMyReservations().then(items => active && setReservations(items))
            .catch(loadError => active && setError(loadError.message))
            .finally(() => active && setLoading(false));
        return () => { active = false; };
    }, []);
    return <main className="registration-page form-page activity-entry-page"><button className="back-button" type="button" onClick={onBack}>← Menú</button>
        <section className="form-card reservations-page"><div className="form-header"><h1>Mis reservas</h1><p>Consultá las actividades que reservaste.</p></div>
            {error && <div className="form-message error" role="alert">{error}</div>}
            {loading ? <p>Cargando reservas…</p> : reservations.length === 0 ? <p className="provider-empty-state">Todavía no tenés reservas.</p> : <div className="reservation-list">{reservations.map(reservation => <article className="reservation-summary-card" key={reservation.id}>
                <div><h2>{reservation.activityName}</h2><p>{reservation.city}, {reservation.department} · {reservation.location}</p><p>{new Date(reservation.activityStart).toLocaleString("es-UY", { dateStyle: "medium", timeStyle: "short" })}</p></div>
                <div className="reservation-card-meta"><strong>{reservation.participantCount} {reservation.participantCount === 1 ? "participante" : "participantes"}</strong><strong>{reservation.total === 0 ? "Gratis" : `$U ${new Intl.NumberFormat("es-UY", { maximumFractionDigits: 2 }).format(Number(reservation.total))}`}</strong><span>{reservation.paymentStatus === "APPROVED" ? "Confirmada" : reservation.paymentStatus}</span></div>
            </article>)}</div>}
        </section>
    </main>;
}
