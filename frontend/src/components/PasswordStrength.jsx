import { passwordStrength } from "./fieldRules";

const labels = { weak: "Insegura", medium: "Poco segura", strong: "Segura" };

export default function PasswordStrength({ password }) {
    if (!password) return null;
    const strength = passwordStrength(password);
    const bars = strength === "strong" ? 3 : strength === "medium" ? 2 : 1;
    return (
        <div className={`password-strength ${strength}`} aria-live="polite">
            <div className="password-strength-bars" aria-hidden="true">
                {[1, 2, 3].map(index => <span key={index} className={index <= bars ? "active" : ""} />)}
            </div>
            <span>{labels[strength]}</span>
        </div>
    );
}
