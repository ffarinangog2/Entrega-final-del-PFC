[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($env:DEMO_DOCENTE_PASSWORD_HASH) -or
    [string]::IsNullOrWhiteSpace($env:DEMO_ADMIN_PISO_PASSWORD_HASH)) {
    throw 'Defina DEMO_DOCENTE_PASSWORD_HASH y DEMO_ADMIN_PISO_PASSWORD_HASH con hashes BCrypt.'
}

$bcryptPattern = '^\$2[aby]\$\d{2}\$.{53}$'
if ($env:DEMO_DOCENTE_PASSWORD_HASH -notmatch $bcryptPattern -or
    $env:DEMO_ADMIN_PISO_PASSWORD_HASH -notmatch $bcryptPattern) {
    throw 'Las credenciales DEMO deben proporcionarse como hashes BCrypt válidos; no se aceptan passwords en texto plano.'
}

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path

function Invoke-DemoSql {
    param(
        [Parameter(Mandatory)] [string] $Service,
        [Parameter(Mandatory)] [string] $Database,
        [Parameter(Mandatory)] [string] $File
    )
    $sql = Get-Content -LiteralPath (Join-Path $scriptDirectory $File) -Raw
    & docker compose exec -T $Service cockroach sql --certs-dir=/cockroach/cockroach-certs --database $Database --execute $sql
    if ($LASTEXITCODE -ne 0) {
        throw "Falló el seed DEMO de $Database."
    }
}

Invoke-DemoSql -Service 'cockroach-academico' -Database 'academico_db' -File 'academico-demo.sql'
Invoke-DemoSql -Service 'cockroach-usuarios' -Database 'usuarios_db' -File 'usuarios-demo.sql'

$authSql = Get-Content -LiteralPath (Join-Path $scriptDirectory 'auth-demo.sql') -Raw
$authSql = $authSql.Replace('__DOCENTE_PASSWORD_HASH__', $env:DEMO_DOCENTE_PASSWORD_HASH)
$authSql = $authSql.Replace('__ADMIN_PISO_PASSWORD_HASH__', $env:DEMO_ADMIN_PISO_PASSWORD_HASH)
& docker compose exec -T cockroach-auth cockroach sql --certs-dir=/cockroach/cockroach-certs --database auth_db --execute $authSql
if ($LASTEXITCODE -ne 0) {
    throw 'Falló el seed DEMO de auth_db.'
}

Write-Host 'Datos DEMO aplicados de forma idempotente. Las contraseñas no fueron impresas.'
