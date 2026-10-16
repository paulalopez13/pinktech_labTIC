import { useState } from "react";
import { requestPasswordReset } from "../services/passwordResetService";

export default function ForgotPasswordPage({ onBack }) {
    const [email, setEmail] = useState("");
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);

    async function handleSubmit(event) {
        event.preventDefault();
        setError("");
        setMessage("");
        setIsSubmitting(true);
        try {
            const result = await requestPasswordReset(email);
            setMessage(result.message);
        } catch (requestError) {
            setError(requestError.message);
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <main className="registration-page">
            <section className="form-card login-card">
                <div className="form-header">
                    <span className="page-label">Uruguay Mnatural</span>
                    <h1>Recuperar contraseña</h1>
                    <p>Ingresá el email asociado a tu cuenta para recibir instrucciones.</p>
                </div>
                <form className="registration-form" onSubmit={handleSubmit}>
                    <label className="form-field">
                        Correo electrónico
                        <input type="email" name="email" autoComplete="email" maxLength={100}
                            value={email} onChange={event => setEmail(event.target.value)} required />
                    </label>
                    {message && <div className="form-message info" role="status">{message}</div>}
                    {error && <div className="form-message error" role="alert">{error}</div>}
                    <button className="submit-button" type="submit" disabled={isSubmitting}>
                        {isSubmitting ? "Enviando..." : "Enviar instrucciones"}
                    </button>
                </form>
                <div className="password-recovery-actions">
                    <button className="text-button" type="button" onClick={onBack}>Volver a iniciar sesión</button>
                </div>
            </section>
        </main>
    );
}
