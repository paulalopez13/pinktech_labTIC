import { useState } from "react";
import SocialIcon from "../components/SocialIcon";
import { providerSocialFields } from "../components/providerSocialFields";
import DocumentInput from "../components/DocumentInput";
import LocationFields from "../components/LocationFields";
import PhoneInput from "../components/PhoneInput";
import PasswordStrength from "../components/PasswordStrength";
import { passwordStrength, sanitizeFieldValue } from "../components/fieldRules";

const commonFields = [
    { name: "username", label: "Nombre de usuario", minLength: 3, maxLength: 30, autoComplete: "username" },
    { name: "email", label: "Correo electrónico", type: "email", maxLength: 100, autoComplete: "email" },
    { name: "password", label: "Contraseña", type: "password", minLength: 8, maxLength: 30, autoComplete: "new-password" },
    { name: "confirmPassword", label: "Repetir contraseña", type: "password", minLength: 8, maxLength: 30, autoComplete: "new-password" },
    { name: "phone", label: "Teléfono", type: "tel", maxLength: 20, autoComplete: "tel" },
    { name: "department", label: "Departamento", maxLength: 30, autoComplete: "address-level1" },
    { name: "city", label: "Ciudad", maxLength: 30, autoComplete: "address-level2" },
    { name: "address", label: "Dirección", maxLength: 150, autoComplete: "street-address", fullWidth: true },
    ...providerSocialFields,
];

export default function ProviderRegistrationForm({ title, fields, register, onBack, onRegistered }) {
    const allFields = [...fields, ...commonFields];
    const [formData, setFormData] = useState(() => ({
        ...Object.fromEntries(allFields.map(field => [field.name, ""])),
        documentType: "CI",
    }));
    const [error, setError] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);

    function handleChange(event) {
        const { name, value } = event.target;
        const field = allFields.find(item => item.name === name);
        setFormData(previous => ({ ...previous, [name]: field ? sanitizeFieldValue(value, field) : value,
            ...(name === "department" ? { city: "" } : {}) }));
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
                        {allFields.map(({ name, label, fullWidth, optional, socialNetwork, inputKind, ...inputProps }) => {
                            if (name === "city") return null;
                            if (name === "department") return <LocationFields key="location" department={formData.department}
                                city={formData.city}
                                onDepartmentChange={department => setFormData(previous => ({ ...previous, department, city: "" }))}
                                onCityChange={city => setFormData(previous => ({ ...previous, city }))} />;
                            if (name === "phone") return <PhoneInput key={name} value={formData.phone}
                                onChange={phone => setFormData(previous => ({ ...previous, phone }))} />;
                            if (name === "ci") return <DocumentInput key={name} documentType={formData.documentType}
                                documentNumber={formData.ci}
                                onTypeChange={documentType => setFormData(previous => ({ ...previous, documentType, ci: "" }))}
                                onNumberChange={ci => setFormData(previous => ({ ...previous, ci }))} />;
                            return (
                            <label className={`form-field${fullWidth ? " full-width" : ""}`} key={name}>
                                <span className={socialNetwork ? "social-field-label" : undefined}>
                                    {socialNetwork && <SocialIcon network={socialNetwork} />}{label}{optional ? " (opcional)" : ""}
                                </span>
                                <input type="text" {...inputProps} name={name}
                                    inputMode={inputKind === "digits" ? "numeric" : undefined}
                                    value={formData[name]} onChange={handleChange} required={!optional} />
                                {name === "password" && <PasswordStrength password={formData.password} />}
                            </label>
                            );
                        })}
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
