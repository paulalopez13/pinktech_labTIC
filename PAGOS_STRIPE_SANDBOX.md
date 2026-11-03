# Pagos de prueba con Stripe

La app usa Stripe Checkout en modo sandbox. Spring Boot crea el pago con la API de Stripe; la tarjeta se ingresa en la página segura de Stripe y nunca pasa por la app.

1. Creá una cuenta de Stripe y activá el modo de prueba.
2. En Developers → API keys, creá una clave restringida de prueba (`rk_test_...`) con permiso de lectura y escritura para Checkout Sessions y permiso de escritura para Refunds. No uses una clave `sk_live_...` ni `rk_live_...`.
3. En la carpeta principal del proyecto, reemplazá el valor de `STRIPE_SECRET_KEY` en `.env` por tu clave. El `.env` está ignorado por Git y la clave no se comparte. No pongas comillas ni espacios alrededor del `=`. No agregues `STRIPE_WEBHOOK_SECRET` hasta configurar Stripe CLI.

4. Guardá el archivo. La app lee la clave de `.env` al iniciar un pago, así que no necesitás encontrar ni reiniciar la terminal de Spring Boot.
5. En el checkout de Stripe podés usar `4242 4242 4242 4242`, una fecha futura, cualquier CVC y un código postal cualquiera. Para probar un rechazo, usá `4000 0000 0000 0002`.

El regreso a la app consulta la sesión a Stripe y confirma la reserva. Para recibir eventos instantáneos de webhook en desarrollo, instalá Stripe CLI y ejecutá `stripe listen --forward-to localhost:8080/api/payments/stripe/webhook`; configurá el `whsec_...` que muestra el comando como `STRIPE_WEBHOOK_SECRET` y reiniciá Spring Boot. Los rechazos necesitan el webhook para extender el plazo de pago a dos horas. Sin clave `sk_test_`, la app no puede iniciar el checkout.
