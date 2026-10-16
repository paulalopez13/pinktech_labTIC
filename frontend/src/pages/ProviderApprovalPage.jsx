import { useCallback, useEffect, useState } from "react";
import { decideProviderApplication, getProviderApplications } from "../services/adminProviderService";

function ProviderCard({ application, onDecision, busy }) {
    const isCompany = application.providerType === "EMPRESA";
    const details = isCompany
        ? [["Empresa", application.companyName], ["RUT", application.rut]]
        : [["Nombre", `${application.firstName} ${application.lastName}`], ["Cédula", application.ci]];
    details.push(["Usuario", application.username], ["Email", application.email], ["Teléfono", application.phone],
        ["Departamento", application.department], ["Ciudad", application.city], ["Dirección", application.address]);

    return (
        <article className="provider-application-card">
            <div className="provider-application-info">
                <h3>{isCompany ? application.companyName : `${application.firstName} ${application.lastName}`}</h3>
                <span className="provider-kind">Prestador {isCompany ? "empresa" : "particular"}</span>
                <dl>
                    {details.map(([label, value]) => (
                        <div key={label}><dt>{label}</dt><dd>{value || "—"}</dd></div>
                    ))}
                </dl>
            </div>
            {application.status === "PENDING" ? (
                <div className="provider-application-actions">
                    <button className="provider-approve-button" type="button" disabled={busy}
                        onClick={() => onDecision(application, "approve")}>Aceptar</button>
                    <button className="provider-reject-button" type="button" disabled={busy}
                        onClick={() => onDecision(application, "reject")}>Rechazar</button>
                </div>
            ) : (
                <div className={`provider-application-status ${application.status.toLowerCase()}`}>
                    {application.status === "APPROVED" ? "Aceptado" : "Rechazado"}
                </div>
            )}
        </article>
    );
}

export default function ProviderApprovalPage({ onBack }) {
    const [applications, setApplications] = useState({ pending: [], reviewed: [] });
    const [error, setError] = useState("");
    const [isLoading, setIsLoading] = useState(true);
    const [busyId, setBusyId] = useState(null);
    const [notice, setNotice] = useState("");

    const loadApplications = useCallback(async () => {
        setError("");
        try {
            setApplications(await getProviderApplications());
        } catch (loadError) {
            setError(loadError.message);
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => { loadApplications(); }, [loadApplications]);

    async function handleDecision(application, decision) {
        setBusyId(application.id);
        setError("");
        setNotice("");
        try {
            await decideProviderApplication(application.id, decision);
            setNotice(decision === "approve" ? "El prestador fue aceptado." : "El prestador fue rechazado.");
            await loadApplications();
        } catch (decisionError) {
            setError(decisionError.message);
        } finally {
            setBusyId(null);
        }
    }

    return (
        <main className="registration-page form-page">
            <button className="back-button" type="button" onClick={onBack}>← Volver</button>
            <section className="form-card provider-approval-page">
                <div className="form-header">
                    <span className="page-label">Administración</span>
                    <h1>Aprobar prestadores</h1>
                    <p>Revisá las solicitudes y consultá las decisiones anteriores.</p>
                </div>
                {notice && <div className="form-message success" role="status">{notice}</div>}
                {error && <div className="form-message error" role="alert">{error}</div>}
                {isLoading ? <div className="form-message info" role="status">Cargando solicitudes...</div> : (
                    <>
                        <section className="provider-approval-section">
                            <h2>Solicitudes pendientes</h2>
                            {applications.pending.length === 0
                                ? <p className="provider-empty-state">No hay solicitudes pendientes.</p>
                                : <div className="provider-application-list">
                                    {applications.pending.map(application => (
                                        <ProviderCard key={`${application.providerType}-${application.id}`}
                                            application={application} onDecision={handleDecision} busy={busyId === application.id} />
                                    ))}
                                </div>}
                        </section>
                        <section className="provider-approval-section reviewed-section">
                            <h2>Solicitudes revisadas</h2>
                            {applications.reviewed.length === 0
                                ? <p className="provider-empty-state">Todavía no hay solicitudes aceptadas o rechazadas.</p>
                                : <div className="provider-application-list">
                                    {applications.reviewed.map(application => (
                                        <ProviderCard key={`${application.providerType}-${application.id}`}
                                            application={application} onDecision={handleDecision} busy={false} />
                                    ))}
                                </div>}
                        </section>
                    </>
                )}
            </section>
        </main>
    );
}
