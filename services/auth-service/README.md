
# Auth Service

## Protección de cuentas

El servicio mantiene BCrypt con coste 12 para hashes de contraseña. Tras cinco
credenciales incorrectas, la cuenta queda bloqueada durante 15 minutos. Los
intentos durante ese intervalo no amplían el bloqueo; al vencer, el siguiente
login o refresh limpia automáticamente el contador. Un login correcto también
reinicia el estado y cada intento se registra sin contraseña ni tokens.

## Recuperación de contraseña

`POST /api/v1/auth/forgot-password` emite siempre una respuesta neutra y, para
una cuenta válida, envía un enlace al correo institucional. El token contiene
32 bytes aleatorios, dura 20 minutos y en base solo se conserva su SHA-256.
`POST /api/v1/auth/reset-password` consume el token una sola vez y almacena la
nueva contraseña con BCrypt. BCrypt es un hash no reversible; no se utiliza AES
para contraseñas.

La contraseña nueva debe tener entre 12 y 64 caracteres, no superar 72 bytes
UTF-8, contener mayúscula, minúscula, número y carácter especial, y no contener
espacios. Tampoco puede ser igual a la contraseña actual.
