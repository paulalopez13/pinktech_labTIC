import { useEffect, useState } from "react";
import { getProviderReservations } from "../services/activityService";

const STATUS_LABELS = {
    APPROVED: "Confirmada",
    PENDING_PAYMENT: "Pago pendiente",
    REJECTED: "Pago rechazado",
};

function formatDate(value) {
    return new Date(value).toLocaleString("es-UY", { weekday: "long", day: "numeric", month: "long", hour: "2-digit", minute: "2-digit" });
}

function formatPrice(value) {
    return Number(value) === 0 ? "Gratis" : `$U ${new Intl.NumberFormat("es-UY", { maximumFractionDigits: 2 }).format(Number(value))}`;
}

// Junta las reservas de una misma salida (misma actividad, mismo día y hora) para mostrarlas juntas.
function groupByOccurrence(reservations) {
    const groups = [];
    reservations.forEach(reservation => {
        const key = `${reservation.activityId}-${reservation.activityStart}`;
        let group = groups.find(item => item.key === key);
        if (!group) {
            group = { key, activityName: reservation.activityName, activityStart: reservation.activityStart, reservations: [] };
            groups.push(group);
        }
        group.reservations.push(reservation);
    });
    return groups;
}

export default function ProviderReservationsPage({ onBack }) {
    const [reservations, setReservations] = useState([]);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(true);
    const [when, setWhen] = useState("upcoming");
    const [activityId, setActivityId] = useState("");
    // Se toma la hora al abrir la página para separar próximas de pasadas.
    const [now] = useState(() => new Date());

    useEffect(() => {
        let active = true;
        getProviderReservations().then(items => active && setReservations(items))
            .catch(loadError => active && setError(loadError.message))
            .finally(() => active && setLoading(false));
        return () => { active = false; };
    }, []);

    // Una opción por actividad que tenga reservas, para el filtro.
    const activities = [...new Map(reservations.map(item => [item.activityId, item.activityName])).entries()];

    const filtered = reservations
        .filter(item => when === "upcoming" ? new Date(item.activityStart) >= now : new Date(item.activityStart) < now)
        .filter(item => activityId === "" || String(item.activityId) === activityId);
    // Las próximas van de la más cercana a la más lejana; las pasadas, de la más reciente a la más vieja.
    const groups = groupByOccurrence(when === "upcoming" ? filtered : [...filtered].reverse());

    return <main className="registration-page form-page activity-entry-page"><button className="back-button" type="button" onClick={onBack}>← Menú</button>
        <section className="form-card reservations-page">
            <div className="form-header"><h1>Reservas recibidas</h1><p>Consultá quién reservó tus actividades.</p></div>
            {error && <div className="form-message error" role="alert">{error}</div>}
            {!loading && reservations.length > 0 && <div className="provider-reservation-filters">
                <div className="provider-reservation-tabs" role="group" aria-label="Fecha de las reservas">
                    <button type="button" className={when === "upcoming" ? "selected" : ""} onClick={() => setWhen("upcoming")}>Próximas</button>
                    <button type="button" className={when === "past" ? "selected" : ""} onClick={() => setWhen("past")}>Pasadas</button>
                </div>
                <select value={activityId} onChange={event => setActivityId(event.target.value)} aria-label="Actividad">
                    <option value="">Todas las actividades</option>
                    {activities.map(([id, name]) => <option key={id} value={id}>{name}</option>)}
                </select>
            </div>}
            {loading ? <p>Cargando reservas…</p>
                : reservations.length === 0 ? <p className="provider-empty-state">Todavía no recibiste reservas.</p>
                : groups.length === 0 ? <p className="provider-empty-state">No hay reservas {when === "upcoming" ? "próximas" : "pasadas"} para mostrar.</p>
                : <div className="reservation-list">{groups.map(group => {
                    const confirmedPeople = group.reservations.filter(item => item.paymentStatus === "APPROVED")
                        .reduce((total, item) => total + item.participantCount, 0);
                    return <article className="provider-reservation-group" key={group.key}>
                        <header><div><h2>{group.activityName}</h2><p>{formatDate(group.activityStart)}</p></div>
                            <strong>{confirmedPeople} {confirmedPeople === 1 ? "persona confirmada" : "personas confirmadas"}</strong></header>
                        {group.reservations.map(reservation => <div className="provider-reservation" key={reservation.id}>
                            <div className="provider-reservation-contact">
                                <div><strong>{reservation.contactName}</strong><span>{reservation.contactEmail} · {reservation.contactPhone}</span></div>
                                <div className="reservation-card-meta"><span>{STATUS_LABELS[reservation.paymentStatus] || reservation.paymentStatus}</span>
                                    <span>{reservation.participantCount} {reservation.participantCount === 1 ? "participante" : "participantes"} · {formatPrice(reservation.total)}</span></div>
                            </div>
                            {reservation.participants.length > 0 && <div className="participant-table-wrapper"><table className="participant-table">
                                <thead><tr><th>Nombre</th><th>Documento</th><th>Edad</th><th>Altura</th><th>Nivel</th></tr></thead>
                                <tbody>{reservation.participants.map((person, index) => <tr key={index}>
                                    <td>{person.fullName}</td>
                                    <td>{person.documentType === "PASSPORT" ? "Pasaporte" : "CI"} {person.documentNumber}</td>
                                    <td>{person.age}</td>
                                    <td>{person.heightCm ? `${person.heightCm} cm` : "—"}</td>
                                    <td>{person.level || "—"}</td>
                                </tr>)}</tbody>
                            </table></div>}
                        </div>)}
                    </article>;
                })}</div>}
        </section>
    </main>;
}
