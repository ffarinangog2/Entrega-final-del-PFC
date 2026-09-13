[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$consumerPom = Join-Path $PSScriptRoot 'pom.xml'
$pactOutput = Join-Path $PSScriptRoot 'target\pacts'
$serviceDirectory = Join-Path $repositoryRoot 'services\reservas-solicitudes-service'
$mavenWrapper = Join-Path $serviceDirectory 'mvnw.cmd'

Write-Host '1/2 Generando Pacts V4 para consumidores Web (scli-web), Movil (scli-mobile) y Contract Tests...'
& $mavenWrapper -f $consumerPom "-Dpact.rootDir=$pactOutput" test
if ($LASTEXITCODE -ne 0) {
    throw "La generacion de Pact finalizo con codigo $LASTEXITCODE."
}

Write-Host '2/2 Verificando reservas-solicitudes-service contra todos los Pacts generados...'
Push-Location $serviceDirectory
try {
    & $mavenWrapper '-Dtest=ReservasProviderPactTest' test
    if ($LASTEXITCODE -ne 0) {
        throw "La verificacion Provider Pact finalizo con codigo $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

Write-Host 'Contratos de Web, Movil y Provider verificados correctamente.'
