function ProviderRegistrationPage({
                                      onSelectCompany,
                                      onSelectIndividual,
                                      onBack,
                                  }) {
    return (
        <main className="registration-page">
            <button
                className="back-button"
                type="button"
                onClick={onBack}
            >
                ← Volver
            </button>

            <section className="selection-card">
                <span className="page-label">Registro</span>

                <h1>Tipo de prestador</h1>

                <p>Seleccione cómo desea registrarse</p>

                <div className="registration-buttons">
                    <button
                        className="registration-button"
                        type="button"
                        onClick={onSelectCompany}
                    >
                        Empresa
                    </button>

                    <button
                        className="registration-button"
                        type="button"
                        onClick={onSelectIndividual}
                    >
                        Particular
                    </button>
                </div>
            </section>
        </main>
    );
}

export default ProviderRegistrationPage;

