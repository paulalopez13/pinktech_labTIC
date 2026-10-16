export default function HomePage({ onExplore }) {
    return (
        <main className="registration-page home-page">
            <section className="selection-card home-card">
                <span className="page-label">Turismo natural en Uruguay</span>
                <h1>Descubrí Uruguay a tu manera</h1>
                <p>Encontrá experiencias y actividades para disfrutar nuestros paisajes.</p>
                <div className="registration-buttons home-actions">
                    <button className="registration-button" type="button" onClick={onExplore}>
                        Explorar actividades
                    </button>
                </div>
            </section>
        </main>
    );
}
