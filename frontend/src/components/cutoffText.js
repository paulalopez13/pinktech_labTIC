// Texto para "Reservas cierran: ..." a partir de los minutos antes del inicio.
// 0 -> "a la hora de inicio", 30 -> "30 minutos antes", 90 -> "1 h 30 min antes", 1440 -> "1 día antes".
export function cutoffText(minutes) {
    const value = Number(minutes) || 0;
    if (value === 0) return "a la hora de inicio";
    if (value % 1440 === 0) return `${value / 1440} ${value === 1440 ? "día" : "días"} antes`;
    if (value % 60 === 0) return `${value / 60} ${value === 60 ? "hora" : "horas"} antes`;
    if (value > 60) return `${Math.floor(value / 60)} h ${value % 60} min antes`;
    return `${value} ${value === 1 ? "minuto" : "minutos"} antes`;
}
