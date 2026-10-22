import { useState } from "react";
import { resetPassword } from "../services/passwordResetService";
import PasswordStrength from "../components/PasswordStrength";
import { passwordStrength } from "../components/fieldRules";

export default function ResetPasswordPage({ token, onBack, onRequestLink }) {
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isComplete, setIsComplete] = useState(false);

    async function handleSubmit(event) {
        event.preventDefault();
        setError("");
        setMessage("");
        if (password !== confirmPassword) {
            setError("Las contraseñas no coinciden.");
            return;
        }
        if (passwordStrength(password) !== "strong") {
            setError("Elegí una contraseña segura: mínimo 8 caracteres, mayúscula, minúscula, número y símbolo.");
            return;
        }
        setIsSubmitting(true);
        try {
            const result = await resetPassword({ token, password, confirmPassword });
            setMessage(result.message);
            setIsComplete(true);
            setPassword("");
            setConfirmPassword("");
        } catch (resetError) {
            setError(resetError.message);
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <main className="registration-page">
            <section className="form-card login-card">
                <div className="form-header">
                    <span className="page-label">Uruguay Mnatural</span>
                    <h1>Crear nueva contraseña</h1>
                    <p>Elegí una contraseña nueva y repetila para confirmarla.</p>
                </div>
                {!token ? (
                    <div className="form-message info" role="status">
                        Abrí el enlace de recuperación que recibiste por correo para continuar.
                    </div>
                ) : isComplete ? (
                    <div className="form-message success" role="status">{message}</div>
                ) : (
                    <form className="registration-form" onSubmit={handleSubmit}>
                        <label className="form-field">
                            Nueva contraseña
                            <input type="password" autoComplete="new-password" minLength={8} maxLength={30}
                                value={password} onChange={event => setPassword(event.target.value)} required />
                            <PasswordStrength password={password} />
                        </label>
                        <label className="form-field">
                            Repetir nueva contraseña
                            <input type="password" autoComplete="new-password" minLength={8} maxLength={30}
                                value={confirmPassword} onChange={event => setConfirmPassword(event.target.value)} required />
                        </label>
                        {error && <div className="form-message error" role="alert">{error}</div>}
                        <button className="submit-button" type="submit" disabled={isSubmitting}>
                            {isSubmitting ? "Guardando..." : "Guardar nueva contraseña"}
                        </button>
                    </form>
                )}
                <div className="password-recovery-actions">
                    <button className="text-button" type="button" onClick={onBack}>Volver a iniciar sesión</button>
                    <button className="text-button" type="button" onClick={onRequestLink}>Pedir otro enlace</button>
                </div>
            </section>
        </main>
    );
}
