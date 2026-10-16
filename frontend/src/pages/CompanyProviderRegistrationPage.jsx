import ProviderRegistrationForm from "./ProviderRegistrationForm.jsx";
import { registerCompany } from "../services/providerService";

const fields = [
    { name: "companyName", label: "Nombre de la empresa", maxLength: 30, autoComplete: "organization" },
    { name: "rut", label: "RUT", maxLength: 20 },
];

export default function CompanyProviderRegistrationPage(props) {
    return <ProviderRegistrationForm {...props} title="Registrar empresa"
        fields={fields} register={registerCompany} />;
}
