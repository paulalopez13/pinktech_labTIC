import { useState } from "react";
import { loginUser } from "../services/authService";

export default function LoginPage({ onRegister, onForgotPassword, onAuthenticated, registrationMessage, initialNotice = "" }) {
    const [credentials, setCredentials] = useState({ identifier: "", password: "" });
    const [notice, setNotice] = useState(initialNotice);
    const [error, setError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);

    function handleChange(event) {
        const { name, value } = event.target;
        setCredentials(previous => ({ ...previous, [name]: value }));
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setNotice("");
        setError("");
        setIsSubmitting(true);
        try {
            const user = await loginUser(credentials);
            onAuthenticated(user);
        } catch (loginError) {
            setError(loginError.message);
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <main className="registration-page">
            <section className="form-card login-card">
                <div className="form-header">
                    <span className="page-label">Uruguay Mnatural</span>
                    <h1>Iniciar sesión</h1>
                    <p>Ingresá tu nombre de usuario o correo electrónico.</p>
                </div>
                {registrationMessage && (
                    <div className="form-message success login-message" role="status">
                        {registrationMessage}
                    </div>
                )}
                <form className="registration-form" onSubmit={handleSubmit}>
                    <label className="form-field">
                        Nombre de usuario o email
                        <input type="text" name="identifier" autoComplete="username"
                            maxLength={100} value={credentials.identifier} onChange={handleChange} required />
                    </label>
                    <label className="form-field">
                        Contraseña
                        <input type="password" name="password" autoComplete="current-password"
                            value={credentials.password} onChange={handleChange} required />
                    </label>
                    {notice && <div className="form-message info" role="status">{notice}</div>}
                    {error && <div className="form-message error" role="alert">{error}</div>}
                    <button className="submit-button" type="submit" disabled={isSubmitting}>
                        {isSubmitting ? "Ingresando..." : "Iniciar sesión"}
                    </button>
                    <div className="login-actions">
                        <button className="text-button" type="button" onClick={onForgotPassword}>
                            Olvidé mi contraseña
                        </button>
                        <button className="text-button" type="button" onClick={onRegister}>
                            Registrarse
                        </button>
                    </div>
                </form>
            </section>
        </main>
    );
}
