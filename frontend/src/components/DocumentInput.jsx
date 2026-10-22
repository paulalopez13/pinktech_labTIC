import { sanitizeFieldValue } from "./fieldRules";

export default function DocumentInput({ documentType, documentNumber, onTypeChange, onNumberChange, required = true }) {
    const maxLength = documentType === "PASSPORT" ? 9 : 8;
    return (
        <div className="form-field document-field">
            <span>Documento</span>
            <div className="document-input-row">
                <select aria-label="Tipo de documento" value={documentType}
                    onChange={event => onTypeChange(event.target.value)}>
                    <option value="CI">Cédula</option>
                    <option value="PASSPORT">Pasaporte</option>
                </select>
                <input type="text" inputMode={documentType === "CI" ? "numeric" : "text"}
                    autoComplete="off" maxLength={maxLength}
                    placeholder={documentType === "CI" ? "Hasta 8 dígitos" : "Hasta 9 letras o números"}
                    aria-label={documentType === "CI" ? "Número de cédula" : "Número de pasaporte"}
                    value={documentNumber} onChange={event => onNumberChange(sanitizeFieldValue(
                        event.target.value.toUpperCase(),
                        { inputKind: documentType === "CI" ? "digits" : "alphanumeric", maxLength }
                    ))} required={required} />
            </div>
        </div>
    );
}
