import ProviderRegistrationForm from "./ProviderRegistrationForm.jsx";
import { registerIndividual } from "../services/providerService";

const fields = [
    { name: "firstName", label: "Nombre", maxLength: 20, autoComplete: "given-name" },
    { name: "lastName", label: "Apellido", maxLength: 20, autoComplete: "family-name" },
    { name: "ci", label: "Cédula de identidad", maxLength: 20 },
];

export default function IndividualProviderRegistrationPage(props) {
    return <ProviderRegistrationForm {...props} title="Registrar particular"
        fields={fields} register={registerIndividual} />;
}
