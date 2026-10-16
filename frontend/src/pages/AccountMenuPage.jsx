export default function AccountMenuPage({ user, onExplore, onReservations, onMyActivities,
    onAddActivity, onApproveProviders, onEditCategories, notice }) {
    const roleDetails = {
        TOURIST: { heading: "Tu espacio de turista", description: "Explorá actividades y experiencias en Uruguay." },
        PROVIDER: { heading: "Tu espacio de prestador", description: "Gestioná tus actividades y descubrí otras experiencias." },
        ADMINISTRATOR: { heading: "Administración", description: "Administrá la plataforma o explorá sus actividades." },
    };
    const details = roleDetails[user.role] || roleDetails.TOURIST;

    return (
        <main className="registration-page home-page">
            <section className="selection-card home-card">
                <span className="page-label">Hola, {user.username}</span>
                <h1>{details.heading}</h1>
                <p>{details.description}</p>
                {user.role === "PROVIDER" && user.providerStatus === "PENDING" && (
                    <div className="form-message info dashboard-notice" role="status">
                        Tu cuenta está pendiente de aprobación. Mientras tanto, podés explorar actividades.
                    </div>
                )}
                {user.role === "PROVIDER" && user.providerStatus === "REJECTED" && (
                    <div className="form-message info dashboard-notice" role="status">
                        Tu solicitud de prestador no fue aprobada. Podés explorar actividades.
                    </div>
                )}
                <div className="registration-buttons home-actions">
                    {user.role === "TOURIST" && (
                        <button className="registration-button" type="button" onClick={onReservations}>
                            Mis reservas
                        </button>
                    )}
                    {user.role === "PROVIDER" && user.providerStatus === "APPROVED" && (
                        <>
                            <button className="registration-button" type="button" onClick={onMyActivities}>
                                Mis actividades
                            </button>
                            <button className="registration-button" type="button" onClick={onAddActivity}>
                                Agregar actividad
                            </button>
                        </>
                    )}
                    {user.role === "ADMINISTRATOR" && (
                        <>
                            <button className="registration-button" type="button" onClick={onApproveProviders}>
                                Aprobar prestadores
                            </button>
                            <button className="registration-button" type="button" onClick={onEditCategories}>
                                Editar categorías
                            </button>
                        </>
                    )}
                    <button className="registration-button" type="button" onClick={onExplore}>
                        Explorar actividades
                    </button>
                </div>
                {notice && <div className="form-message info dashboard-notice" role="status">{notice}</div>}
            </section>
        </main>
    );
}
