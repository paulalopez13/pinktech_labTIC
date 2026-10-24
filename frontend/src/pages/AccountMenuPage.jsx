import UruguayPhotoCarousel from "../components/UruguayPhotoCarousel";

export default function AccountMenuPage({ user, onExplore, onReservations, onMyActivities, onProviderReservations,
    onAddActivity, onApproveProviders, onEditCategories, onAddAdministrator, notice }) {
    const roleDetails = {
        TOURIST: { heading: "Descubrí Uruguay", description: "Explorá actividades y experiencias en todo el país." },
        PROVIDER: { heading: "Panel de prestador", description: "Gestioná tus actividades y descubrí nuevas propuestas." },
        ADMINISTRATOR: { heading: "Panel de gestión", description: "Administrá la plataforma o explorá sus actividades." },
    };
    const details = roleDetails[user.role] || roleDetails.TOURIST;

    return (
        <main className="registration-page home-page">
            <section className="home-hero account-hero">
                <div className="home-copy">
                    <span className="home-eyebrow"><span /> Hola, {user.username}</span>
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
                    <div className={`registration-buttons home-actions account-actions ${user.role === "ADMINISTRATOR" ? "admin-account-actions" : ""} ${user.role === "PROVIDER" && user.providerStatus === "APPROVED" ? "provider-account-actions" : ""}`}>
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
                                <button className="registration-button" type="button" onClick={onProviderReservations}>
                                    Reservas recibidas
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
                                <button className="registration-button" type="button" onClick={onAddAdministrator}>
                                    Agregar administrador
                                </button>
                            </>
                        )}
                        <button className="registration-button" type="button" onClick={onExplore}>
                            Explorar actividades
                        </button>
                    </div>
                    {notice && <div className="form-message info dashboard-notice" role="status">{notice}</div>}
                    <div className="home-footnote">
                        <span className="home-footnote-mark" aria-hidden="true">✳</span>
                        <span>Pequeñas escapadas. Grandes recuerdos.</span>
                    </div>
                </div>
                <UruguayPhotoCarousel />
            </section>
        </main>
    );
}
