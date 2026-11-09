import { useEffect, useRef, useState } from "react";

let stripeScriptPromise;

function loadStripeJs() {
    if (window.Stripe) return Promise.resolve(window.Stripe);
    if (!stripeScriptPromise) {
        stripeScriptPromise = new Promise((resolve, reject) => {
            const script = document.createElement("script");
            script.src = "https://js.stripe.com/v3/";
            script.async = true;
            script.onload = () => window.Stripe ? resolve(window.Stripe) : reject(new Error("No se pudo cargar el formulario seguro de Stripe."));
            script.onerror = () => reject(new Error("No se pudo conectar con Stripe. Revisá tu conexión e intentá de nuevo."));
            document.head.appendChild(script);
        });
    }
    return stripeScriptPromise;
}

const money = value => `$U ${new Intl.NumberFormat("es-UY", { maximumFractionDigits: 2 }).format(Number(value || 0))}`;

export default function StripePaymentModal({ payment, reservation, onVerify, onPaid, onClose }) {
    const cardMount = useRef(null);
    const stripeRef = useRef(null);
    const cardRef = useRef(null);
    const [ready, setReady] = useState(false);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState(false);
    const [remaining, setRemaining] = useState(0);

    useEffect(() => {
        let active = true;
        let card;
        loadStripeJs().then(Stripe => {
            if (!active || !cardMount.current) return;
            const stripe = Stripe(payment.publishableKey);
            const elements = stripe.elements();
            card = elements.create("card", {
                hidePostalCode: true,
                style: { base: { color: "#23433d", fontFamily: "Arial, sans-serif", fontSize: "16px", "::placeholder": { color: "#778780" } }, invalid: { color: "#b42318" } },
            });
            stripeRef.current = stripe;
            cardRef.current = card;
            card.mount(cardMount.current);
            setReady(true);
        }).catch(loadError => active && setError(loadError.message));
        return () => {
            active = false;
            card?.destroy();
            stripeRef.current = null;
            cardRef.current = null;
        };
    }, [payment.publishableKey]);

    useEffect(() => {
        if (!reservation.paymentExpiresAt) return undefined;
        const update = () => setRemaining(Math.max(0, Math.ceil((new Date(reservation.paymentExpiresAt).getTime() - Date.now()) / 1000)));
        update();
        const timer = window.setInterval(update, 1000);
        return () => window.clearInterval(timer);
    }, [reservation.paymentExpiresAt]);

    async function submit(event) {
        event.preventDefault();
        if (!stripeRef.current || !cardRef.current || busy || !ready) return;
        setBusy(true);
        setError("");
        try {
            const result = await stripeRef.current.confirmCardPayment(payment.clientSecret, {
                payment_method: {
                    card: cardRef.current,
                    billing_details: { name: reservation.contactName || undefined, email: reservation.contactEmail || undefined },
                },
            });
            if (result.error) throw new Error(result.error.message || "Stripe no pudo procesar la tarjeta.");
            if (result.paymentIntent?.status !== "succeeded")
                throw new Error("El pago todavía no fue aprobado. Revisá la operación e intentá nuevamente.");
            const verified = await onVerify(result.paymentIntent.id);
            if (verified.paymentStatus !== "APPROVED")
                throw new Error("Stripe recibió el pago, pero todavía estamos verificando la reserva. Revisá Mis reservas en unos segundos.");
            setSuccess(true);
            onPaid?.(verified);
        } catch (submitError) {
            setError(submitError.message || "No se pudo completar el pago.");
        } finally {
            setBusy(false);
        }
    }

    return <div className="stripe-payment-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget && !busy) onClose(); }}>
        <section className="stripe-payment-modal" role="dialog" aria-modal="true" aria-labelledby="stripe-payment-title">
            <button className="stripe-payment-close" type="button" onClick={onClose} disabled={busy} aria-label="Cerrar">×</button>
            {success ? <>
                <div className="stripe-payment-success-icon" aria-hidden="true">✓</div>
                <h2 id="stripe-payment-title">¡Reserva confirmada!</h2>
                <p>Stripe aprobó el pago. Tu lugar quedó reservado.</p>
                <button className="submit-button" type="button" onClick={onClose}>Volver a mis reservas</button>
            </> : <>
                <span className="eyebrow">Pago seguro · Stripe</span>
                <h2 id="stripe-payment-title">Completá el pago</h2>
                <p className="stripe-payment-activity">{reservation.activityName}</p>
                <div className="stripe-payment-summary"><span>{reservation.participantCount} {reservation.participantCount === 1 ? "participante" : "participantes"}</span><strong>{money(reservation.total)}</strong></div>
                {remaining > 0 && <p className="stripe-payment-timer">Tiempo para completar el pago: {String(Math.floor(remaining / 60)).padStart(2, "0")}:{String(remaining % 60).padStart(2, "0")}</p>}
                {remaining === 0 && reservation.paymentExpiresAt && <p className="form-message error">El tiempo para pagar terminó. Cerrá esta ventana y actualizá tus reservas.</p>}
                <form onSubmit={submit}>
                    <label className="stripe-card-label">Datos de la tarjeta</label>
                    <div className="stripe-card-element" ref={cardMount} />
                    <p className="stripe-payment-security">Los datos de tu tarjeta se ingresan de forma segura en Stripe. Uruguay Mnatural no los recibe ni los guarda.</p>
                    {error && <div className="form-message error" role="alert">{error}</div>}
                    <button className="submit-button" type="submit" disabled={!ready || busy || remaining === 0}>
                        {busy ? "Procesando pago…" : `Pagar ${money(reservation.total)}`}
                    </button>
                </form>
            </>}
        </section>
    </div>;
}
