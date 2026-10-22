import { useEffect, useState } from "react";
import { getProfile, updateProfile } from "../services/profileService";
import SocialIcon from "../components/SocialIcon";
import { providerSocialFields } from "../components/providerSocialFields";
import DocumentInput from "../components/DocumentInput";
import LocationFields from "../components/LocationFields";
import PhoneInput from "../components/PhoneInput";
import PasswordStrength from "../components/PasswordStrength";
import { passwordStrength, sanitizeFieldValue } from "../components/fieldRules";

const accountFields = [
    { name: "username", label: "Nombre de usuario", minLength: 3, maxLength: 30 },
    { name: "email", label: "Correo electrónico", type: "email", maxLength: 100 },
    { name: "password", label: "Nueva contraseña", type: "password", minLength: 8, maxLength: 30,
        placeholder: "Dejá este campo vacío para mantenerla", optional: true },
    { name: "confirmPassword", label: "Repetir nueva contraseña", type: "password", minLength: 8, maxLength: 30,
        placeholder: "Repetila para cambiarla", optional: true },
];
const touristFields = [
    { name: "firstName", label: "Nombre", maxLength: 50, inputKind: "letters" },
    { name: "lastName", label: "Apellido", maxLength: 50, inputKind: "letters" },
    { name: "ci", label: "Documento", maxLength: 8 },
    { name: "age", label: "Edad", inputMode: "numeric", inputKind: "digits", maxLength: 3 },
];
const providerFields = [
    { name: "phone", label: "Teléfono", type: "tel", maxLength: 20 },
    { name: "department", label: "Departamento", maxLength: 30 },
    { name: "city", label: "Ciudad", maxLength: 30 },
    { name: "address", label: "Dirección", maxLength: 150, fullWidth: true },
    ...providerSocialFields,
];

function fieldsFor(profile) {
    if (profile.role === "TOURIST") return [...accountFields, ...touristFields];
    if (profile.role === "ADMINISTRATOR") return accountFields;
    if (profile.role === "PROVIDER" && profile.companyName !== null) {
        return [...accountFields,
            { name: "companyName", label: "Nombre de la empresa", maxLength: 30, inputKind: "letters" },
            { name: "rut", label: "RUT", maxLength: 12, inputKind: "digits", inputMode: "numeric" },
            ...providerFields];
    }
    return [...accountFields,
        { name: "firstName", label: "Nombre", maxLength: 20, inputKind: "letters" },
        { name: "lastName", label: "Apellido", maxLength: 20, inputKind: "letters" },
        { name: "ci", label: "Documento", maxLength: 8 },
        ...providerFields];
}

export default function ProfilePage({ onBack, onUpdated }) {
    const [profile, setProfile] = useState(null);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [passwordWasChanged, setPasswordWasChanged] = useState(false);

    useEffect(() => {
        let isCurrent = true;
        getProfile()
            .then(data => { if (isCurrent) setProfile({ ...data, documentType: data.documentType || "CI", password: "" }); })
            .catch(loadError => { if (isCurrent) setError(loadError.message); })
            .finally(() => { if (isCurrent) setIsLoading(false); });
        return () => { isCurrent = false; };
    }, []);

    function handleChange(event) {
        const { name, value } = event.target;
        const field = fieldsFor(profile).find(item => item.name === name);
        const sanitized = field ? sanitizeFieldValue(value, field) : value;
        setProfile(previous => ({ ...previous, [name]: name === "age" && sanitized !== "" ? Number(sanitized) : sanitized,
            ...(name === "department" ? { city: "" } : {}) }));
        setError("");
        setSuccess("");
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setError("");
        setSuccess("");
        if (profile.password && profile.password !== profile.confirmPassword) {
            setError("Las contraseñas no coinciden");
            return;
        }
        if (!profile.password && profile.confirmPassword) {
            setError("Ingresá una contraseña nueva para confirmarla");
            return;
        }
        if (profile.password && passwordStrength(profile.password) !== "strong") {
            setError("Elegí una contraseña segura: mínimo 8 caracteres, mayúscula, minúscula, número y símbolo.");
            return;
        }
        setIsSaving(true);
        try {
            const changedPassword = Boolean(profile.password);
            const updated = await updateProfile(profile);
            setProfile({ ...updated, password: "", confirmPassword: "" });
            if (changedPassword) setPasswordWasChanged(true);
            onUpdated(updated);
            setSuccess("Se guardaron los cambios.");
        } catch (saveError) {
            setError(saveError.message);
        } finally {
            setIsSaving(false);
        }
    }

    return (
        <main className="registration-page form-page">
            <button className="back-button" type="button" onClick={onBack}>← Volver</button>
            <section className="form-card profile-card">
                <div className="form-header">
                    <span className="page-label">Mi perfil</span>
                    <h1>Mis datos</h1>
                    <p>Actualizá tus datos. Para conservar tu contraseña, dejá ese campo vacío.</p>
                </div>
                {isLoading && <div className="form-message info" role="status">Cargando perfil...</div>}
                {!isLoading && error && !profile && <div className="form-message error" role="alert">{error}</div>}
                {profile && (
                    <form className="registration-form" onSubmit={handleSubmit}>
                        {success && <div className="form-message success" role="status">{success}</div>}
                        <div className="form-grid">
                            {fieldsFor(profile).map(({ name, label, fullWidth, optional, socialNetwork, inputKind, ...inputProps }) => {
                                if (name === "city") return null;
                                if (name === "department") return <LocationFields key="location" department={profile.department || ""}
                                    city={profile.city || ""}
                                    onDepartmentChange={department => setProfile(previous => ({ ...previous, department, city: "" }))}
                                    onCityChange={city => setProfile(previous => ({ ...previous, city }))} />;
                                if (name === "phone") return <PhoneInput key={name} value={profile.phone || ""}
                                    onChange={phone => setProfile(previous => ({ ...previous, phone }))} />;
                                if (name === "ci") return <DocumentInput key={name} documentType={profile.documentType || "CI"}
                                    documentNumber={profile.ci || ""}
                                    onTypeChange={documentType => setProfile(previous => ({ ...previous, documentType, ci: "" }))}
                                    onNumberChange={ci => setProfile(previous => ({ ...previous, ci }))} />;
                                return (
                                <label className={`form-field${fullWidth ? " full-width" : ""}`} key={name}>
                                    <span className={socialNetwork ? "social-field-label" : undefined}>
                                        {socialNetwork && <SocialIcon network={socialNetwork} />}{label}{optional ? " (opcional)" : ""}
                                    </span>
                                    <input type="text" {...inputProps}
                                        inputMode={inputProps.inputMode || (inputKind === "digits" ? "numeric" : undefined)}
                                        placeholder={name === "password" && passwordWasChanged ? "••••••••" : inputProps.placeholder}
                                        name={name} value={profile[name] ?? ""}
                                        onChange={handleChange}
                                        required={name === "confirmPassword" ? Boolean(profile.password) : !optional} />
                                    {name === "password" && <PasswordStrength password={profile.password} />}
                                </label>
                                );
                            })}
                        </div>
                        {error && <div className="form-message error" role="alert">{error}</div>}
                        <button className="submit-button" type="submit" disabled={isSaving}>
                            {isSaving ? "Guardando..." : "Guardar cambios"}
                        </button>
                    </form>
                )}
            </section>
        </main>
    );
}
