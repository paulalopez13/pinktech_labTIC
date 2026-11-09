import { useEffect, useState } from "react";
import BrandLogo from "./BrandLogo";
import { getActivities } from "../services/activityService";

const price = value => Number(value) === 0 ? "Gratis" : `$U ${new Intl.NumberFormat("es-UY", { maximumFractionDigits: 2 }).format(Number(value))}`;

export default function TopRatedActivities({ onExplore, onSelectActivity }) {
    const [highlights, setHighlights] = useState({ rated: [], popular: [] });

    useEffect(() => {
        let active = true;
        getActivities().then(items => {
            if (!active) return;
            setHighlights({
                rated: items.filter(activity => activity.reviewCount > 0)
                    .sort((a, b) => b.averageRating - a.averageRating || b.reviewCount - a.reviewCount).slice(0, 3),
                popular: [...items]
                    .sort((a, b) => (b.recentReservationCount || 0) - (a.recentReservationCount || 0)
                        || (b.averageRating || 0) - (a.averageRating || 0)).slice(0, 3)
            });
        }).catch(() => {});
        return () => { active = false; };
    }, []);

    const section = (id, eyebrow, title, activities, metric) => activities.length > 0 && <section className="home-top-activities" aria-labelledby={`${id}-title`} key={id}>
        <div className="home-top-heading"><div><span>{eyebrow}</span><h2 id={`${id}-title`}>{title}</h2></div>
            <button type="button" onClick={onExplore}>Ver todas <span aria-hidden="true">↗</span></button></div>
        <div className="home-top-grid">{activities.map((activity, index) => <button className="home-top-card" type="button" key={activity.id} onClick={() => onSelectActivity?.(activity)}>
            <span className="home-top-rank">{index + 1}</span>
            <span className="home-top-photo">{activity.photos?.length ? <img src={`${activity.photos[0].url}`} alt="" loading="lazy" /> : <span className="home-top-placeholder"><BrandLogo /></span>}</span>
            <span className="home-top-info"><strong className="home-top-name">{activity.name}</strong><span className="home-top-location">{activity.city}, {activity.department}</span>
                {metric === "rating" ? <span className="home-top-rating"><span aria-label={`${activity.averageRating.toFixed(1)} de 5 estrellas`}>★</span> {activity.averageRating.toFixed(1)} <small>({activity.reviewCount} {activity.reviewCount === 1 ? "reseña" : "reseñas"})</small></span>
                    : <span className="home-top-popularity">↗ {activity.recentReservationCount || 0} reservas recientes</span>}
                <strong className="home-top-price">{price(activity.price)}</strong></span>
        </button>)}</div>
    </section>;

    return <>{section("home-top-rated", "Las favoritas de la comunidad", "Top 3 mejor valoradas", highlights.rated, "rating")}
        {section("home-top-popular", "Las más elegidas", "Top 3 más populares", highlights.popular, "popular")}</>;
}
