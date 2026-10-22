import { useEffect, useRef, useState } from "react";
import LocationFields from "../components/LocationFields";
import { getCategories } from "../services/categoryService";
import { createActivity, updateActivity } from "../services/activityService";

const weekdayOptions = [
    ["MONDAY", "Lunes"], ["TUESDAY", "Martes"], ["WEDNESDAY", "Miércoles"],
    ["THURSDAY", "Jueves"], ["FRIDAY", "Viernes"], ["SATURDAY", "Sábado"], ["SUNDAY", "Domingo"],
];
const recurrenceOptions = [
    ["ONCE", "Una sola vez"], ["DAILY", "Diariamente"], ["WEEKLY", "Semanalmente"],
    ["BIWEEKLY", "Cada 2 semanas"], ["MONTHLY", "Mensualmente"], ["SEASONAL", "Por temporada"],
];
const formSteps = ["Propuesta", "Fotos", "Fechas y horarios", "Requisitos", "Vista previa"];
const recurrenceNames = Object.fromEntries(recurrenceOptions);
const weekdayRecurrences = ["WEEKLY", "BIWEEKLY", "SEASONAL"];

function localToday() {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

function dayAfter(value) {
    if (!value) return localToday();
    const [year, month, day] = value.split("-").map(Number);
    const next = new Date(year, month - 1, day + 1);
    return `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, "0")}-${String(next.getDate()).padStart(2, "0")}`;
}

function blankForm() {
    return {
        name: "", department: "", city: "", location: "", description: "", categoryIds: [],
        firstDate: localToday(), recurrence: "ONCE", repeatUntil: "", startTimes: ["10:00"], weekdays: [],
        capacity: "10", price: "", durationMinutes: "60", bookingCutoffHours: "0", equipment: "",
        minimumAge: "", maximumAge: "", minimumHeightCm: "", maximumHeightCm: "",
        difficulty: "", otherRequirements: "",
    };
}

function formFromActivity(activity) {
    return { ...blankForm(), ...activity, categoryIds: activity.categories.map(category => category.id),
        firstDate: activity.firstDate, repeatUntil: activity.repeatUntil || "",
        startTimes: activity.startTimes, weekdays: activity.weekdays,
        capacity: String(activity.capacity), durationMinutes: String(activity.durationMinutes),
        price: activity.price == null ? "" : String(activity.price),
        bookingCutoffHours: String(activity.bookingCutoffHours),
        minimumAge: activity.minimumAge == null ? "" : String(activity.minimumAge),
        maximumAge: activity.maximumAge == null ? "" : String(activity.maximumAge),
        minimumHeightCm: activity.minimumHeightCm == null ? "" : String(activity.minimumHeightCm),
        maximumHeightCm: activity.maximumHeightCm == null ? "" : String(activity.maximumHeightCm),
    };
}

function displayPrice(value) {
    return Number(value) === 0 ? "Gratis" : `$U ${new Intl.NumberFormat("es-UY", { maximumFractionDigits: 2 }).format(Number(value))}`;
}

export default function ActivityFormPage({ activity, onBack, onSaved }) {
    const [form, setForm] = useState(() => activity ? formFromActivity(activity) : blankForm());
    const [categories, setCategories] = useState([]);
    const [error, setError] = useState("");
    const [isSaving, setIsSaving] = useState(false);
    const [newPhotos, setNewPhotos] = useState([]);
    const newPhotosRef = useRef(newPhotos);
    newPhotosRef.current = newPhotos;
    const [keptPhotos, setKeptPhotos] = useState(activity?.photos || []);
    const [photoError, setPhotoError] = useState("");
    const [step, setStep] = useState(0);
    const [previewPhotoIndex, setPreviewPhotoIndex] = useState(0);
    const previewPhotos = [
        ...keptPhotos.map(photo => ({ src: `http://${window.location.hostname}:8080${photo.url}`, alt: photo.fileName || "Foto de la actividad" })),
        ...newPhotos.map(photo => ({ src: photo.preview, alt: photo.name })),
    ];
    const activePreviewPhoto = previewPhotos[Math.min(previewPhotoIndex, Math.max(0, previewPhotos.length - 1))];
    const selectedWeekdayNames = form.weekdays
        .map(day => weekdayOptions.find(([value]) => value === day)?.[1])
        .filter(Boolean);
    const recurrencePreview = `${recurrenceNames[form.recurrence]}${weekdayRecurrences.includes(form.recurrence) && selectedWeekdayNames.length
        ? ` · ${selectedWeekdayNames.join(", ").toLowerCase()}` : ""}`;
    const hasRequirements = Boolean(form.equipment || form.otherRequirements || Number(form.minimumAge) > 0
        || form.maximumAge || Number(form.minimumHeightCm) > 0 || form.maximumHeightCm || form.difficulty);

    useEffect(() => {
        getCategories().then(setCategories).catch(error => setError(error.message));
    }, []);

    function change(name, value) { setForm(current => ({ ...current, [name]: value })); }

    useEffect(() => () => newPhotosRef.current.forEach(photo => URL.revokeObjectURL(photo.preview)), []);

    function selectPhotos(fileList) {
        const selected = Array.from(fileList || []);
        const allowed = selected.filter(file => ["image/jpeg", "image/png", "image/webp"].includes(file.type));
        if (allowed.length !== selected.length) setPhotoError("Elegí imágenes JPG, PNG o WEBP.");
        else setPhotoError("");
        const tooLarge = allowed.some(file => file.size > 8 * 1024 * 1024);
        if (tooLarge) { setPhotoError("Cada foto puede pesar hasta 8 MB."); return; }
        const remaining = Math.max(0, 8 - keptPhotos.length - newPhotos.length);
        const accepted = allowed.slice(0, remaining).map(file => Object.assign(file, { preview: URL.createObjectURL(file) }));
        if (accepted.length < allowed.length) setPhotoError("Podés guardar hasta 8 fotos por actividad.");
        setNewPhotos(current => [...current, ...accepted]);
    }

    function removeNewPhoto(index) {
        setNewPhotos(current => {
            const removed = current[index];
            if (removed) URL.revokeObjectURL(removed.preview);
            return current.filter((_, photoIndex) => photoIndex !== index);
        });
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setError("");
        if (step === 0 && !form.categoryIds.length) { setError("Elegí al menos una categoría."); return; }
        if (step === 2) {
            if (["WEEKLY", "BIWEEKLY", "SEASONAL"].includes(form.recurrence) && !form.weekdays.length) {
                setError("Elegí al menos un día para repetir la actividad."); return;
            }
            if (form.recurrence === "SEASONAL" && !form.repeatUntil) { setError("Indicá la fecha de fin de la temporada."); return; }
            if (form.repeatUntil && form.repeatUntil <= form.firstDate) {
                setError("La fecha de fin debe ser posterior a la primera fecha."); return;
            }
            if (form.recurrence === "ONCE" && form.firstDate === localToday()
                && form.startTimes.every(time => time <= new Date().toTimeString().slice(0, 5))) {
                setError("Para una actividad de una sola vez, elegí una hora futura."); return;
            }
            if (new Set(form.startTimes).size !== form.startTimes.length) {
                setError("No repitas el mismo horario."); return;
            }
        }
        if (step === 3 && form.maximumAge && form.minimumAge && Number(form.maximumAge) < Number(form.minimumAge)) {
            setError("La edad máxima debe ser mayor o igual a la mínima."); return;
        }
        if (step === 3 && form.maximumHeightCm && form.minimumHeightCm && Number(form.maximumHeightCm) < Number(form.minimumHeightCm)) {
            setError("La altura máxima debe ser mayor o igual a la mínima."); return;
        }
        if (step < formSteps.length - 1) { setStep(current => current + 1); return; }
        setIsSaving(true);
        const request = {
            name: form.name, department: form.department, city: form.city, location: form.location,
            description: form.description,
            firstDate: form.firstDate, recurrence: form.recurrence,
            startTimes: form.startTimes, weekdays: form.weekdays,
            categoryIds: form.categoryIds.map(Number),
            repeatUntil: form.recurrence === "ONCE" || !form.repeatUntil ? null : form.repeatUntil,
            weekdays: ["WEEKLY", "BIWEEKLY", "SEASONAL"].includes(form.recurrence) ? form.weekdays : [],
            capacity: Number(form.capacity), durationMinutes: Number(form.durationMinutes),
            price: Number(form.price),
            bookingCutoffHours: Number(form.bookingCutoffHours),
            equipment: form.equipment,
            minimumAge: form.minimumAge ? Number(form.minimumAge) : null,
            maximumAge: form.maximumAge ? Number(form.maximumAge) : null,
            minimumHeightCm: form.minimumHeightCm ? Number(form.minimumHeightCm) : null,
            maximumHeightCm: form.maximumHeightCm ? Number(form.maximumHeightCm) : null,
            difficulty: form.difficulty || null,
            otherRequirements: form.otherRequirements,
        };
        try {
            const saved = activity
                ? await updateActivity(activity.id, request, newPhotos, keptPhotos.map(photo => photo.id))
                : await createActivity(request, newPhotos);
            onSaved(saved);
        } catch (saveError) { setError(saveError.message); }
        finally { setIsSaving(false); }
    }

    return (
        <main className="registration-page form-page activity-entry-page">
            <button className="back-button" type="button" onClick={onBack}>← Menú</button>
            <section className="form-card activity-form-card">
                <div className="form-header">
                    <span className="page-label">Actividades</span>
                    <h1>{activity ? "Modificar actividad" : "Agregar actividad"}</h1>
                    <p>{step === 4
                        ? "Revisá cómo quedará antes de publicarla."
                        : `Paso ${step + 1} de 4 · ${formSteps[step]}. Podés volver para hacer cambios.`}</p>
                </div>
                <nav className="activity-stepper" aria-label="Pasos del formulario">
                    {formSteps.map((label, index) => <div key={label} className={`activity-step ${index === step ? "current" : ""} ${index < step ? "completed" : ""}`}>
                        <span>{index < step ? "✓" : index + 1}</span><small>{label}</small>
                    </div>)}
                </nav>
                <form className="registration-form" onSubmit={handleSubmit}>
                    {step === 0 && <div className="activity-form-section">
                        <h2>La propuesta</h2>
                        <label className="form-field">Nombre de la actividad
                            <input maxLength={100} value={form.name} onChange={event => change("name", event.target.value)} required />
                        </label>
                        <div className="form-grid">
                            <LocationFields department={form.department} city={form.city}
                                onDepartmentChange={value => setForm(current => ({ ...current, department: value, city: "" }))}
                                onCityChange={value => change("city", value)} />
                        </div>
                        <label className="form-field">Ubicación o punto de encuentro
                            <input maxLength={180} placeholder="Dirección, establecimiento o referencia" value={form.location}
                                onChange={event => change("location", event.target.value)} required />
                        </label>
                        <label className="form-field">Descripción
                            <textarea maxLength={3000} rows={5} value={form.description}
                                onChange={event => change("description", event.target.value)} required />
                        </label>
                        <fieldset className="activity-category-picker">
                            <legend>Categorías <span>(elegí una o más)</span></legend>
                            <div className="activity-category-options">
                                {categories.map(category => <label key={category.id}>
                                    <input type="checkbox" checked={form.categoryIds.includes(category.id)}
                                        onChange={event => change("categoryIds", event.target.checked
                                            ? [...form.categoryIds, category.id]
                                            : form.categoryIds.filter(id => id !== category.id))} />
                                    {category.name}
                                </label>)}
                            </div>
                            {categories.length === 0 && <small>El administrador todavía no agregó categorías.</small>}
                        </fieldset>
                    </div>}

                    {step === 1 && <div className="activity-form-section">
                        <h2>Fotos</h2>
                        <p className="activity-photo-help">Agregá hasta 8 imágenes JPG, PNG o WEBP. Cada una puede pesar hasta 8 MB.</p>
                        <label className="activity-photo-upload">
                            <span aria-hidden="true">＋</span>
                            <strong>Elegir fotos</strong>
                            <small>{keptPhotos.length + newPhotos.length} de 8 seleccionadas</small>
                            <input type="file" accept="image/jpeg,image/png,image/webp" multiple
                                disabled={keptPhotos.length + newPhotos.length >= 8}
                                onChange={event => { selectPhotos(event.target.files); event.target.value = ""; }} />
                        </label>
                        {(keptPhotos.length > 0 || newPhotos.length > 0) && <div className="activity-photo-grid">
                            {keptPhotos.map(photo => <figure key={`saved-${photo.id}`}>
                                <img src={`http://${window.location.hostname}:8080${photo.url}`} alt={photo.fileName || "Foto de la actividad"} />
                                <button type="button" aria-label="Quitar foto" onClick={() => setKeptPhotos(current => current.filter(item => item.id !== photo.id))}>×</button>
                            </figure>)}
                            {newPhotos.map((photo, index) => <figure key={`new-${photo.preview}`}>
                                <img src={photo.preview} alt={photo.name} />
                                <button type="button" aria-label="Quitar foto" onClick={() => removeNewPhoto(index)}>×</button>
                            </figure>)}
                        </div>}
                        {photoError && <small className="activity-photo-error" role="alert">{photoError}</small>}
                    </div>}

                    {step === 2 && <div className="activity-form-section">
                        <h2>Fechas y horarios</h2>
                        <div className="form-grid">
                            <label className="form-field">Primera fecha
                                <input type="date" min={localToday()} value={form.firstDate}
                                    onChange={event => change("firstDate", event.target.value)} required />
                            </label>
                            <label className="form-field">Repetición
                                <select value={form.recurrence} onChange={event => change("recurrence", event.target.value)}>
                                    {recurrenceOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                                </select>
                            </label>
                        </div>
                        {form.recurrence !== "ONCE" && <label className="form-field">
                            {form.recurrence === "SEASONAL" ? "La temporada termina el" : "Repetir hasta (opcional)"}
                            <input type="date" min={dayAfter(form.firstDate)} value={form.repeatUntil}
                                onChange={event => change("repeatUntil", event.target.value)} required={form.recurrence === "SEASONAL"} />
                        </label>}
                        {["WEEKLY", "BIWEEKLY", "SEASONAL"].includes(form.recurrence) && <fieldset className="activity-weekday-picker">
                            <legend>{form.recurrence === "SEASONAL" ? "Días activos de la temporada" : "Días de la semana"}</legend>
                            <div className="weekday-options">{weekdayOptions.map(([value, label]) => <label key={value}>
                                <input type="checkbox" checked={form.weekdays.includes(value)} onChange={event => change("weekdays", event.target.checked
                                    ? [...form.weekdays, value] : form.weekdays.filter(day => day !== value))} />{label}
                            </label>)}</div>
                        </fieldset>}
                        <div className="activity-time-list">
                            <div className="activity-time-heading"><strong>Horarios de inicio</strong>
                                <button className="text-button" type="button" disabled={form.startTimes.length >= 8}
                                    onClick={() => change("startTimes", [...form.startTimes, "12:00"])}>+ Agregar horario</button>
                            </div>
                            {form.startTimes.map((time, index) => <div className="activity-time-row" key={index}>
                                <label className="form-field">{form.startTimes.length > 1 ? `Horario ${index + 1}` : "Hora de inicio"}
                                    <input type="time" value={time} onChange={event => change("startTimes", form.startTimes.map((value, i) => i === index ? event.target.value : value))} required />
                                </label>
                                {form.startTimes.length > 1 && <button className="text-button" type="button" aria-label={`Quitar horario ${index + 1}`}
                                    onClick={() => change("startTimes", form.startTimes.filter((_, i) => i !== index))}>Quitar</button>}
                            </div>)}
                        </div>
                        <div className="form-grid">
                            <label className="form-field">Duración (minutos)
                                <input type="number" min="1" max="1440" value={form.durationMinutes}
                                    onChange={event => change("durationMinutes", event.target.value)} required />
                            </label>
                            <label className="form-field">Cupos por horario
                                <input type="number" min="1" max="10000" value={form.capacity}
                                    onChange={event => change("capacity", event.target.value)} required />
                            </label>
                            <label className="form-field">Precio por persona (UYU)
                                <input type="number" min="0" max="99999999.99" step="0.01" value={form.price}
                                    onChange={event => change("price", event.target.value)} placeholder="0 si es gratis" required />
                            </label>
                            <label className="form-field">Cerrar reservas (horas antes)
                                <input type="number" min="0" max="720" value={form.bookingCutoffHours}
                                    onChange={event => change("bookingCutoffHours", event.target.value)} required />
                            </label>
                        </div>
                    </div>}

                    {step === 3 && <div className="activity-form-section">
                        <h2>Equipamiento y requisitos</h2>
                        <label className="form-field">Equipamiento necesario
                            <textarea rows={2} maxLength={500} value={form.equipment}
                                onChange={event => change("equipment", event.target.value)} placeholder="Por ejemplo: calzado cerrado, agua, protector solar" />
                        </label>
                        <div className="form-grid">
                            <label className="form-field">Edad mínima (años)
                                <input type="number" min="0" max="120" value={form.minimumAge}
                                    onChange={event => change("minimumAge", event.target.value)} placeholder="Sin requisito" />
                            </label>
                            <label className="form-field">Edad máxima (años)
                                <input type="number" min="0" max="120" value={form.maximumAge}
                                    onChange={event => change("maximumAge", event.target.value)} placeholder="Sin requisito" />
                            </label>
                            <label className="form-field">Altura mínima requerida (cm)
                                <input type="number" min="0" max="300" value={form.minimumHeightCm}
                                    onChange={event => change("minimumHeightCm", event.target.value)} placeholder="Sin requisito" />
                            </label>
                            <label className="form-field">Altura máxima permitida (cm)
                                <input type="number" min="0" max="300" value={form.maximumHeightCm}
                                    onChange={event => change("maximumHeightCm", event.target.value)} placeholder="Sin requisito" />
                            </label>
                            <label className="form-field">Nivel de dificultad requerido
                                <select value={form.difficulty} onChange={event => change("difficulty", event.target.value)}>
                                    <option value="">Sin nivel específico</option><option>Principiante</option><option>Intermedio</option><option>Avanzado</option>
                                </select>
                            </label>
                        </div>
                        <label className="form-field">Otros requisitos o indicaciones
                            <textarea rows={3} maxLength={1000} value={form.otherRequirements}
                                onChange={event => change("otherRequirements", event.target.value)} />
                        </label>
                    </div>}
                    {step === 4 && <section className="activity-form-section activity-preview">
                        <div className="activity-preview-heading">
                            <span>VISTA PREVIA</span>
                            <h2>Así se verá tu actividad</h2>
                            <p>Revisá la información antes de publicarla.</p>
                        </div>
                        {activePreviewPhoto && <div className="activity-preview-gallery">
                            <img src={activePreviewPhoto.src} alt={activePreviewPhoto.alt} />
                            {previewPhotos.length > 1 && <>
                                <button className="gallery-arrow previous" type="button" aria-label="Foto anterior"
                                    onClick={() => setPreviewPhotoIndex(index => (index - 1 + previewPhotos.length) % previewPhotos.length)}>‹</button>
                                <button className="gallery-arrow next" type="button" aria-label="Foto siguiente"
                                    onClick={() => setPreviewPhotoIndex(index => (index + 1) % previewPhotos.length)}>›</button>
                                <span className="gallery-counter">{Math.min(previewPhotoIndex, previewPhotos.length - 1) + 1} / {previewPhotos.length}</span>
                            </>}
                        </div>}
                        <h3>{form.name || "Nombre de la actividad"}</h3>
                        <p><strong>Ubicación:</strong> {[form.city, form.department].filter(Boolean).join(", ") || "Sin completar"} · {form.location || "Punto de encuentro"}</p>
                        <p className="activity-preview-description">{form.description || "Acá aparecerá la descripción."}</p>
                        <div className="activity-preview-tags">
                            {categories.filter(category => form.categoryIds.includes(category.id)).map(category => <span key={category.id}>{category.name}</span>)}
                        </div>
                        <div className="activity-preview-highlights">
                            <p><strong>{form.capacity || "—"}</strong><span>cupos por horario</span></p>
                            <p><strong>{form.price === "" ? "—" : displayPrice(form.price)}</strong><span>por persona</span></p>
                        </div>
                        <div className="activity-preview-grid">
                            <p><strong>Fecha:</strong> {form.firstDate || "Sin definir"}</p>
                            <p><strong>Repetición:</strong> {recurrencePreview}</p>
                            {form.repeatUntil && <p><strong>Hasta:</strong> {form.repeatUntil}</p>}
                            <p><strong>Horarios:</strong> {form.startTimes.join(" · ")}</p>
                            <p><strong>Duración:</strong> {form.durationMinutes} minutos</p>
                            <p><strong>Reservas cierran:</strong> {form.bookingCutoffHours} horas antes</p>
                        </div>
                        {hasRequirements && <div className="activity-preview-requirements">
                            <h4>Requisitos para participar</h4>
                            {Number(form.minimumAge) > 0 && <p><strong>Edad mínima:</strong> {form.minimumAge} años.</p>}
                            {form.maximumAge && <p><strong>Edad máxima:</strong> {form.maximumAge} años.</p>}
                            {Number(form.minimumHeightCm) > 0 && <p><strong>Altura mínima:</strong> {form.minimumHeightCm} cm.</p>}
                            {form.maximumHeightCm && <p><strong>Altura máxima:</strong> {form.maximumHeightCm} cm.</p>}
                            {form.difficulty && <p><strong>Nivel requerido:</strong> {form.difficulty}.</p>}
                            {form.equipment && <p><strong>Equipamiento necesario:</strong> {form.equipment}</p>}
                            {form.otherRequirements && <p><strong>Otras indicaciones:</strong> {form.otherRequirements}</p>}
                        </div>}
                    </section>}
                    {error && <div className="form-message error" role="alert">{error}</div>}
                    <div className="activity-step-actions">
                        {step > 0 && <button className="activity-step-back" type="button" onClick={() => { setError(""); setStep(current => current - 1); }}>← Volver</button>}
                        {step < formSteps.length - 1
                            ? <button className="submit-button" type="submit">Siguiente →</button>
                            : <button className="submit-button" type="submit" disabled={isSaving}>{isSaving ? "Publicando…" : activity ? "Guardar cambios" : "Publicar actividad"}</button>}
                    </div>
                </form>
            </section>
        </main>
    );
}
