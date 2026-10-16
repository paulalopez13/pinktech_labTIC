# Configuración de recuperación de contraseña

La aplicación envía enlaces con Spring Boot Mail. El backend no contiene credenciales SMTP: las lee del entorno al arrancar.

En PowerShell, en la misma ventana desde la que se inicia Spring Boot, definí los datos que te da tu proveedor SMTP:

```powershell
$env:SMTP_HOST = "smtp.tu-proveedor.com"
$env:SMTP_PORT = "587"
$env:SMTP_USERNAME = "tu-cuenta-de-envio"
$env:SMTP_PASSWORD = "tu-clave-o-clave-de-aplicacion"
$env:SMTP_FROM = "cuenta-verificada@tudominio.com"
$env:APP_FRONTEND_URL = "http://127.0.0.1:5173"
```

Después iniciá o reiniciá el backend desde esa misma ventana. Para Gmail, usá una contraseña de aplicación creada en Google; no uses la contraseña normal de la cuenta. No compartas esos valores en el chat ni los guardes en Git.

El enlace vence a los 20 minutos y se invalida después de usarse. Para pedir otro enlace, hay una espera de 60 segundos por email. Si el SMTP no está configurado, la pantalla informa que el servidor todavía no puede enviar correos.
