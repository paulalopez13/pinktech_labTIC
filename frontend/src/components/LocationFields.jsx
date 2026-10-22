import { uruguayLocations } from "./uruguayLocations";

export default function LocationFields({ department, city, onDepartmentChange, onCityChange }) {
    const cities = uruguayLocations[department] || [];
    const options = city && !cities.includes(city) ? [...cities, city] : cities;
    return <>
        <label className="form-field">
            Departamento
            <select value={department} onChange={event => onDepartmentChange(event.target.value)} required>
                <option value="" disabled>Elegí un departamento</option>
                {Object.keys(uruguayLocations).map(name => <option key={name} value={name}>{name}</option>)}
            </select>
        </label>
        <label className="form-field">
            Ciudad o localidad
            <select value={city} onChange={event => onCityChange(event.target.value)} required disabled={!department}>
                <option value="" disabled>{department ? "Elegí una ciudad o localidad" : "Primero elegí un departamento"}</option>
                {options.map(name => <option key={name} value={name}>{name}</option>)}
            </select>
        </label>
    </>;
}
