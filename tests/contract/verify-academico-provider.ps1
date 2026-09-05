[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$wrapper = Join-Path $repositoryRoot 'services\reservas-solicitudes-service\mvnw.cmd'
$pactOutput = Join-Path $PSScriptRoot 'target\pacts'
$serviceDirectory = Join-Path $repositoryRoot 'services\academico-laboratorios-service'

& $wrapper -f (Join-Path $PSScriptRoot 'pom.xml') "-Dpact.rootDir=$pactOutput" '-Dtest=MobileAcademicoConsumerPactTest' test
if ($LASTEXITCODE -ne 0) { throw "La generación del Pact Académico falló con código $LASTEXITCODE." }

Push-Location $serviceDirectory
try {
    & $wrapper '-Dtest=AcademicoProviderPactTest' test
    if ($LASTEXITCODE -ne 0) { throw "La verificación Provider Académico falló con código $LASTEXITCODE." }
}
finally { Pop-Location }
