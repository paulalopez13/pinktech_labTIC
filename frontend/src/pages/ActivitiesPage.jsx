import { useEffect, useMemo, useState } from "react";
import { getActivities, getMyActivities, getFavoriteActivities, getFavoriteActivityIds,
    saveFavoriteActivity, removeFavoriteActivity, deleteActivity } from "../services/activityService";
import ActivityDetailPage from "./ActivityDetailPage";
import ProviderProfilePage from "./ProviderProfilePage";
import BrandLogo from "../components/BrandLogo";

const price = value => Number(value) === 0 ? "Gratis" : `$U ${new Intl.NumberFormat("es-UY", { maximumFractionDigits: 2 }).format(Number(value))}`;
const localDateKey = () => {
    const date = new Date();
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
};

function ActivityDeleteConfirmation({ activity, deleting, error, onCancel, onConfirm }) {
    if (!activity) return null;
    return <div className="review-modal-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget && !deleting) onCancel(); }}>
        <section className="review-modal activity-delete-dialog" role="dialog" aria-modal="true" aria-labelledby="delete-activity-title">
            <h2 id="delete-activity-title">¿Eliminar esta actividad?</h2>
            <p><strong>{activity.name}</strong> dejará de aparecer en actividades. Si tiene pagos pendientes, su ficha se conservará para completarlos; si solo tiene reservas pasadas o confirmadas, se guardarán los datos necesarios para el historial.</p>
            {error && <div className="form-message error" role="alert">{error}</div>}
            <div className="review-modal-actions"><button className="activity-step-back" type="button" disabled={deleting} onClick={onCancel}>Cancelar</button><button className="activity-delete-confirm" type="button" disabled={deleting} onClick={onConfirm}>{deleting ? "Eliminando…" : "Eliminar"}</button></div>
        </section>
    </div>;
}

export default function ActivitiesPage({ onBack, currentUser, onLogin, onReserved, onViewReservations, onEdit, onAddActivity, onExploreCategory, initialCategory = "", initialActivityId = null, initialActivityData = null, mode = "explore" }) {
    const [activities, setActivities] = useState([]);
    const [favoriteIds, setFavoriteIds] = useState(new Set());
    const [search, setSearch] = useState("");
    const [category, setCategory] = useState(initialCategory);
    const [department, setDepartment] = useState("");
    const [city, setCity] = useState("");
    const [desiredDate, setDesiredDate] = useState("");
    const [minPrice, setMinPrice] = useState("");
    const [maxPrice, setMaxPrice] = useState("");
    const [activeFilter, setActiveFilter] = useState("all");
    const [sort, setSort] = useState("popular");
    const [hoverMenu, setHoverMenu] = useState("");
    const [clickedMenu, setClickedMenu] = useState("");
    const openMenu = clickedMenu || hoverMenu;
    const [selectedId, setSelectedId] = useState(initialActivityId);
    const [selectedProviderId, setSelectedProviderId] = useState(null);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");
    const [loading, setLoading] = useState(true);
    const [deleteCandidate, setDeleteCandidate] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const isMine = mode === "mine", isFavorites = mode === "favorites", isExplore = mode === "explore";

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
            && (!isExplore || !desiredDate || (activity.upcomingOccurrences || []).some(slot => slot.slice(0, 10) === desiredDate))
            && (minPrice === "" || Number(activity.price) >= Number(minPrice))
            && (maxPrice === "" || Number(activity.price) <= Number(maxPrice))
            && (!isMine || activeFilter === "all" || ((activity.upcomingOccurrences || []).length > 0) === (activeFilter === "active")));
        return [...filtered].sort((a, b) => {
            if (sort === "oldest") return Number(a.id) - Number(b.id);
            if (sort === "newest") return Number(b.id) - Number(a.id);
            if (sort === "popular") return b.recentReservationCount - a.recentReservationCount || b.averageRating - a.averageRating;
            if (sort === "leastPopular") return a.recentReservationCount - b.recentReservationCount || a.averageRating - b.averageRating;
            if (sort === "rating") return b.averageRating - a.averageRating || b.reviewCount - a.reviewCount;
            if (sort === "worstRating") return a.averageRating - b.averageRating || a.reviewCount - b.reviewCount;
            if (sort === "name") return a.name.localeCompare(b.name, "es");
            if (sort === "priceAsc") return Number(a.price) - Number(b.price) || a.name.localeCompare(b.name, "es");
            if (sort === "priceDesc") return Number(b.price) - Number(a.price) || a.name.localeCompare(b.name, "es");
            return b.recentReservationCount - a.recentReservationCount || b.averageRating - a.averageRating;
        });
    }, [activities, search, category, department, city, desiredDate, minPrice, maxPrice, sort, isMine, isExplore, activeFilter]);

    const selected = activities.find(activity => activity.id === selectedId)
        || (initialActivityData?.id === selectedId ? initialActivityData : null);
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

    async function removeActivity(event, activity) {
        event.stopPropagation();
        setDeleteCandidate(activity);
    }

    async function confirmRemoveActivity() {
        if (!deleteCandidate) return;
        const activity = deleteCandidate;
        setDeleting(true);
        setError("");
        try {
            const result = await deleteActivity(activity.id);
            setActivities(items => items.filter(item => item.id !== activity.id));
            setSelectedId(null);
            setNotice(result?.hasPendingPayment
                ? "La actividad se quitó del catálogo. Su ficha se conserva para completar las reservas pendientes y ya no acepta nuevas reservas."
                : result?.retainedForReservations
                    ? "La actividad se quitó del catálogo. Se conservan los datos necesarios para el historial de reservas."
                    : "La actividad se eliminó.");
            setDeleteCandidate(null);
        } catch (deleteError) { setError(deleteError.message); }
        finally { setDeleting(false); }
    }

    if (selectedProviderId) return <ProviderProfilePage providerId={selectedProviderId} currentUser={currentUser} onLogin={onLogin}
        onBack={() => setSelectedProviderId(null)} />;
    if (selected) return <><ActivityDetailPage activity={selected} currentUser={currentUser}
        isOwner={isMine} favorite={favoriteIds.has(selected.id)} onToggleFavorite={event => toggleFavorite(event, selected)}
        onProviderClick={() => selected.providerId && setSelectedProviderId(selected.providerId)}
        onBack={() => setSelectedId(null)} onLogin={() => onLogin?.(selected.id)}
        onCategoryClick={name => { if (isMine) onExploreCategory?.(name); else { setCategory(name); setSelectedId(null); } }}
        onEdit={onEdit} onDelete={activity => setDeleteCandidate(activity)}
        onReserved={message => { setNotice(message); setSelectedId(null); }} onViewReservations={onViewReservations} />
        <ActivityDeleteConfirmation activity={deleteCandidate} deleting={deleting} error={error}
            onCancel={() => { setDeleteCandidate(null); setError(""); }} onConfirm={confirmRemoveActivity} />
    </>;

    return <main className="registration-page form-page activity-entry-page">
        <button className="back-button" type="button" onClick={onBack}>← Menú</button>
        <section className="form-card activities-catalog">
            <div className="form-header"><h1>{isMine ? "Mis actividades" : isFavorites ? "Mis favoritos" : "Explorá actividades"}</h1>
                <p>{isMine ? "Consultá y gestioná las actividades que publicaste." : isFavorites ? "Tus actividades guardadas." : "Encontrá tu próxima actividad."}</p></div>
            {notice && <div className="form-message success" role="status">{notice}</div>}
            {error && !deleteCandidate && <div className="form-message error" role="alert">{error}</div>}
            {isMine && <button className="submit-button my-activity-add" type="button" onClick={onAddActivity}>Agregar actividad</button>}
            <div className="activity-toolbar">
                <input className="activity-search" type="search" value={search} onChange={event => setSearch(event.target.value)} placeholder="Buscar actividad" aria-label="Buscar actividad por nombre" />
                <div className="activity-menu-wrap" onMouseEnter={() => setHoverMenu("filters")} onMouseLeave={() => { setHoverMenu(""); setClickedMenu(""); }}>
                    <button className={`activity-menu-button ${openMenu === "filters" ? "active" : ""}`} type="button" aria-expanded={openMenu === "filters"} aria-controls="activity-filter-menu" onClick={() => setClickedMenu(clickedMenu === "filters" ? "" : "filters")}>Filtrar <span aria-hidden="true">⌄</span></button>
                    {openMenu === "filters" && <div className="activity-popover activity-filter-popover" id="activity-filter-menu" role="group" aria-label="Filtros de actividades">
                        <label>Categoría<select value={category} onChange={event => setCategory(event.target.value)}><option value="">Todas</option>{categoryNames.map(name => <option key={name}>{name}</option>)}</select></label>
                        <label>Departamento<select value={department} onChange={event => { setDepartment(event.target.value); setCity(""); }}><option value="">Todos los departamentos</option>{departments.map(name => <option key={name}>{name}</option>)}</select></label>
                        <label>Ciudad<select value={city} onChange={event => setCity(event.target.value)} disabled={!department}><option value="">{department ? "Todas las ciudades" : "Elegí primero un departamento"}</option>{cities.map(name => <option key={name}>{name}</option>)}</select></label>
                        {isExplore && <label className="activity-date-filter">Fecha<input type="date" min={localDateKey()} value={desiredDate} onChange={event => setDesiredDate(event.target.value)} /></label>}
                        <div className="activity-price-filter"><label>Precio desde<input type="number" min="0" step="any" value={minPrice} onChange={event => setMinPrice(event.target.value)} /></label>
                            <label>Precio hasta<input type="number" min="0" step="any" value={maxPrice} onChange={event => setMaxPrice(event.target.value)} /></label></div>
                        {isMine && <label>Estado<select value={activeFilter} onChange={event => setActiveFilter(event.target.value)}><option value="all">Todas</option><option value="active">Activas</option><option value="inactive">Inactivas</option></select></label>}
                        <button className="activity-clear-filters" type="button" onClick={() => { setCategory(""); setDepartment(""); setCity(""); setDesiredDate(""); setMinPrice(""); setMaxPrice(""); setActiveFilter("all"); }}>Limpiar filtros</button>
                    </div>}
                </div>
                <div className="activity-menu-wrap" onMouseEnter={() => setHoverMenu("sort")} onMouseLeave={() => { setHoverMenu(""); setClickedMenu(""); }}>
                    <button className={`activity-menu-button ${openMenu === "sort" ? "active" : ""}`} type="button" aria-expanded={openMenu === "sort"} aria-controls="activity-sort-menu" onClick={() => setClickedMenu(clickedMenu === "sort" ? "" : "sort")}>Ordenar <span aria-hidden="true">⌄</span></button>
                    {openMenu === "sort" && <div className="activity-popover activity-sort-popover" id="activity-sort-menu" role="group" aria-label="Ordenar actividades">
                        {(isMine
                            ? [["oldest", "Más antiguas"], ["newest", "Más recientes"], ["popular", "Más populares"], ["leastPopular", "Menos populares"], ["rating", "Mejor valoradas"], ["worstRating", "Peor valoradas"], ["name", "Alfabéticamente (A–Z)"]]
                            : [["popular", "Más populares"], ["leastPopular", "Menos populares"], ["rating", "Mejor valoradas"], ["worstRating", "Peor valoradas"], ["name", "Alfabéticamente"], ["priceAsc", "Precio: menor a mayor"], ["priceDesc", "Precio: mayor a menor"]]
                        ).map(([value, label]) => <button key={value} type="button" className={sort === value ? "selected" : ""} onClick={() => { setSort(value); setClickedMenu(""); }}>{label}</button>)}
                    </div>}
                </div>
            </div>
            {loading ? <p className="provider-empty-state">Cargando actividades…</p> : visible.length === 0 ?
                <p className="provider-empty-state">{activities.length ? "No encontramos actividades con esos filtros." : isMine ? "Todavía no publicaste actividades." : isFavorites ? "Todavía no guardaste actividades favoritas." : "Todavía no hay actividades publicadas."}</p> :
                <div className="activity-tile-grid">{visible.map(activity => <article className={`activity-tile${isMine && !(activity.upcomingOccurrences || []).length ? " inactive-activity" : ""}`} key={activity.id} role="button" tabIndex={0}
                    onClick={() => { setSelectedId(activity.id); setNotice(""); }} onKeyDown={event => { if (event.target === event.currentTarget && ["Enter", " "].includes(event.key)) { event.preventDefault(); setSelectedId(activity.id); setNotice(""); } }}>
                    <div className="activity-tile-photo">{activity.photos?.length ? <img src={`${activity.photos[0].url}`} alt={activity.name} loading="lazy" />
                        : <div className="activity-tile-placeholder"><div className="activity-tile-brand"><BrandLogo /></div></div>}
                        {currentUser?.role === "TOURIST" && activity.acceptingReservations !== false && <button className={`activity-favorite-button ${favoriteIds.has(activity.id) ? "active" : ""}`} type="button"
                            onClick={event => toggleFavorite(event, activity)} aria-label={favoriteIds.has(activity.id) ? "Quitar de favoritos" : "Guardar en favoritos"}>{favoriteIds.has(activity.id) ? "♥" : "♡"}</button>}
                        <div className="activity-tile-hover"><p>{activity.description}</p>{!isMine && <span>Prestador: <strong>{activity.providerName}</strong></span>}
                        </div>
                    </div>
                    <div className="activity-tile-info"><div className="activity-tile-main"><h2>{activity.name}</h2><p>{activity.city}, {activity.department}</p></div><strong>{price(activity.price)}</strong></div>
                    {isMine && <div className="activity-management-actions"><button type="button" onClick={event => { event.stopPropagation(); onEdit?.(activity); }}>Modificar</button><button type="button" className="danger" onClick={event => removeActivity(event, activity)}>Eliminar</button></div>}
                    {!isMine && <div className="activity-tile-rating">{activity.reviewCount ? `★ ${activity.averageRating.toFixed(1)}` : "Nueva experiencia"}</div>}
                </article>)}</div>}
        </section>
        <ActivityDeleteConfirmation activity={deleteCandidate} deleting={deleting} error={error}
            onCancel={() => { setDeleteCandidate(null); setError(""); }} onConfirm={confirmRemoveActivity} />
    </main>;
}
