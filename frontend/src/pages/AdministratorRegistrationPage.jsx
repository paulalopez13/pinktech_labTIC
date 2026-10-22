import { useState } from "react";
import PasswordStrength from "../components/PasswordStrength";
import { passwordStrength } from "../components/fieldRules";
import { createAdministrator } from "../services/administratorService";

const initialData = { username: "", email: "", password: "", confirmPassword: "" };

export default function AdministratorRegistrationPage({ onBack }) {
    const [data, setData] = useState(initialData);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");
    const [busy, setBusy] = useState(false);

    function handleChange(event) {
        setData(previous => ({ ...previous, [event.target.name]: event.target.value }));
    }

    async function handleSubmit(event) {
        event.preventDefault();
        if (busy) return;
        setError("");
        setNotice("");
        if (data.password !== data.confirmPassword) {
            setError("Las contraseñas no coinciden.");
            return;
        }
        if (passwordStrength(data.password) !== "strong") {
            setError("La contraseña debe tener 8–30 caracteres, mayúsculas, minúsculas, números y símbolos.");
            return;
        }
        setBusy(true);
        try {
            const result = await createAdministrator(data);
            setNotice(result?.message || "Administrador creado correctamente. Se envió un correo de aviso si el email está configurado.");
            setData(initialData);
        } catch (submitError) {
            setError(submitError.message);
        } finally {
            setBusy(false);
        }
    }

    return (
        <main className="registration-page form-page">
            <button className="back-button" type="button" onClick={onBack}>← Volver</button>
            <section className="form-card">
                <div className="form-header">
                    <span className="page-label">Administración</span>
                    <h1>Agregar administrador</h1>
                    <p>Ingresá los datos de la nueva cuenta. Se enviará un aviso al correo indicado.</p>
                </div>
                <form className="registration-form" onSubmit={handleSubmit}>
                    <div className="form-grid">
                        <label className="form-field">Nombre de usuario
                            <input type="text" name="username" minLength={3} maxLength={30} autoComplete="username"
                                value={data.username} onChange={handleChange} required />
                        </label>
                        <label className="form-field">Correo electrónico
                            <input type="email" name="email" maxLength={100} autoComplete="email"
                                value={data.email} onChange={handleChange} required />
                        </label>
                        <label className="form-field">Contraseña
                            <input type="password" name="password" minLength={8} maxLength={30} autoComplete="new-password"
                                value={data.password} onChange={handleChange} required />
                            <PasswordStrength password={data.password} />
                        </label>
                        <label className="form-field">Repetir contraseña
                            <input type="password" name="confirmPassword" minLength={8} maxLength={30} autoComplete="new-password"
                                value={data.confirmPassword} onChange={handleChange} required />
                        </label>
                    </div>
                    {error && <div className="form-message error" role="alert">{error}</div>}
                    {notice && <div className="form-message success" role="status">{notice}</div>}
                    <button className="submit-button" type="submit" disabled={busy}>
                        {busy ? "Creando administrador..." : "Crear administrador"}
                    </button>
                </form>
            </section>
        </main>
    );
}
