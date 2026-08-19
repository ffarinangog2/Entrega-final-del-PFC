param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('eficiencia_nominal_50u_5m', 'fiabilidad_nominal_50u_1h')]
    [string]$Escenario,

    [Parameter(Mandatory = $true)]
    [ValidateRange(1, 10)]
    [int]$Repeticion,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^https?://')]
    [string]$HostObjetivo,

    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$scenarioConfig = @{
    'eficiencia_nominal_50u_5m' = @{ Duration = '5m'; Range = '5m' }
    'fiabilidad_nominal_50u_1h' = @{ Duration = '1h'; Range = '1h' }
}
$config = $scenarioConfig[$Escenario]
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$locustFile = Join-Path $repositoryRoot 'tests\load\locustfile.py'
$rawRoot = Join-Path $PSScriptRoot 'resultados\raw'
$repetitionName = 'rep-{0:D2}' -f $Repeticion

if ($DryRun) {
    $evidenceDirectory = Join-Path $rawRoot "_dry-run\$Escenario\$repetitionName"
} else {
    $evidenceDirectory = Join-Path $rawRoot "$Escenario\$repetitionName"
}

if ((Test-Path -LiteralPath $evidenceDirectory) -and
        (Get-ChildItem -LiteralPath $evidenceDirectory -Force | Select-Object -First 1)) {
    throw "El directorio de evidencia ya contiene archivos: $evidenceDirectory"
}
New-Item -ItemType Directory -Force -Path $evidenceDirectory | Out-Null

$csvPrefix = Join-Path $evidenceDirectory 'locust'
$htmlReport = Join-Path $evidenceDirectory 'locust-report.html'
$locustLog = Join-Path $evidenceDirectory 'locust.log'
$locustArguments = @(
    '-m', 'locust',
    '-f', $locustFile,
    '--headless',
    '--host', $HostObjetivo,
    '--users', '50',
    '--spawn-rate', '10',
    '--run-time', $config.Duration,
    '--csv', $csvPrefix,
    '--csv-full-history',
    '--html', $htmlReport
)
$displayCommand = 'python ' + (($locustArguments | ForEach-Object {
    if ($_ -match '\s') { '"' + $_ + '"' } else { $_ }
}) -join ' ')

$job = 'reservas-solicitudes-service'
$range = $config.Range
$fiveXxQuery = @"
100 * sum(increase(http_server_requests_seconds_count{job="$job",status=~"5.."}[$range])) / clamp_min(sum(increase(http_server_requests_seconds_count{job="$job"}[$range])), 1)
"@
$fiveXxCountQuery = @"
sum(increase(http_server_requests_seconds_count{job="$job",status=~"5.."}[$range]))
"@
$p95Query = @"
1000 * histogram_quantile(0.95, sum by (le) (increase(http_request_duration_seconds_bucket{job="$job"}[$range])))
"@

Set-Content -LiteralPath (Join-Path $evidenceDirectory 'prometheus-5xx-percent.promql') -Value $fiveXxQuery -Encoding utf8
Set-Content -LiteralPath (Join-Path $evidenceDirectory 'prometheus-5xx-count.promql') -Value $fiveXxCountQuery -Encoding utf8
Set-Content -LiteralPath (Join-Path $evidenceDirectory 'prometheus-p95.promql') -Value $p95Query -Encoding utf8

$plan = [ordered]@{
    status = if ($DryRun) { 'dry-run' } else { 'planned' }
    scenario = $Escenario
    repetition = $Repeticion
    host = $HostObjetivo
    users = 50
    spawn_rate = 10
    duration = $config.Duration
    command = $displayCommand
    evidence_directory = $evidenceDirectory
    created_at_utc = (Get-Date).ToUniversalTime().ToString('o')
    locust_version = $null
    started_at_utc = $null
    finished_at_utc = $null
    exit_code = $null
}
$metadataPath = Join-Path $evidenceDirectory 'metadata.json'
$plan | ConvertTo-Json | Set-Content -LiteralPath $metadataPath -Encoding utf8

Write-Output "Evidencia: $evidenceDirectory"
Write-Output "Comando: $displayCommand"
if ($DryRun) {
    Write-Output 'DRY-RUN: no se ejecutó Locust ni se modificó iso25010.csv.'
    exit 0
}

$locustVersion = python -m locust --version 2>&1
$plan.locust_version = ($locustVersion | Out-String).Trim()
$plan.started_at_utc = (Get-Date).ToUniversalTime().ToString('o')
$plan.status = 'running'
$plan | ConvertTo-Json | Set-Content -LiteralPath $metadataPath -Encoding utf8

& python @locustArguments *> $locustLog
$exitCode = $LASTEXITCODE
$plan.finished_at_utc = (Get-Date).ToUniversalTime().ToString('o')
$plan.exit_code = $exitCode
$plan.status = if ($exitCode -eq 0) { 'completed' } else { 'failed' }
$plan | ConvertTo-Json | Set-Content -LiteralPath $metadataPath -Encoding utf8

Write-Output "Locust finalizó con código $exitCode."
Write-Output 'Guarde la respuesta real de Prometheus como prometheus-5xx-result.txt antes de importar la fila.'
exit $exitCode
