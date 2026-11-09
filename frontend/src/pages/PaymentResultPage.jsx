import { useEffect, useState } from "react";
import { cancelStripeCheckout, verifyStripeCheckout } from "../services/activityService";

export default function PaymentResultPage({ onViewReservations, onHome, onLogin }) {
    const params = new URLSearchParams(window.location.search);
    const outcome = params.get("payment");
    const reservationId = params.get("reservationId");
    const sessionId = params.get("session_id");
    const [state, setState] = useState({ loading: true, paid: false, cancelled: false, paymentStatus: "", error: "" });

    useEffect(() => {
        let active = true;
        const run = async () => {
            try {
                if (!reservationId) throw new Error("No encontramos la reserva asociada a este pago.");
                if (outcome === "cancelled") {
                    const result = await cancelStripeCheckout(reservationId);
                    if (active) setState({ loading: false, paid: result?.paymentStatus === "APPROVED", cancelled: result?.cancelled === true, paymentStatus: result?.paymentStatus || "", error: "" });
                    return;
                }
                if (outcome !== "success" || !sessionId) throw new Error("No encontramos el resultado del pago.");
                const reservation = await verifyStripeCheckout(reservationId, sessionId);
                if (active) setState({ loading: false, paid: reservation.paymentStatus === "APPROVED", paymentStatus: reservation.paymentStatus, error: "" });
            } catch (error) {
                if (active) setState({ loading: false, paid: false, error: error.message });
            }
        };
        run();
        return () => { active = false; };
    }, [outcome, reservationId, sessionId]);

    return <main className="form-card payment-result-page">
        <div className="form-header"><span className="eyebrow">Uruguay Mnatural</span>
            <h1>{state.loading ? "Verificando el pago…" : state.paid ? "¡Reserva confirmada!" : state.paymentStatus === "EXPIRED" ? "La reserva venció" : state.cancelled ? "Pago cancelado" : "Pago pendiente"}</h1>
            <p>{state.loading ? "Estamos consultando el estado de la operación con Stripe." : state.paid ? "El pago se aprobó y tu lugar quedó reservado." : state.paymentStatus === "EXPIRED" ? "La reserva venció y los cupos se liberaron. Si Stripe llegó a cobrar el pago, se tramita su devolución." : state.cancelled ? "No se realizó el cobro. Tu lugar queda apartado durante el tiempo restante para que puedas volver a intentarlo." : state.error || "Stripe todavía no confirmó el pago. Podés revisar el estado en Mis reservas."}</p>
        </div>
        {state.error && <div className="form-message error" role="alert">{state.error}</div>}
        {!state.loading && <div className="activity-step-actions">
            {state.error?.includes("Iniciá sesión como turista")
                ? <button className="submit-button" type="button" onClick={onLogin}>Iniciar sesión para consultar el pago</button>
                : <button className="submit-button" type="button" onClick={onViewReservations}>Ver mis reservas</button>}
            <button className="activity-step-back" type="button" onClick={onHome}>Volver al inicio</button>
        </div>}
    </main>;
}
