import { useEffect, useMemo, useState } from "react";
import { getActivityAvailability, reserveActivitySandbox } from "../services/activityService";
import DocumentInput from "./DocumentInput";
import PhoneInput, { phoneCountries } from "./PhoneInput";

const dateKey = value => value.slice(0, 10);
const dateLabel = value => new Date(`${value}T12:00:00`).toLocaleDateString("es-UY", { dateStyle: "full" });
const levels = ["Principiante", "Intermedio", "Avanzado"];
const emptyPerson = () => ({ fullName: "", documentType: "CI", documentNumber: "", age: "", heightCm: "", level: "" });

export default function ActivityReservationWizard({ activity, currentUser, onBack, onReserved }) {
    const [step, setStep] = useState(0);
    const [count, setCount] = useState(1);
    const [availability, setAvailability] = useState({});
    const [loading, setLoading] = useState(false);
    const [selectedDate, setSelectedDate] = useState("");
    const [selectedTime, setSelectedTime] = useState("");
    const [month, setMonth] = useState(() => {
        const first = activity.upcomingOccurrences?.[0];
        return first ? new Date(`${dateKey(first)}T12:00:00`) : new Date();
    });
    const [contact, setContact] = useState({ email: currentUser?.email || "", phone: "" });
    const [reservator, setReservator] = useState(emptyPerson);
    const [companions, setCompanions] = useState([]);
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);
    const occurrences = activity.upcomingOccurrences || [];
    const daySlots = useMemo(() => {
        const map = new Map();
        occurrences.forEach(slot => {
            const day = dateKey(slot);
            map.set(day, [...(map.get(day) || []), slot]);
        });
        return map;
    }, [occurrences]);

    useEffect(() => {
        let active = true;
        setLoading(true);
        Promise.all(occurrences.map(async slot => {
            try { return [slot, (await getActivityAvailability(activity.id, slot)).remaining]; }
            catch { return [slot, 0]; }
        })).then(entries => { if (active) setAvailability(Object.fromEntries(entries)); })
            .finally(() => active && setLoading(false));
        return () => { active = false; };
    }, [activity.id, count, occurrences]);

    const availableDates = useMemo(() => [...daySlots]
        .filter(([, slots]) => slots.some(slot => Number(availability[slot] || 0) >= count)).map(([day]) => day),
    [daySlots, availability, count]);
    const slotsForDate = (daySlots.get(selectedDate) || []).filter(slot => Number(availability[slot] || 0) >= count);
    const canHeight = Number(activity.minimumHeightCm) > 0 || activity.maximumHeightCm != null;
    const canLevel = Boolean(activity.difficulty);
    const priceTotal = (Number(activity.price) || 0) * count;
    const allPeople = [reservator, ...companions];
    const steps = ["Fecha y horario", "Datos del grupo", "Pago de prueba"];
    const startDay = new Date(month.getFullYear(), month.getMonth(), 1);
    const offset = (startDay.getDay() + 6) % 7;
    const daysInMonth = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
    const monthName = month.toLocaleDateString("es-UY", { month: "long", year: "numeric" });

    function phoneIsComplete(value) {
        const digits = value.replace(/\D/g, "");
        const country = [...phoneCountries].sort((a, b) => b.code.length - a.code.length).find(item => digits.startsWith(item.code));
        return Boolean(country && digits.slice(country.code.length).length === country.digits);
    }

    function personIsValid(person, index) {
        if (!person.fullName.trim()) return `Completá el nombre de ${index === 0 ? "quien reserva" : `la persona ${index + 1}`}.`;
        if (person.documentType === "CI" ? !/^\d{8}$/.test(person.documentNumber) : !/^[A-Z0-9]{6,9}$/.test(person.documentNumber))
            return `Revisá el documento de ${index === 0 ? "quien reserva" : `la persona ${index + 1}`}.`;
        if (person.age === "" || Number(person.age) < 0 || Number(person.age) > 120) return `Ingresá una edad válida para ${index === 0 ? "quien reserva" : `la persona ${index + 1}`}.`;
        if (activity.minimumAge != null && Number(person.age) < activity.minimumAge) return `La persona ${index + 1} no cumple la edad mínima de ${activity.minimumAge} años.`;
        if (activity.maximumAge != null && Number(person.age) > activity.maximumAge) return `La persona ${index + 1} supera la edad máxima de ${activity.maximumAge} años.`;
        if (canHeight && (person.heightCm === "" || Number(person.heightCm) < 30 || Number(person.heightCm) > 250)) return `Ingresá la altura de la persona ${index + 1}.`;
        if (activity.minimumHeightCm > 0 && Number(person.heightCm) < activity.minimumHeightCm) return `La persona ${index + 1} no cumple la altura mínima de ${activity.minimumHeightCm} cm.`;
        if (activity.maximumHeightCm != null && Number(person.heightCm) > activity.maximumHeightCm) return `La persona ${index + 1} supera la altura máxima de ${activity.maximumHeightCm} cm.`;
        if (canLevel && !person.level) return `Elegí el nivel de experiencia de la persona ${index + 1}.`;
        const levelValue = { Principiante: 1, Intermedio: 2, Avanzado: 3 };
        if (canLevel && levelValue[person.level] < levelValue[activity.difficulty]) return `La persona ${index + 1} no tiene el nivel requerido (${activity.difficulty}).`;
        return "";
    }

    function next() {
        setError("");
        if (step === 0) {
            if (!Number.isInteger(count) || count < 1 || count > activity.capacity) return setError(`Elegí entre 1 y ${activity.capacity} participantes.`);
            if (!selectedDate || !selectedTime) return setError("Elegí una fecha y un horario disponible para todo el grupo.");
        }
        if (step === 1) {
            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(contact.email)) return setError("Ingresá un correo válido.");
            if (!phoneIsComplete(contact.phone)) return setError("Completá todos los dígitos del teléfono según el país elegido.");
            const invalidPerson = allPeople.map(personIsValid).find(Boolean);
            if (invalidPerson) return setError(invalidPerson);
        }
        setStep(value => Math.min(2, value + 1));
    }

    async function payInSandbox() {
        setBusy(true); setError("");
        try {
            const result = await reserveActivitySandbox(activity.id, {
                activityStart: selectedTime,
                contactName: reservator.fullName.trim(), contactEmail: contact.email.trim(), contactPhone: contact.phone,
                participants: allPeople.map(person => ({ ...person, fullName: person.fullName.trim(), documentNumber: person.documentNumber.trim().toUpperCase(),
                    age: Number(person.age), heightCm: person.heightCm === "" ? null : Number(person.heightCm), level: person.level || null }))
            });
            onReserved?.(result, contact.email.trim());
        } catch (payError) { setError(payError.message); }
        finally { setBusy(false); }
    }

    function updateCount(event) {
        const value = Math.max(1, Math.min(activity.capacity, Number(event.target.value) || 1));
        setCount(value);
        setCompanions(current => Array.from({ length: value - 1 }, (_, index) => current[index] || emptyPerson()));
        setSelectedDate(""); setSelectedTime("");
    }

    function chooseDay(day) {
        setSelectedDate(day);
        const firstAvailable = (daySlots.get(day) || []).find(slot => Number(availability[slot] || 0) >= count);
        setSelectedTime(firstAvailable || "");
    }

    function changePerson(index, changes) {
        if (index === 0) setReservator(current => ({ ...current, ...changes }));
        else setCompanions(current => current.map((person, i) => i === index - 1 ? { ...person, ...changes } : person));
    }

    function personFields(person, index) {
        return <fieldset className="reservation-participant" key={index}>
            <legend>{index === 0 ? "Quien realiza la reserva" : `Participante ${index + 1}`}</legend>
            <div className="reservation-fields">
                <label className="form-field">Nombre completo<input value={person.fullName} onChange={event => changePerson(index, { fullName: event.target.value })} autoComplete={index === 0 ? "name" : "off"} /></label>
                <DocumentInput documentType={person.documentType} documentNumber={person.documentNumber}
                    onTypeChange={documentType => changePerson(index, { documentType, documentNumber: "" })}
                    onNumberChange={documentNumber => changePerson(index, { documentNumber })} />
                <label className="form-field">Edad<input type="number" min="0" max="120" value={person.age} onChange={event => changePerson(index, { age: event.target.value })} /></label>
                {canHeight && <label className="form-field">Altura (cm)<input type="number" min="30" max="250" value={person.heightCm} onChange={event => changePerson(index, { heightCm: event.target.value })} /></label>}
                {canLevel && <label className="form-field">Experiencia<select value={person.level} onChange={event => changePerson(index, { level: event.target.value })}><option value="">Elegí un nivel</option>{levels.map(level => <option key={level}>{level}</option>)}</select></label>}
            </div>
        </fieldset>;
    }

    return <section className="reservation-wizard form-card">
        <button className="back-button" type="button" onClick={onBack}>← Volver a la actividad</button>
        <div className="form-header"><h1>Reservar {activity.name}</h1><p>Completá cada paso para guardar tu lugar.</p></div>
        <div className="activity-stepper" aria-label="Pasos de reserva">{steps.map((label, index) => <div key={label} className={`activity-step ${step === index ? "current" : step > index ? "completed" : ""}`}><span>{index + 1}</span><small>{label}</small></div>)}</div>

        {step === 0 && <section className="reservation-step"><h2>Elegí fecha y horario</h2>
            <label className="form-field reservation-count-field">Cantidad de participantes<input type="number" min="1" max={activity.capacity} value={count} onChange={updateCount} /></label>
            <p className="reservation-calendar-hint">Los días verdes tienen cupos disponibles para el grupo.</p>
            <div className="reservation-calendar"><div className="reservation-calendar-head"><button type="button" aria-label="Mes anterior" onClick={() => setMonth(value => new Date(value.getFullYear(), value.getMonth() - 1, 1))}>‹</button><strong>{monthName}</strong><button type="button" aria-label="Mes siguiente" onClick={() => setMonth(value => new Date(value.getFullYear(), value.getMonth() + 1, 1))}>›</button></div>
                <div className="reservation-calendar-grid">{["L", "M", "M", "J", "V", "S", "D"].map((day, index) => <span className="weekday" key={`${day}-${index}`}>{day}</span>)}
                    {Array.from({ length: offset }, (_, index) => <span key={`empty-${index}`} />)}
                    {Array.from({ length: daysInMonth }, (_, index) => { const date = new Date(month.getFullYear(), month.getMonth(), index + 1); const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`; const available = availableDates.includes(key); return <button type="button" key={key} disabled={!available || loading} className={`${available ? "available" : ""} ${selectedDate === key ? "selected" : ""}`} onClick={() => chooseDay(key)} aria-label={`${key}${available ? ", disponible" : ", sin disponibilidad"}`}>{date.getDate()}</button>; })}</div>
                {loading && <small>Consultando cupos…</small>}
            </div>
            {selectedDate && <div className="reservation-time-picker"><h3>Horarios disponibles · {dateLabel(selectedDate)}</h3><div className="reservation-slot-list">{slotsForDate.map(slot => <button type="button" key={slot} className={selectedTime === slot ? "selected" : ""} onClick={() => setSelectedTime(slot)}>{new Date(slot).toLocaleTimeString("es-UY", { hour: "2-digit", minute: "2-digit" })} · {availability[slot]} cupos</button>)}</div>{slotsForDate.length === 0 && <p>No quedan horarios con cupos suficientes para este grupo.</p>}</div>}
        </section>}

        {step === 1 && <section className="reservation-step"><h2>Datos del grupo</h2><p>Completá los datos de quien reserva y de cada participante.</p>
            <div className="reservation-contact-fields"><label className="form-field">Correo electrónico<input type="email" value={contact.email} onChange={event => setContact({ ...contact, email: event.target.value })} autoComplete="email" /></label><PhoneInput value={contact.phone} onChange={phone => setContact({ ...contact, phone })} /></div>
            {(activity.minimumAge != null || activity.maximumAge != null || canHeight || canLevel || activity.equipment || activity.otherRequirements) && <div className="reservation-requirements"><strong>Requisitos de esta actividad</strong>
                {activity.minimumAge != null && <span>Edad mínima: {activity.minimumAge} años</span>}{activity.maximumAge != null && <span>Edad máxima: {activity.maximumAge} años</span>}
                {activity.minimumHeightCm > 0 && <span>Altura mínima: {activity.minimumHeightCm} cm</span>}{activity.maximumHeightCm != null && <span>Altura máxima: {activity.maximumHeightCm} cm</span>}
                {canLevel && <span>Nivel de experiencia: {activity.difficulty}</span>}{activity.equipment && <span>Equipamiento: {activity.equipment}</span>}{activity.otherRequirements && <span>Otras indicaciones: {activity.otherRequirements}</span>}
            </div>}
            <div className="reservation-participant-list">{allPeople.map(personFields)}</div>
            <small className="reservation-disclaimer">La empresa no se hace responsable si, al llegar, algún participante no cumple los requisitos indicados para esta actividad.</small>
        </section>}

        {step === 2 && <section className="reservation-step"><h2>Resumen y pago de prueba</h2><div className="reservation-summary"><p><span>Actividad</span><strong>{activity.name}</strong></p><p><span>Fecha y hora</span><strong>{dateLabel(selectedDate)} · {new Date(selectedTime).toLocaleTimeString("es-UY", { hour: "2-digit", minute: "2-digit" })}</strong></p><p><span>Participantes</span><strong>{count}</strong></p><p><span>Precio por persona</span><strong>{priceText(activity.price)}</strong></p><p className="total"><span>Total</span><strong>{priceText(priceTotal)}</strong></p></div><div className="form-message info sandbox-note">Entorno sandbox de la aplicación: este botón simula un pago aprobado. No se solicitan datos de tarjeta ni se realiza un cobro real.</div><button className="submit-button" type="button" disabled={busy} onClick={payInSandbox}>{busy ? "Procesando prueba…" : "Confirmar reserva"}</button></section>}

        {error && <div className="form-message error" role="alert">{error}</div>}
        <div className="activity-step-actions">{step > 0 && <button className="activity-step-back" type="button" onClick={() => { setError(""); setStep(value => value - 1); }}>Volver</button>}{step < 2 && <button className="submit-button" type="button" onClick={next}>Siguiente</button>}</div>
    </section>;
}

function priceText(value) { return Number(value) === 0 ? "Gratis" : `$U ${new Intl.NumberFormat("es-UY", { maximumFractionDigits: 2 }).format(Number(value))}`; }
