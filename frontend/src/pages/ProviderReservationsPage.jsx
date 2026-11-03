import { useEffect, useState } from "react";
import { getProviderReservations } from "../services/activityService";

const STATUS_LABELS = {
    APPROVED: "Confirmada",
    PENDING_PAYMENT: "Pago pendiente",
    REJECTED: "Pago rechazado",
    WAITLISTED: "En lista de espera",
    WAITLIST_OFFERED: "Lugar ofrecido",
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
    const [activityDate, setActivityDate] = useState("");
    const [activityTime, setActivityTime] = useState("");
    const [calendarMonth, setCalendarMonth] = useState(() => new Date(new Date().getFullYear(), new Date().getMonth(), 1));
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

    const byActivityAndRange = reservations
        .filter(item => when === "upcoming" ? new Date(item.activityStart) >= now : new Date(item.activityStart) < now)
        .filter(item => activityId === "" || String(item.activityId) === activityId);
    const dates = [...new Set(byActivityAndRange.map(item => item.activityStart.slice(0, 10)))].sort();
    const datesInMonth = new Set(dates.filter(date => {
        const parsed = new Date(`${date}T12:00:00`);
        return parsed.getFullYear() === calendarMonth.getFullYear() && parsed.getMonth() === calendarMonth.getMonth();
    }));
    const calendarOffset = (new Date(calendarMonth.getFullYear(), calendarMonth.getMonth(), 1).getDay() + 6) % 7;
    const calendarDays = new Date(calendarMonth.getFullYear(), calendarMonth.getMonth() + 1, 0).getDate();
    const times = [...new Map(byActivityAndRange.filter(item => !activityDate || item.activityStart.startsWith(activityDate))
        .map(item => [item.activityStart, item.activityStart])).keys()].sort();
    const filtered = byActivityAndRange
        .filter(item => !activityDate || item.activityStart.startsWith(activityDate))
        .filter(item => !activityTime || item.activityStart === activityTime);
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
                <select value={activityId} onChange={event => { setActivityId(event.target.value); setActivityDate(""); setActivityTime(""); }} aria-label="Actividad">
                    <option value="">Todas las actividades</option>
                    {activities.map(([id, name]) => <option key={id} value={id}>{name}</option>)}
                </select>
                <details className="provider-date-picker" aria-disabled={!activityId}>
                    <summary>{!activityId ? "Elegí una actividad" : activityDate ? new Date(`${activityDate}T12:00:00`).toLocaleDateString("es-UY", { dateStyle: "medium" }) : "Elegir fecha"}</summary>
                    {activityId && <div className="provider-date-calendar"><header><button type="button" aria-label="Mes anterior" onClick={() => setCalendarMonth(value => new Date(value.getFullYear(), value.getMonth() - 1, 1))}>‹</button><strong>{calendarMonth.toLocaleDateString("es-UY", { month: "long", year: "numeric" })}</strong><button type="button" aria-label="Mes siguiente" onClick={() => setCalendarMonth(value => new Date(value.getFullYear(), value.getMonth() + 1, 1))}>›</button></header>
                        <div className="provider-date-grid">{["L", "M", "M", "J", "V", "S", "D"].map((day, index) => <span key={`${day}-${index}`}>{day}</span>)}
                            {Array.from({ length: calendarOffset }, (_, index) => <i key={`blank-${index}`} />)}
                            {Array.from({ length: calendarDays }, (_, index) => { const day = index + 1; const value = `${calendarMonth.getFullYear()}-${String(calendarMonth.getMonth() + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`; const enabled = datesInMonth.has(value); return <button type="button" key={value} disabled={!enabled} className={`${enabled ? "has-reservations" : ""} ${activityDate === value ? "selected" : ""}`} onClick={() => { setActivityDate(value); setActivityTime(""); }}>{day}</button>; })}</div>
                        <button type="button" className="provider-date-clear" onClick={() => { setActivityDate(""); setActivityTime(""); }}>Todas las fechas</button>
                    </div>}
                </details>
                <select value={activityTime} disabled={!activityId || !activityDate} onChange={event => setActivityTime(event.target.value)} aria-label="Horario de la actividad">
                    <option value="">Todos los horarios</option>
                    {times.map(time => <option key={time} value={time}>{new Date(time).toLocaleTimeString("es-UY", { hour: "2-digit", minute: "2-digit" })}</option>)}
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
