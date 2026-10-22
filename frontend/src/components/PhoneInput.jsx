import { useState } from "react";

export const phoneCountries = [
    { name: "Uruguay", code: "598", digits: 8 },
    { name: "Argentina", code: "54", digits: 10 },
    { name: "Brasil", code: "55", digits: 11 },
    { name: "Chile", code: "56", digits: 9 },
    { name: "Paraguay", code: "595", digits: 9 },
    { name: "Bolivia", code: "591", digits: 8 },
    { name: "Perú", code: "51", digits: 9 },
    { name: "Ecuador", code: "593", digits: 9 },
    { name: "Colombia", code: "57", digits: 10 },
    { name: "Venezuela", code: "58", digits: 10 },
    { name: "México", code: "52", digits: 10 },
    { name: "Estados Unidos", code: "1", digits: 10 },
    { name: "Canadá", code: "1", digits: 10 },
    { name: "España", code: "34", digits: 9 },
    { name: "Portugal", code: "351", digits: 9 },
    { name: "Francia", code: "33", digits: 9 },
    { name: "Italia", code: "39", digits: 10 },
    { name: "Alemania", code: "49", digits: 11 },
    { name: "Reino Unido", code: "44", digits: 10 },
    { name: "Países Bajos", code: "31", digits: 9 },
    { name: "Bélgica", code: "32", digits: 9 },
    { name: "Suiza", code: "41", digits: 9 },
    { name: "Suecia", code: "46", digits: 9 },
    { name: "Noruega", code: "47", digits: 8 },
    { name: "Irlanda", code: "353", digits: 9 },
    { name: "Australia", code: "61", digits: 9 },
    { name: "Nueva Zelanda", code: "64", digits: 9 },
    { name: "Japón", code: "81", digits: 10 },
    { name: "China", code: "86", digits: 11 },
    { name: "India", code: "91", digits: 10 },
    { name: "Corea del Sur", code: "82", digits: 10 },
    { name: "Sudáfrica", code: "27", digits: 9 },
    { name: "Costa Rica", code: "506", digits: 8 },
    { name: "Cuba", code: "53", digits: 8 },
    { name: "República Dominicana", code: "1", digits: 10 },
    { name: "Guatemala", code: "502", digits: 8 },
    { name: "Honduras", code: "504", digits: 8 },
    { name: "Nicaragua", code: "505", digits: 8 },
    { name: "Panamá", code: "507", digits: 8 },
    { name: "El Salvador", code: "503", digits: 8 },
];

function phoneParts(value = "") {
    if (!value.startsWith("+")) {
        const digits = value.replace(/\D/g, "");
        return { country: phoneCountries[0], national: digits.startsWith("0") ? digits.slice(1, 9) : digits.slice(0, 8) };
    }
    const digits = value.slice(1).replace(/\D/g, "");
    const country = [...phoneCountries].sort((a, b) => b.code.length - a.code.length)
        .find(option => digits.startsWith(option.code)) || phoneCountries[0];
    return { country, national: digits.slice(country.code.length, country.code.length + country.digits) };
}

export default function PhoneInput({ value, onChange, required = true }) {
    const [countryName, setCountryName] = useState(() => phoneParts(value).country.name);
    const selectedCountry = phoneCountries.find(option => option.name === countryName) || phoneCountries[0];
    const parsed = phoneParts(value);
    const national = value?.startsWith(`+${selectedCountry.code}`)
        ? value.slice(selectedCountry.code.length + 1).replace(/\D/g, "").slice(0, selectedCountry.digits)
        : parsed.national;
    function changeCountry(event) {
        const next = phoneCountries.find(option => option.name === event.target.value) || phoneCountries[0];
        setCountryName(next.name);
        onChange(`+${next.code}${national.slice(0, next.digits)}`);
    }
    function changeNumber(event) {
        const digits = event.target.value.replace(/\D/g, "").slice(0, selectedCountry.digits);
        onChange(`+${selectedCountry.code}${digits}`);
    }

    return (
        <label className="form-field phone-field">
            Teléfono
            <div className="phone-input-row">
                <select aria-label="País del teléfono" value={selectedCountry.name} onChange={changeCountry}>
                    {phoneCountries.map(option => <option key={option.name} value={option.name}>
                        {option.name} (+{option.code})
                    </option>)}
                </select>
                <div className="phone-number-box">
                    <span>+{selectedCountry.code}</span>
                    <input type="tel" inputMode="numeric" autoComplete="tel-national"
                        maxLength={selectedCountry.digits} value={national} onChange={changeNumber}
                        placeholder={`Hasta ${selectedCountry.digits} dígitos`} required={required} />
                </div>
            </div>
        </label>
    );
}

