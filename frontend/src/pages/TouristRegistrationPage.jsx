import { useState } from "react";
import { registerTourist } from "../services/providerService";

const fields = [
    { name: "firstName", label: "Nombre", autoComplete: "given-name", maxLength: 50 },
    { name: "lastName", label: "Apellido", autoComplete: "family-name", maxLength: 50 },
    { name: "ci", label: "Cédula", autoComplete: "off", maxLength: 20 },
    { name: "age", label: "Edad", type: "number", min: 0, max: 120 },
    { name: "username", label: "Nombre de usuario", autoComplete: "username", minLength: 3, maxLength: 30 },
    { name: "email", label: "Correo electrónico", type: "email", autoComplete: "email", maxLength: 100 },
    { name: "password", label: "Contraseña", type: "password", autoComplete: "new-password", minLength: 8, maxLength: 30 },
    { name: "confirmPassword", label: "Repetir contraseña", type: "password", autoComplete: "new-password", minLength: 8, maxLength: 30 },
];

export default function TouristRegistrationPage({ onBack, onRegistered }) {
    const [formData, setFormData] = useState(() => Object.fromEntries(fields.map(({ name }) => [name, ""])));
    const [error, setError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);

    function handleChange(event) {
        const { name, value } = event.target;
        setFormData(previous => ({ ...previous, [name]: value }));
    }

    async function handleSubmit(event) {
        event.preventDefault();
        if (isSubmitting) return;
        setError("");
        if (formData.password !== formData.confirmPassword) {
            setError("Las contraseñas no coinciden");
            return;
        }
        setIsSubmitting(true);
        try {
            await registerTourist(formData);
            onRegistered();
        } catch (registrationError) {
            setError(registrationError.message);
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <main className="registration-page form-page">
            <button className="back-button" type="button" onClick={onBack}>← Volver</button>
            <section className="form-card">
                <div className="form-header">
                    <span className="page-label">Registro de turista</span>
                    <h1>Crear cuenta</h1>
                    <p>Completá tus datos para registrarte.</p>
                </div>
                <form className="registration-form" onSubmit={handleSubmit}>
                    <div className="form-grid">
                        {fields.map(({ name, label, ...inputProps }) => (
                            <label className="form-field" key={name}>
                                {label}
                                <input type="text" {...inputProps} name={name}
                                    value={formData[name]} onChange={handleChange} required />
                            </label>
                        ))}
                    </div>
                    {error && <div className="form-message error" role="alert">{error}</div>}
                    <button className="submit-button" type="submit" disabled={isSubmitting}>
                        {isSubmitting ? "Creando cuenta..." : "Registrarme"}
                    </button>
                </form>
            </section>
        </main>
    );
}
