export default function RatingStars({ rating = 0, onChange, label = "Puntuación", allowHalf = true }) {
    const interactive = typeof onChange === "function";
    return <span className={`rating-stars ${interactive ? "interactive" : ""}`} role={interactive ? "group" : undefined} aria-label={interactive ? label : `${Number(rating).toFixed(1)} de 5 estrellas`}>
        {[0, 1, 2, 3, 4].map(index => {
            const fill = Math.max(0, Math.min(1, Number(rating) - index));
            return <span className="rating-star" key={index}>
                <span className="rating-star-base" aria-hidden="true">★</span>
                <span className="rating-star-fill" style={{ width: `${fill * 100}%` }} aria-hidden="true">★</span>
                {interactive && (allowHalf ? <>
                    <button type="button" className="rating-star-hit left" aria-label={`Calificar con ${index + 0.5} de 5 estrellas`} onClick={() => onChange(index + 0.5)} />
                    <button type="button" className="rating-star-hit right" aria-label={`Calificar con ${index + 1} de 5 estrellas`} onClick={() => onChange(index + 1)} />
                </> : <button type="button" className="rating-star-hit whole" aria-label={`Filtrar por ${index + 1} estrellas`} onClick={() => onChange(index + 1)} />)}
            </span>;
        })}
    </span>;
}
