import ProviderRegistrationForm from "./ProviderRegistrationForm.jsx";
import { registerCompany } from "../services/providerService";

const fields = [
    { name: "companyName", label: "Nombre de la empresa", maxLength: 30, autoComplete: "organization", inputKind: "letters" },
    { name: "rut", label: "RUT (12 dígitos)", maxLength: 12, inputKind: "digits", inputMode: "numeric" },
];

export default function CompanyProviderRegistrationPage(props) {
    return <ProviderRegistrationForm {...props} title="Registrar empresa"
        fields={fields} register={registerCompany} />;
}
