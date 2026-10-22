import { useEffect, useMemo, useState } from "react";
import { getActivities, getMyActivities, getFavoriteActivities, getFavoriteActivityIds,
    saveFavoriteActivity, removeFavoriteActivity } from "../services/activityService";
import ActivityDetailPage from "./ActivityDetailPage";
import ProviderProfilePage from "./ProviderProfilePage";
import BrandLogo from "../components/BrandLogo";

const weekdayNames = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
const localDate = value => new Date(`${value}T12:00:00`);
function happensOn(activity, dateText) {
    const date = localDate(dateText), first = localDate(activity.firstDate);
    if (date < first || (activity.repeatUntil && date > localDate(activity.repeatUntil))) return false;
    const day = weekdayNames[date.getDay()];
    const days = activity.weekdays?.length ? activity.weekdays : [weekdayNames[first.getDay()]];
    switch (activity.recurrence) {
        case "ONCE": return dateText === activity.firstDate;
        case "DAILY": return true;
        case "WEEKLY": return days.includes(day);
        case "BIWEEKLY": {
            const monday = value => { const d = new Date(value); d.setDate(d.getDate() - ((d.getDay() + 6) % 7)); return d; };
            const weeks = Math.floor((monday(date) - monday(first)) / (7 * 86400000));
            return days.includes(day) && weeks % 2 === 0;
        }
        case "MONTHLY": return date.getDate() === first.getDate();
        case "SEASONAL": return days.includes(day);
        default: return false;
    }
}
const price = value => Number(value) === 0 ? "Gratis" : `$U ${new Intl.NumberFormat("es-UY", { maximumFractionDigits: 2 }).format(Number(value))}`;

export default function ActivitiesPage({ onBack, currentUser, onLogin, onReserved, onEdit, onAddActivity, onExploreCategory, initialCategory = "", initialActivityId = null, mode = "explore" }) {
    const [activities, setActivities] = useState([]);
    const [favoriteIds, setFavoriteIds] = useState(new Set());
    const [search, setSearch] = useState("");
    const [category, setCategory] = useState(initialCategory);
    const [department, setDepartment] = useState("");
    const [city, setCity] = useState("");
    const [minPrice, setMinPrice] = useState("");
    const [maxPrice, setMaxPrice] = useState("");
    const [desiredDate, setDesiredDate] = useState("");
    const [sort, setSort] = useState("popular");
    const [hoverMenu, setHoverMenu] = useState("");
    const [clickedMenu, setClickedMenu] = useState("");
    const openMenu = clickedMenu || hoverMenu;
    const [selectedId, setSelectedId] = useState(initialActivityId);
    const [selectedProviderId, setSelectedProviderId] = useState(null);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");
    const [loading, setLoading] = useState(true);
    const isMine = mode === "mine", isFavorites = mode === "favorites";

    useEffect(() => {
        if (!openMenu) return undefined;
        const closeOnEscape = event => { if (event.key === "Escape") { setClickedMenu(""); setHoverMenu(""); } };
        window.addEventListener("keydown", closeOnEscape);
        return () => window.removeEventListener("keydown", closeOnEscape);
    }, [openMenu]);

    useEffect(() => {
        let active = true;
        (isMine ? getMyActivities() : isFavorites ? getFavoriteActivities() : getActivities())
            .then(data => { if (active) setActivities(data); })
            .catch(loadError => { if (active) setError(loadError.message); })
            .finally(() => { if (active) setLoading(false); });
        if (currentUser?.role === "TOURIST") {
            getFavoriteActivityIds().then(ids => active && setFavoriteIds(new Set(ids))).catch(() => {});
        }
        return () => { active = false; };
    }, [isMine, isFavorites, currentUser?.role]);

    const categoryNames = useMemo(() => [...new Set(activities.flatMap(activity => activity.categories.map(item => item.name)))].sort((a, b) => a.localeCompare(b, "es")), [activities]);
    const departments = useMemo(() => [...new Set(activities.map(activity => activity.department).filter(Boolean))].sort((a, b) => a.localeCompare(b, "es")), [activities]);
    const cities = useMemo(() => [...new Set(activities.filter(activity => !department || activity.department === department).map(activity => activity.city).filter(Boolean))].sort((a, b) => a.localeCompare(b, "es")), [activities, department]);
    const visible = useMemo(() => {
        const term = search.trim().toLocaleLowerCase("es");
        const filtered = activities.filter(activity => (!term || activity.name.toLocaleLowerCase("es").includes(term))
            && (!category || activity.categories.some(item => item.name === category))
            && (!department || activity.department === department)
            && (!city || activity.city === city)
            && (minPrice === "" || Number(activity.price) >= Number(minPrice))
            && (maxPrice === "" || Number(activity.price) <= Number(maxPrice))
            && (!desiredDate || happensOn(activity, desiredDate)));
        const hasRecentReservations = filtered.some(activity => activity.recentReservationCount > 0);
        return [...filtered].sort((a, b) => {
            if (sort === "name") return a.name.localeCompare(b.name, "es");
            if (sort === "priceAsc") return Number(a.price) - Number(b.price) || a.name.localeCompare(b.name, "es");
            if (sort === "priceDesc") return Number(b.price) - Number(a.price) || a.name.localeCompare(b.name, "es");
            if (sort === "rating" || (sort === "popular" && !hasRecentReservations)) return b.averageRating - a.averageRating || b.reviewCount - a.reviewCount;
            return b.recentReservationCount - a.recentReservationCount || b.averageRating - a.averageRating;
        });
    }, [activities, search, category, department, city, minPrice, maxPrice, desiredDate, sort]);

    const selected = activities.find(activity => activity.id === selectedId);
    async function toggleFavorite(event, activity) {
        event.stopPropagation();
        if (currentUser?.role !== "TOURIST") { onLogin?.(activity.id); return; }
        const isSaved = favoriteIds.has(activity.id);
        setError("");
        try {
            if (isSaved) await removeFavoriteActivity(activity.id); else await saveFavoriteActivity(activity.id);
            setFavoriteIds(current => { const next = new Set(current); isSaved ? next.delete(activity.id) : next.add(activity.id); return next; });
            if (isFavorites && isSaved) setActivities(items => items.filter(item => item.id !== activity.id));
        } catch (toggleError) { setError(toggleError.message); }
    }

    if (selectedProviderId) return <ProviderProfilePage providerId={selectedProviderId} currentUser={currentUser} onLogin={onLogin}
        onBack={() => setSelectedProviderId(null)} />;
    if (selected) return <ActivityDetailPage activity={selected} currentUser={currentUser}
        isOwner={isMine} favorite={favoriteIds.has(selected.id)} onToggleFavorite={event => toggleFavorite(event, selected)}
        onProviderClick={() => selected.providerId && setSelectedProviderId(selected.providerId)}
        onBack={() => setSelectedId(null)} onLogin={() => onLogin?.(selected.id)}
        onCategoryClick={name => { if (isMine) onExploreCategory?.(name); else { setCategory(name); setSelectedId(null); } }}
        onEdit={onEdit} onAddActivity={onAddActivity}
        onReserved={message => { setNotice(message); setSelectedId(null); }} />;

    return <main className="registration-page form-page activity-entry-page">
        <button className="back-button" type="button" onClick={onBack}>← Menú</button>
        <section className="form-card activities-catalog">
            <div className="form-header"><h1>{isMine ? "Mis actividades" : isFavorites ? "Mis favoritos" : "Explorá actividades"}</h1>
                <p>{isMine ? "Consultá y gestioná las actividades que publicaste." : isFavorites ? "Tus actividades guardadas." : "Encontrá tu próxima actividad."}</p></div>
            {notice && <div className="form-message success" role="status">{notice}</div>}
            {error && <div className="form-message error" role="alert">{error}</div>}
            {isMine && <button className="submit-button my-activity-add" type="button" onClick={onAddActivity}>Agregar actividad</button>}
            {!isMine && <div className="activity-toolbar">
                <input className="activity-search" type="search" value={search} onChange={event => setSearch(event.target.value)} placeholder="Buscar actividad" aria-label="Buscar actividad por nombre" />
                <div className="activity-menu-wrap" onMouseEnter={() => setHoverMenu("filters")} onMouseLeave={() => setHoverMenu("")}>
                    <button className={`activity-menu-button ${openMenu === "filters" ? "active" : ""}`} type="button" aria-expanded={openMenu === "filters"} aria-controls="activity-filter-menu" onClick={() => setClickedMenu(clickedMenu === "filters" ? "" : "filters")}>Filtrar <span aria-hidden="true">⌄</span></button>
                    {openMenu === "filters" && <div className="activity-popover activity-filter-popover" id="activity-filter-menu" role="group" aria-label="Filtros de actividades">
                        <label>Categoría<select value={category} onChange={event => setCategory(event.target.value)}><option value="">Todas</option>{categoryNames.map(name => <option key={name}>{name}</option>)}</select></label>
                        <label>Departamento<select value={department} onChange={event => { setDepartment(event.target.value); setCity(""); }}><option value="">Todos los departamentos</option>{departments.map(name => <option key={name}>{name}</option>)}</select></label>
                        <label>Ciudad<select value={city} onChange={event => setCity(event.target.value)} disabled={!department}><option value="">{department ? "Todas las ciudades" : "Elegí primero un departamento"}</option>{cities.map(name => <option key={name}>{name}</option>)}</select></label>
                        <div className="activity-price-filter"><label>Precio desde<input type="number" min="0" step="any" value={minPrice} onChange={event => setMinPrice(event.target.value)} /></label>
                            <label>Precio hasta<input type="number" min="0" step="any" value={maxPrice} onChange={event => setMaxPrice(event.target.value)} /></label></div>
                        <label>Fecha deseada<input type="date" value={desiredDate} onChange={event => setDesiredDate(event.target.value)} /></label>
                        <button className="activity-clear-filters" type="button" onClick={() => { setCategory(""); setDepartment(""); setCity(""); setMinPrice(""); setMaxPrice(""); setDesiredDate(""); }}>Limpiar filtros</button>
                    </div>}
                </div>
                <div className="activity-menu-wrap" onMouseEnter={() => setHoverMenu("sort")} onMouseLeave={() => setHoverMenu("")}>
                    <button className={`activity-menu-button ${openMenu === "sort" ? "active" : ""}`} type="button" aria-expanded={openMenu === "sort"} aria-controls="activity-sort-menu" onClick={() => setClickedMenu(clickedMenu === "sort" ? "" : "sort")}>Ordenar <span aria-hidden="true">⌄</span></button>
                    {openMenu === "sort" && <div className="activity-popover activity-sort-popover" id="activity-sort-menu" role="group" aria-label="Ordenar actividades">
                        {[ ["popular", "Más populares"], ["rating", "Mejor valoradas"], ["name", "Alfabéticamente"], ["priceAsc", "Precio: menor a mayor"], ["priceDesc", "Precio: mayor a menor"] ].map(([value, label]) => <button key={value} type="button" className={sort === value ? "selected" : ""} onClick={() => { setSort(value); setClickedMenu(""); }}>{label}</button>)}
                    </div>}
                </div>
            </div>}
            {loading ? <p className="provider-empty-state">Cargando actividades…</p> : visible.length === 0 ?
                <p className="provider-empty-state">{activities.length ? "No encontramos actividades con esos filtros." : isMine ? "Todavía no publicaste actividades." : isFavorites ? "Todavía no guardaste actividades favoritas." : "Todavía no hay actividades publicadas."}</p> :
                <div className="activity-tile-grid">{visible.map(activity => <article className="activity-tile" key={activity.id} role="button" tabIndex={0}
                    onClick={() => { setSelectedId(activity.id); setNotice(""); }} onKeyDown={event => { if (event.target === event.currentTarget && ["Enter", " "].includes(event.key)) { event.preventDefault(); setSelectedId(activity.id); setNotice(""); } }}>
                    <div className="activity-tile-photo">{activity.photos?.length ? <img src={`http://${window.location.hostname}:8080${activity.photos[0].url}`} alt={activity.name} loading="lazy" />
                        : <div className="activity-tile-placeholder"><div className="activity-tile-brand"><BrandLogo /></div></div>}
                        {currentUser?.role === "TOURIST" && <button className={`activity-favorite-button ${favoriteIds.has(activity.id) ? "active" : ""}`} type="button"
                            onClick={event => toggleFavorite(event, activity)} aria-label={favoriteIds.has(activity.id) ? "Quitar de favoritos" : "Guardar en favoritos"}>{favoriteIds.has(activity.id) ? "♥" : "♡"}</button>}
                        <div className="activity-tile-hover"><p>{activity.description}</p><span>Prestador: <strong>{activity.providerName}</strong></span>
                        </div>
                    </div>
                    <div className="activity-tile-info"><div><h2>{activity.name}</h2><p>{activity.city}, {activity.department}</p></div><strong>{price(activity.price)}</strong></div>
                    {!isMine && <div className="activity-tile-rating">{activity.reviewCount ? `★ ${activity.averageRating.toFixed(1)} · ${activity.reviewCount} opiniones` : "Nueva experiencia"}</div>}
                </article>)}</div>}
        </section>
    </main>;
}
