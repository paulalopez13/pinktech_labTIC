import { useEffect, useState } from "react";
import { getProfile, updateProfile } from "../services/profileService";

const accountFields = [
    { name: "username", label: "Nombre de usuario", minLength: 3, maxLength: 30 },
    { name: "email", label: "Correo electrónico", type: "email", maxLength: 100 },
    { name: "password", label: "Nueva contraseña", type: "password", minLength: 8, maxLength: 30,
        placeholder: "Dejá este campo vacío para mantenerla", optional: true },
    { name: "confirmPassword", label: "Repetir nueva contraseña", type: "password", minLength: 8, maxLength: 30,
        placeholder: "Repetila para cambiarla", optional: true },
];
const touristFields = [
    { name: "firstName", label: "Nombre", maxLength: 50 },
    { name: "lastName", label: "Apellido", maxLength: 50 },
    { name: "ci", label: "Cédula", maxLength: 20 },
    { name: "age", label: "Edad", type: "number", min: 0, max: 120 },
];
const providerFields = [
    { name: "phone", label: "Teléfono", type: "tel", maxLength: 20 },
    { name: "department", label: "Departamento", maxLength: 30 },
    { name: "city", label: "Ciudad", maxLength: 30 },
    { name: "address", label: "Dirección", maxLength: 150, fullWidth: true },
];

function fieldsFor(profile) {
    if (profile.role === "TOURIST") return [...accountFields, ...touristFields];
    if (profile.role === "ADMINISTRATOR") return accountFields;
    if (profile.role === "PROVIDER" && profile.companyName !== null) {
        return [...accountFields,
            { name: "companyName", label: "Nombre de la empresa", maxLength: 30 },
            { name: "rut", label: "RUT", maxLength: 20 },
            ...providerFields];
    }
    return [...accountFields,
        { name: "firstName", label: "Nombre", maxLength: 20 },
        { name: "lastName", label: "Apellido", maxLength: 20 },
        { name: "ci", label: "Cédula", maxLength: 20 },
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
            .then(data => { if (isCurrent) setProfile({ ...data, password: "" }); })
            .catch(loadError => { if (isCurrent) setError(loadError.message); })
            .finally(() => { if (isCurrent) setIsLoading(false); });
        return () => { isCurrent = false; };
    }, []);

    function handleChange(event) {
        const { name, value } = event.target;
        setProfile(previous => ({ ...previous, [name]: name === "age" && value !== "" ? Number(value) : value }));
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
                            {fieldsFor(profile).map(({ name, label, fullWidth, optional, ...inputProps }) => (
                                <label className={`form-field${fullWidth ? " full-width" : ""}`} key={name}>
                                    {label}
                                    <input type="text" {...inputProps}
                                        placeholder={name === "password" && passwordWasChanged ? "••••••••" : inputProps.placeholder}
                                        name={name} value={profile[name] ?? ""}
                                        onChange={handleChange}
                                        required={name === "confirmPassword" ? Boolean(profile.password) : !optional} />
                                </label>
                            ))}
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
