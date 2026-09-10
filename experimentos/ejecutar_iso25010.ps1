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
    [ValidatePattern('^https?://')]
    [string]$PrometheusUrl = 'http://localhost:9090',
    [string]$ComposeFile = 'docker-compose.yml',
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
if (Test-Path variable:PSNativeCommandUseErrorActionPreference) {
    $PSNativeCommandUseErrorActionPreference = $false
}
$pythonCommand = if (Get-Command python -ErrorAction SilentlyContinue) {
    'python'
} elseif (Get-Command python3 -ErrorAction SilentlyContinue) {
    'python3'
} else {
    throw 'No se encontró un intérprete Python. Instale python o python3 y asegúrese de que esté disponible en PATH.'
}
$scenarioConfig = @{
    eficiencia_nominal_50u_5m = @{ Duration = '5m'; Range = '5m'; Seconds = 300 }
    fiabilidad_nominal_50u_1h = @{ Duration = '1h'; Range = '1h'; Seconds = 3600 }
}
$config = $scenarioConfig[$Escenario]
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$composePath = Join-Path $repositoryRoot $ComposeFile
$locustFile = Join-Path $repositoryRoot 'tests/load/locustfile.py'
$rawRoot = Join-Path $PSScriptRoot 'resultados/raw'
$repetitionName = 'rep-{0:D2}' -f $Repeticion
$evidenceDirectory = if ($DryRun) {
    Join-Path $rawRoot "_dry-run/$Escenario/$repetitionName"
} else {
    Join-Path $rawRoot "$Escenario/$repetitionName"
}
if ((Test-Path -LiteralPath $evidenceDirectory) -and
        (Get-ChildItem -LiteralPath $evidenceDirectory -Force | Select-Object -First 1)) {
    throw "El directorio de evidencia ya contiene archivos: $evidenceDirectory"
}
New-Item -ItemType Directory -Force -Path $evidenceDirectory | Out-Null

function Write-Evidence([string]$Name, [AllowEmptyString()][string]$Content) {
    Set-Content -LiteralPath (Join-Path $evidenceDirectory $Name) -Value $Content -Encoding utf8
}
function Invoke-Captured([string]$Name, [string]$Command, [string[]]$Arguments) {
    $output = & $Command @Arguments 2>&1 | Out-String
    $code = $LASTEXITCODE
    Write-Evidence $Name ($output.TrimEnd())
    if ($code -ne 0) { throw "$Command finalizó con código $code; consulte $Name" }
}
function Invoke-HttpCapture([string]$Name, [string]$Uri) {
    $response = Invoke-WebRequest -UseBasicParsing -Uri $Uri -TimeoutSec 30
    Write-Evidence $Name $response.Content
}
function Invoke-PrometheusQuery([string]$Name, [string]$Query, [long]$Epoch) {
    $encoded = [uri]::EscapeDataString($Query.Trim())
    Invoke-HttpCapture $Name "$($PrometheusUrl.TrimEnd('/'))/api/v1/query?query=$encoded&time=$Epoch"
}
function Get-DeploymentFingerprint {
    $ids = & docker compose -f $composePath ps -q 2>&1
    if ($LASTEXITCODE -ne 0 -or -not $ids) { throw 'No se pudo identificar el despliegue.' }
    $values = foreach ($id in $ids) {
        $value = & docker inspect --format '{{.Id}} {{.Config.Image}} {{.Image}}' $id 2>&1
        if ($LASTEXITCODE -ne 0) { throw "No se pudo inspeccionar $id." }
        $value
    }
    return ($values | Sort-Object) -join "`n"
}
function Get-ExperimentGitStatus {
    $scenarioEvidence = Join-Path $rawRoot $Escenario
    $relativeEvidence = $scenarioEvidence.Substring($repositoryRoot.Length).TrimStart('\', '/').Replace('\', '/')
    $lines = & git -C $repositoryRoot status --porcelain --untracked-files=all 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar el estado Git.' }
    return ($lines | Where-Object {
        $path = if ($_.Length -gt 3) { $_.Substring(3).Replace('\', '/') } else { '' }
        -not $path.StartsWith("$relativeEvidence/")
    }) -join "`n"
}

$csvPrefix = Join-Path $evidenceDirectory 'locust'
$locustLog = Join-Path $evidenceDirectory 'locust.log'
$locustArguments = @(
    '-m', 'locust', '-f', $locustFile, '--headless', '--host', $HostObjetivo,
    '--users', '50', '--spawn-rate', '10', '--run-time', $config.Duration,
    '--csv', $csvPrefix, '--csv-full-history',
    '--html', (Join-Path $evidenceDirectory 'locust-report.html')
)
$displayCommand = $pythonCommand + ' ' + (($locustArguments | ForEach-Object {
    if ($_ -match '\s') { '"' + $_ + '"' } else { $_ }
}) -join ' ')
$job = 'reservas-solicitudes-service'
$range = $config.Range
$fiveXxPercent = "100 * sum(increase(http_server_requests_seconds_count{job=`"$job`",status=~`"5..`"}[$range])) / clamp_min(sum(increase(http_server_requests_seconds_count{job=`"$job`"}[$range])), 1)"
$fiveXxCount = "sum(increase(http_server_requests_seconds_count{job=`"$job`",status=~`"5..`"}[$range]))"
$p95 = "1000 * histogram_quantile(0.95, sum by (le) (increase(http_request_duration_seconds_bucket{job=`"$job`"}[$range])))"
Write-Evidence 'prometheus-5xx-percent.promql' $fiveXxPercent
Write-Evidence 'prometheus-5xx-count.promql' $fiveXxCount
Write-Evidence 'prometheus-p95.promql' $p95

$gitBranch = (& git -C $repositoryRoot branch --show-current 2>&1 | Out-String).Trim()
$gitSha = (& git -C $repositoryRoot rev-parse HEAD 2>&1 | Out-String).Trim()
$gitStatusBefore = Get-ExperimentGitStatus
$pythonVersion = (& $pythonCommand --version 2>&1 | Out-String).Trim()
$metadata = [ordered]@{
    status = if ($DryRun) { 'dry-run' } else { 'planned' }
    scenario = $Escenario; repetition = $Repeticion; host = $HostObjetivo
    prometheus_url = $PrometheusUrl; users = 50; spawn_rate = 10
    planned_duration = $config.Duration; planned_duration_seconds = $config.Seconds
    command = $displayCommand; created_at_utc = (Get-Date).ToUniversalTime().ToString('o')
    git_branch = $gitBranch; git_sha = $gitSha
    git_worktree_clean_before = [string]::IsNullOrEmpty($gitStatusBefore)
    git_status_before = $gitStatusBefore; python_version = $pythonVersion
    locust_version = $null; deployment_fingerprint_before = $null
    deployment_fingerprint_after = $null; environment_consistent = $false
    started_at_utc = $null; finished_at_utc = $null; elapsed_seconds = $null
    locust_exit_code = $null; duration_completed = $false
    execution_completed = $false; evidence_complete = $false; launcher_error = $null
}
$metadataPath = Join-Path $evidenceDirectory 'metadata.json'
$metadata | ConvertTo-Json -Depth 5 | Set-Content $metadataPath -Encoding utf8
Write-Output "Evidencia: $evidenceDirectory"
Write-Output "Comando: $displayCommand"
if ($DryRun) { Write-Output 'DRY-RUN: no se ejecutó Locust ni se modificó iso25010.csv.'; exit 0 }

$locustExitCode = $null
$startTime = $null
$stopwatch = $null
try {
    foreach ($command in @('git', 'docker')) {
        if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { throw "Falta $command." }
    }
    if (-not $env:LOCUST_USERNAME -or -not $env:LOCUST_PASSWORD) {
        throw 'LOCUST_USERNAME y LOCUST_PASSWORD son obligatorios.'
    }
    if (-not (Test-Path $composePath -PathType Leaf)) { throw "No existe $composePath." }
    $metadata.locust_version = (& $pythonCommand -m locust --version 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar la versión de Locust.' }
    $metadata.deployment_fingerprint_before = Get-DeploymentFingerprint
    Write-Evidence 'deployment-state-before.txt' $metadata.deployment_fingerprint_before
    Invoke-HttpCapture 'prometheus-health-before.txt' "$($PrometheusUrl.TrimEnd('/'))/-/healthy"
    Invoke-HttpCapture 'gateway-health-before.json' "$($HostObjetivo.TrimEnd('/'))/actuator/health"
    Invoke-Captured 'reservas-health-before.json' docker @('compose','-f',$composePath,'ps','--format','json','reservas-solicitudes-service')
    Invoke-Captured 'docker-stats-before.txt' docker @('stats','--no-stream')
    Invoke-Captured 'cockroach-containers-before.txt' docker @('compose','-f',$composePath,'ps','--format','json','crdb-e3-1','crdb-e3-2','crdb-e3-3')
    Write-Evidence 'environment.txt' (@(
        "captured_at_utc=$((Get-Date).ToUniversalTime().ToString('o'))",
        "git_branch=$gitBranch", "git_sha=$gitSha",
        "git_worktree_clean=$($metadata.git_worktree_clean_before)",
        "host=$HostObjetivo", "prometheus_url=$PrometheusUrl", "compose_file=$ComposeFile",
        "python=$pythonVersion", "locust=$($metadata.locust_version)",
        "os=$([System.Runtime.InteropServices.RuntimeInformation]::OSDescription)",
        "architecture=$([System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture)",
        "processor_count=$([Environment]::ProcessorCount)"
    ) -join "`n")
    $startTime = (Get-Date).ToUniversalTime()
    $metadata.started_at_utc = $startTime.ToString('o'); $metadata.status = 'running'
    $metadata | ConvertTo-Json -Depth 5 | Set-Content $metadataPath -Encoding utf8
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    & $pythonCommand @locustArguments *> $locustLog
    $locustExitCode = $LASTEXITCODE
    $stopwatch.Stop()
} catch {
    $metadata.launcher_error = $_.Exception.Message
    if ($stopwatch -and $stopwatch.IsRunning) { $stopwatch.Stop() }
} finally {
    $finishTime = (Get-Date).ToUniversalTime()
    $metadata.finished_at_utc = $finishTime.ToString('o')
    $metadata.elapsed_seconds = if ($stopwatch) { [math]::Round($stopwatch.Elapsed.TotalSeconds, 3) } else { 0 }
    $metadata.locust_exit_code = $locustExitCode
    $statsPath = Join-Path $evidenceDirectory 'locust_stats.csv'
    $metadata.duration_completed = [bool]($stopwatch -and
        $stopwatch.Elapsed.TotalSeconds -ge ($config.Seconds - 5) -and
        (Test-Path $statsPath) -and (Get-Item $statsPath).Length -gt 0)
    try {
        if ($startTime) {
            $epoch = ([DateTimeOffset]$finishTime).ToUnixTimeSeconds()
            Invoke-PrometheusQuery 'prometheus-5xx-result.txt' $fiveXxCount $epoch
            Invoke-PrometheusQuery 'prometheus-5xx-percent-result.txt' $fiveXxPercent $epoch
            Invoke-PrometheusQuery 'prometheus-p95-result.txt' $p95 $epoch
            Invoke-HttpCapture 'prometheus-health-after.txt' "$($PrometheusUrl.TrimEnd('/'))/-/healthy"
            Invoke-HttpCapture 'gateway-health-after.json' "$($HostObjetivo.TrimEnd('/'))/actuator/health"
            Invoke-Captured 'reservas-health-after.json' docker @('compose','-f',$composePath,'ps','--format','json','reservas-solicitudes-service')
            Invoke-Captured 'docker-stats-after.txt' docker @('stats','--no-stream')
            Invoke-Captured 'cockroach-containers-after.txt' docker @('compose','-f',$composePath,'ps','--format','json','crdb-e3-1','crdb-e3-2','crdb-e3-3')
            Invoke-Captured 'reservas-service.log' docker @('compose','-f',$composePath,'logs','--no-color','--since',$startTime.ToString('o'),'reservas-solicitudes-service')
        }
        $metadata.deployment_fingerprint_after = Get-DeploymentFingerprint
        Write-Evidence 'deployment-state-after.txt' $metadata.deployment_fingerprint_after
        $branchAfter = (& git -C $repositoryRoot branch --show-current | Out-String).Trim()
        $shaAfter = (& git -C $repositoryRoot rev-parse HEAD | Out-String).Trim()
        $statusAfter = Get-ExperimentGitStatus
        $metadata.environment_consistent = [bool](
            $branchAfter -eq $gitBranch -and $shaAfter -eq $gitSha -and
            $statusAfter -eq $gitStatusBefore -and
            $metadata.deployment_fingerprint_after -eq $metadata.deployment_fingerprint_before)
        $required = @(
            'locust_stats.csv','locust_stats_history.csv','locust_failures.csv','locust_exceptions.csv',
            'locust-report.html','locust.log','prometheus-5xx-result.txt',
            'prometheus-5xx-percent-result.txt','prometheus-p95-result.txt','prometheus-health-before.txt',
            'prometheus-health-after.txt',
            'gateway-health-before.json','gateway-health-after.json','reservas-health-before.json',
            'reservas-health-after.json','docker-stats-before.txt','docker-stats-after.txt',
            'cockroach-containers-before.txt','cockroach-containers-after.txt',
            'deployment-state-before.txt','deployment-state-after.txt','reservas-service.log','environment.txt')
        $missing = @($required | Where-Object {
            $path = Join-Path $evidenceDirectory $_
            -not (Test-Path $path -PathType Leaf) -or (Get-Item $path).Length -eq 0
        })
        $metadata.evidence_complete = $missing.Count -eq 0
        if ($missing) { $metadata.launcher_error = "Evidencia ausente o vacía: $($missing -join ', ')" }
    } catch {
        $metadata.launcher_error = $_.Exception.Message; $metadata.evidence_complete = $false
    }
    $metadata.execution_completed = [bool]($metadata.duration_completed -and
        $metadata.environment_consistent -and $metadata.evidence_complete)
    $metadata.status = if ($metadata.execution_completed) { 'completed' } else { 'aborted' }
    $metadata | ConvertTo-Json -Depth 5 | Set-Content $metadataPath -Encoding utf8
}
Write-Output "Código real de Locust: $locustExitCode"
Write-Output "Ejecución experimental completada: $($metadata.execution_completed)"
if (-not $metadata.execution_completed) { exit 2 }
exit $locustExitCode
