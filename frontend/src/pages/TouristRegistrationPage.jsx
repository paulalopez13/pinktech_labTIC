import { useState } from "react";
import { registerTourist } from "../services/providerService";
import DocumentInput from "../components/DocumentInput";
import PasswordStrength from "../components/PasswordStrength";
import { passwordStrength, sanitizeFieldValue } from "../components/fieldRules";

const fields = [
    { name: "firstName", label: "Nombre", autoComplete: "given-name", maxLength: 50, inputKind: "letters" },
    { name: "lastName", label: "Apellido", autoComplete: "family-name", maxLength: 50, inputKind: "letters" },
    { name: "ci", label: "Documento", autoComplete: "off", maxLength: 8 },
    { name: "age", label: "Edad", inputMode: "numeric", inputKind: "digits", maxLength: 3 },
    { name: "username", label: "Nombre de usuario", autoComplete: "username", minLength: 3, maxLength: 30, inputKind: "username" },
    { name: "email", label: "Correo electrónico", type: "email", autoComplete: "email", maxLength: 100 },
    { name: "password", label: "Contraseña", type: "password", autoComplete: "new-password", minLength: 8, maxLength: 30 },
    { name: "confirmPassword", label: "Repetir contraseña", type: "password", autoComplete: "new-password", minLength: 8, maxLength: 30 },
];

export default function TouristRegistrationPage({ onBack, onRegistered }) {
    const [formData, setFormData] = useState(() => ({ ...Object.fromEntries(fields.map(({ name }) => [name, ""])), documentType: "CI" }));
    const [error, setError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);

    function handleChange(event) {
        const { name, value } = event.target;
        const field = fields.find(item => item.name === name);
        setFormData(previous => ({ ...previous, [name]: field ? sanitizeFieldValue(value, field) : value }));
    }

    async function handleSubmit(event) {
        event.preventDefault();
        if (isSubmitting) return;
        setError("");
        if (formData.password !== formData.confirmPassword) {
            setError("Las contraseñas no coinciden");
            return;
        }
        if (passwordStrength(formData.password) !== "strong") {
            setError("Elegí una contraseña segura: mínimo 8 caracteres, mayúscula, minúscula, número y símbolo.");
            return;
        }
        setIsSubmitting(true);
        try {
            await registerTourist({ ...formData, age: Number(formData.age) });
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
                        {fields.map(({ name, label, inputKind, ...inputProps }) => name === "ci" ? (
                            <DocumentInput key={name} documentType={formData.documentType}
                                documentNumber={formData.ci}
                                onTypeChange={documentType => setFormData(previous => ({ ...previous, documentType, ci: "" }))}
                                onNumberChange={ci => setFormData(previous => ({ ...previous, ci }))} />
                        ) : (
                            <label className="form-field" key={name}>
                                {label}
                                <input type="text" {...inputProps} name={name}
                                    inputMode={inputProps.inputMode || (inputKind === "digits" ? "numeric" : undefined)}
                                    value={formData[name]} onChange={handleChange} required />
                                {name === "password" && <PasswordStrength password={formData.password} />}
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
