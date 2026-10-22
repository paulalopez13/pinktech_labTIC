export function sanitizeFieldValue(value, { inputKind, maxLength }) {
    let result = value;
    if (inputKind === "digits") result = result.replace(/\D/g, "");
    if (inputKind === "letters") result = result.replace(/[^\p{L}\p{M} '\-]/gu, "");
    if (inputKind === "alphanumeric") result = result.replace(/[^\p{L}\p{M}\d]/gu, "");
    if (inputKind === "username") result = result.replace(/[^\p{L}\p{M}\d._-]/gu, "");
    return maxLength ? result.slice(0, maxLength) : result;
}

export function passwordStrength(password = "") {
    const validLength = password.length >= 8 && password.length <= 30;
    const hasLower = /\p{Ll}/u.test(password);
    const hasUpper = /\p{Lu}/u.test(password);
    const hasNumber = /\d/.test(password);
    const hasSymbol = /[\p{P}\p{S}]/u.test(password);

    if (validLength && hasLower && hasUpper && hasNumber && hasSymbol) return "strong";
    // Once the password has an acceptable length and includes letters and
    // numbers, adding more character types must not make its rating go down.
    if (validLength && hasLower && hasNumber) return "medium";
    return "weak";
}
