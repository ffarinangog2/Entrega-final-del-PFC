# Datos DEMO institucionales

Estos scripts crean datos ficticios y deterministas únicamente para desarrollo. No forman parte de Flyway ni se ejecutan en producción.

Incluyen una facultad, carrera, período activo, campus, bloque, dos pisos, dos laboratorios, dos materias, dos horarios, un docente y un administrador del piso A. Los UUID externos coinciden entre Académico, Usuarios y Auth; no se crean claves foráneas entre servicios.

Antes de ejecutarlos, aplique normalmente las migraciones de los servicios y proporcione hashes BCrypt generados fuera del repositorio:

```powershell
$env:DEMO_DOCENTE_PASSWORD_HASH = '<hash BCrypt no registrado en Git>'
$env:DEMO_ADMIN_PISO_PASSWORD_HASH = '<hash BCrypt no registrado en Git>'
.\scripts\demo\seed-demo.ps1
```

El script rechaza texto que no tenga formato BCrypt, no imprime los hashes y usa `INSERT ... ON CONFLICT` para poder repetirse. Los nombres de usuario resultantes son `demo.docente` y `demo.admin.piso`; sus contraseñas son únicamente las correspondientes a los hashes suministrados localmente.

Para el docente DEMO, el flujo de catálogos usa:

- `GET /api/v1/docentes/perfil/{perfilId}` para resolver `docentes.id`.
- `GET /api/v1/horarios/docente/{docenteId}` para derivar materia, período y laboratorio.
- `GET /api/v1/materias`, `GET /api/v1/periodos-lectivos/actual` y `GET /api/v1/laboratorios` como catálogos protegidos por Bearer.
