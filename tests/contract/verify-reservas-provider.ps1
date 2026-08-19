[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$consumerPom = Join-Path $PSScriptRoot 'pom.xml'
$pactOutput = Join-Path $PSScriptRoot 'target\pacts'
$serviceDirectory = Join-Path $repositoryRoot 'services\reservas-solicitudes-service'
$mavenWrapper = Join-Path $serviceDirectory 'mvnw.cmd'

Write-Host '1/2 Generando Pact V4 desde ReservasConsumerPactTest...'
& $mavenWrapper -f $consumerPom "-Dpact.rootDir=$pactOutput" test
if ($LASTEXITCODE -ne 0) {
    throw "La prueba Consumer Pact finalizo con codigo $LASTEXITCODE."
}

Write-Host '2/2 Verificando reservas-solicitudes-service contra el Pact generado...'
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

Write-Host 'Consumer 2/2 y Provider 2/2 verificados correctamente.'
