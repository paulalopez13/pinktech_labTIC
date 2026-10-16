import { useState } from "react";

const commonFields = [
    { name: "username", label: "Nombre de usuario", minLength: 3, maxLength: 30, autoComplete: "username" },
    { name: "email", label: "Correo electrónico", type: "email", maxLength: 100, autoComplete: "email" },
    { name: "password", label: "Contraseña", type: "password", minLength: 8, maxLength: 30, autoComplete: "new-password" },
    { name: "confirmPassword", label: "Repetir contraseña", type: "password", minLength: 8, maxLength: 30, autoComplete: "new-password" },
    { name: "phone", label: "Teléfono", type: "tel", maxLength: 20, autoComplete: "tel" },
    { name: "department", label: "Departamento", maxLength: 30, autoComplete: "address-level1" },
    { name: "city", label: "Ciudad", maxLength: 30, autoComplete: "address-level2" },
    { name: "address", label: "Dirección", maxLength: 150, autoComplete: "street-address", fullWidth: true },
];

export default function ProviderRegistrationForm({ title, fields, register, onBack, onRegistered }) {
    const allFields = [...fields, ...commonFields];
    const [formData, setFormData] = useState(() =>
        Object.fromEntries(allFields.map(field => [field.name, ""]))
    );
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
            await register(formData);
            onRegistered();
        } catch (error) {
            setError(error.message);
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <main className="registration-page form-page">
            <button className="back-button" type="button" onClick={onBack}>← Volver</button>
            <section className="form-card">
                <div className="form-header">
                    <span className="page-label">Registro de prestador</span>
                    <h1>{title}</h1>
                    <p>Complete los datos para solicitar el registro.</p>
                </div>
                <form className="registration-form" onSubmit={handleSubmit}>
                    <div className="form-grid">
                        {allFields.map(({ name, label, fullWidth, ...inputProps }) => (
                            <label className={`form-field${fullWidth ? " full-width" : ""}`} key={name}>
                                {label}
                                <input type="text" {...inputProps} name={name}
                                    value={formData[name]} onChange={handleChange} required />
                            </label>
                        ))}
                    </div>
                    {error && <div className="form-message error" role="alert">{error}</div>}
                    <button className="submit-button" type="submit" disabled={isSubmitting}>
                        {isSubmitting ? "Enviando solicitud..." : "Solicitar registro"}
                    </button>
                </form>
            </section>
        </main>
    );
}
