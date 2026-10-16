function RegistrationTypePage({ onSelectTourist, onSelectProvider }) {
    return (
        <main className="registration-page">
            <section className="selection-card">
                <span className="page-label">Uruguay Mnatural</span>

                <h1>Crear una cuenta</h1>

                <p>Seleccione el tipo de usuario</p>

                <div className="registration-buttons">
                    <button
                        className="registration-button"
                        type="button"
                        onClick={onSelectTourist}
                    >
                        Turista
                    </button>

                    <button
                        className="registration-button"
                        type="button"
                        onClick={onSelectProvider}
                    >
                        Prestador
                    </button>
                </div>
            </section>
        </main>
    );
}

export default RegistrationTypePage;
