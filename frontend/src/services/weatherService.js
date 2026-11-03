const geocodeCache = new Map();
const forecastCache = new Map();

async function findCoordinates(city, department, signal) {
    const query = city || department;
    if (!query) throw new Error("No hay una ubicación disponible para esta actividad.");
    if (!geocodeCache.has(query)) {
        const url = new URL("https://geocoding-api.open-meteo.com/v1/search");
        url.search = new URLSearchParams({ name: query, count: "10", language: "es", format: "json", countryCode: "UY" });
        const response = await fetch(url, { signal });
        if (!response.ok) throw new Error("No pudimos consultar la ubicación.");
        const results = (await response.json()).results || [];
        const uruguayResult = results.find(result => result.country_code === "UY");
        if (!uruguayResult) throw new Error("No encontramos la ubicación de esta actividad.");
        geocodeCache.set(query, { latitude: uruguayResult.latitude, longitude: uruguayResult.longitude });
    }
    return geocodeCache.get(query);
}

function describeWeather(code) {
    if (code === 0) return ["☀️", "Despejado"];
    if (code === 1) return ["🌤️", "Mayormente despejado"];
    if (code === 2) return ["⛅", "Parcialmente nublado"];
    if (code === 3) return ["☁️", "Nublado"];
    if (code === 45 || code === 48) return ["🌫️", "Niebla"];
    if ([51, 53, 55, 56, 57].includes(code)) return ["🌦️", "Llovizna"];
    if ([61, 63, 65, 66, 67, 80, 81, 82].includes(code)) return ["🌧️", "Lluvia"];
    if ([71, 73, 75, 77, 85, 86].includes(code)) return ["🌨️", "Nieve"];
    if ([95, 96, 99].includes(code)) return ["⛈️", "Tormenta"];
    return ["🌤️", "Estado del tiempo variable"];
}

export async function getWeatherPreview({ city, department, activityStart }, signal) {
    const date = activityStart.slice(0, 10);
    const hour = Number(activityStart.slice(11, 13));
    const target = new Date(`${date}T12:00:00-03:00`);
    const today = new Date();
    const todayInUruguay = new Date(today.toLocaleString("en-US", { timeZone: "America/Montevideo" }));
    todayInUruguay.setHours(0, 0, 0, 0);
    const daysAhead = Math.round((target - todayInUruguay) / 86400000);
    if (daysAhead < 0) throw new Error("El pronóstico no está disponible para fechas pasadas.");
    if (daysAhead > 15) return { unavailable: true, message: "El pronóstico estará disponible cuando se acerque la fecha." };

    const { latitude, longitude } = await findCoordinates(city, department, signal);
    const locationKey = `${latitude},${longitude}`;
    if (!forecastCache.has(locationKey)) {
        const url = new URL("https://api.open-meteo.com/v1/forecast");
        url.search = new URLSearchParams({
            latitude: String(latitude), longitude: String(longitude),
            hourly: "temperature_2m,precipitation_probability,weather_code,wind_speed_10m",
            timezone: "America/Montevideo", forecast_days: "16"
        });
        const response = await fetch(url, { signal });
        if (!response.ok) throw new Error("No pudimos cargar el pronóstico ahora.");
        forecastCache.set(locationKey, await response.json());
    }
    const forecast = forecastCache.get(locationKey);
    const hourKey = `${date}T${String(hour).padStart(2, "0")}:00`;
    const index = forecast.hourly.time.findIndex(time => time === hourKey);
    if (index < 0) return { unavailable: true, message: "El pronóstico para ese horario todavía no está disponible." };
    const [icon, condition] = describeWeather(forecast.hourly.weather_code[index]);
    return {
        icon, condition,
        temperature: Math.round(forecast.hourly.temperature_2m[index]),
        rain: forecast.hourly.precipitation_probability?.[index],
        wind: Math.round(forecast.hourly.wind_speed_10m[index])
    };
}
