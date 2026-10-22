import ProviderRegistrationForm from "./ProviderRegistrationForm.jsx";
import { registerIndividual } from "../services/providerService";

const fields = [
    { name: "firstName", label: "Nombre", maxLength: 20, autoComplete: "given-name", inputKind: "letters" },
    { name: "lastName", label: "Apellido", maxLength: 20, autoComplete: "family-name", inputKind: "letters" },
    { name: "ci", label: "Documento", maxLength: 8 },
];

export default function IndividualProviderRegistrationPage(props) {
    return <ProviderRegistrationForm {...props} title="Registrar particular"
        fields={fields} register={registerIndividual} />;
}
