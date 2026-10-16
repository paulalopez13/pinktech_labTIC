export default function ActivitiesPage({ onBack }) {
    return (
        <main className="registration-page">
            <button className="back-button" type="button" onClick={onBack}>← Volver al inicio</button>
            <section className="selection-card">
                <span className="page-label">Uruguay Mnatural</span>
                <h1>Actividades</h1>
                <p>El catálogo de actividades estará disponible próximamente.</p>
            </section>
        </main>
    );
}
