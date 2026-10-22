import { useEffect, useState } from "react";
import { getPublicProvider, getActivities } from "../services/activityService";

const socials = [["Sitio web", "website"], ["Instagram", "instagram"], ["Facebook", "facebook"], ["Twitter / X", "twitter"], ["YouTube", "youtube"], ["WhatsApp", "whatsapp"]];
function externalUrl(kind, value) {
    if (kind === "website") return /^https?:\/\//i.test(value) ? value : `https://${value}`;
    if (kind === "whatsapp") return `https://wa.me/${value.replace(/\D/g, "")}`;
    if (kind === "instagram" && !value.includes("/")) return `https://instagram.com/${value.replace(/^@/, "")}`;
    if (kind === "facebook" && !value.includes("/")) return `https://facebook.com/${value.replace(/^@/, "")}`;
    if (kind === "youtube" && !value.includes("/")) return `https://youtube.com/@${value.replace(/^@/, "")}`;
    if (kind === "twitter" && !value.includes("/")) return `https://x.com/${value.replace(/^@/, "")}`;
    return /^https?:\/\//i.test(value) ? value : `https://${value}`;
}

export default function ProviderProfilePage({ providerId, onBack }) {
    const [provider, setProvider] = useState(null);
    const [activities, setActivities] = useState([]);
    const [error, setError] = useState("");
    const roundedRating = Math.round(Number(provider?.averageRating || 0) * 2) / 2;

    useEffect(() => {
        let active = true;
        Promise.all([getPublicProvider(providerId), getActivities()])
            .then(([profile, allActivities]) => {
                if (!active) return;
                setProvider(profile);
                setActivities(allActivities.filter(activity => activity.providerId === Number(providerId)));
            })
            .catch(loadError => active && setError(loadError.message));
        return () => { active = false; };
    }, [providerId]);

    return <main className="registration-page form-page activity-entry-page">
        <button className="back-button" type="button" onClick={onBack}>← Volver</button>
        <section className="form-card provider-public-card">
            {error && <div className="form-message error" role="alert">{error}</div>}
            {!provider && !error ? <p className="provider-empty-state">Cargando perfil…</p> : provider && <>
                <div className="provider-public-heading">
                    <div><span className="page-label">Prestador de Uruguay Mnatural</span><h1>{provider.displayName}</h1>
                        <p>{provider.providerType} · {provider.city}, {provider.department}</p></div>
                </div>
                <section className="provider-rating-panel" aria-label={`Promedio de ${roundedRating.toFixed(1)} sobre 5`}>
                    <div className="provider-rating-stars" aria-hidden="true">
                        {[0, 1, 2, 3, 4].map(index => {
                            const fill = Math.max(0, Math.min(1, roundedRating - index));
                            return <span className="provider-rating-star" key={index}>
                                <span className="provider-rating-empty">★</span>
                                <span className="provider-rating-fill" style={{ width: `${fill * 100}%` }}>★</span>
                            </span>;
                        })}
                    </div>
                    <strong>{roundedRating.toFixed(1)} / 5</strong>
                    <span>{provider.reviewCount} {provider.reviewCount === 1 ? "opinión" : "opiniones"}</span>
                </section>
                <section className="provider-contact-panel"><h2>Contacto y redes</h2>
                    {provider.email && <a href={`mailto:${provider.email}`}><span>Correo electrónico</span><strong>{provider.email}</strong></a>}
                    {provider.phone && <a href={`tel:${provider.phone}`}><span>Teléfono</span><strong>{provider.phone}</strong></a>}
                    {provider.address && <div className="provider-contact-detail"><span>Dirección</span><strong>{provider.address}</strong></div>}
                    {provider.city && <div className="provider-contact-detail"><span>Localidad</span><strong>{provider.city}, {provider.department}</strong></div>}
                    {socials.filter(([, key]) => provider[key]).map(([label, key]) => <a key={key} href={externalUrl(key, provider[key])} target="_blank" rel="noreferrer">
                        <span>{label}</span><strong>{key === "website" ? provider[key] : provider[key].startsWith("http") ? provider[key] : `@${provider[key].replace(/^@/, "")}`}</strong>
                    </a>)}
                    {!provider.email && !provider.phone && !provider.address && socials.every(([, key]) => !provider[key]) && <p>Este prestador todavía no agregó medios de contacto.</p>}
                </section>
                <section className="provider-public-activities"><h2>Actividades publicadas</h2>
                    {activities.length ? activities.map(activity => <div key={activity.id} className="provider-public-activity">
                        <span><strong>{activity.name}</strong><small>{activity.city} · {activity.price === 0 ? "Gratis" : `$U ${activity.price}`}</small></span>
                    </div>) : <p>Este prestador todavía no tiene actividades publicadas.</p>}
                </section>
            </>}
        </section>
    </main>;
}
