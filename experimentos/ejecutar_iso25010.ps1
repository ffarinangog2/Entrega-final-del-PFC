param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('eficiencia_nominal_50u_5m', 'fiabilidad_nominal_50u_1h', 'fiabilidad_nominal_50u_1h_refresh')]
    [string]$Escenario,
    [ValidateRange(0, 10)]
    [int]$Repeticion = 0,
    [ValidateRange(1, 99)]
    [int]$Intento = 1,
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^https?://')]
    [string]$HostObjetivo,
    [ValidatePattern('^https?://')]
    [string]$PrometheusUrl = 'http://localhost:9090',
    [string]$ComposeFile = 'docker-compose.yml',
    [string]$EvidenceRoot,
    [switch]$Precheck,
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
    fiabilidad_nominal_50u_1h_refresh = @{ Duration = '1h'; Range = '1h'; Seconds = 3600 }
}
if ($Precheck -and $Escenario -ne 'fiabilidad_nominal_50u_1h') {
    throw '-Precheck sólo es válido con fiabilidad_nominal_50u_1h.'
}
if ($Precheck -and $Repeticion -ne 0) {
    throw '-Precheck no acepta -Repeticion porque no cuenta como repetición oficial.'
}
if ($Precheck -and $DryRun) {
    throw '-Precheck y -DryRun son modos excluyentes.'
}
if (-not $Precheck -and $Repeticion -notin 1..10) {
    throw 'Las ejecuciones oficiales requieren -Repeticion entre 1 y 10.'
}
$config = if ($Precheck) {
    @{ Duration = '30s'; Range = '30s'; Seconds = 30 }
} else {
    $scenarioConfig[$Escenario]
}
$users = if ($Precheck) { 2 } else { 50 }
$spawnRate = if ($Precheck) { 1 } else { 10 }
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$composePath = Join-Path $repositoryRoot $ComposeFile
$locustRelativeFile = if ($Escenario -eq 'fiabilidad_nominal_50u_1h_refresh') {
    'tests/load/locustfile_e2_correctiva.py'
} else {
    'tests/load/locustfile.py'
}
$locustFile = Join-Path $repositoryRoot $locustRelativeFile
$rawRoot = if ($EvidenceRoot) {
    if ([System.IO.Path]::IsPathRooted($EvidenceRoot)) {
        [System.IO.Path]::GetFullPath($EvidenceRoot)
    } else {
        [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $EvidenceRoot))
    }
} else {
    Join-Path $PSScriptRoot 'resultados/raw'
}
$repetitionName = if ($Precheck) {
    $null
} elseif ($Intento -eq 1) {
    'rep-{0:D2}' -f $Repeticion
} else {
    'rep-{0:D2}-attempt-{1:D2}' -f $Repeticion, $Intento
}
$evidenceDirectory = if ($Precheck) {
    Join-Path $rawRoot '_precheck/fiabilidad_nominal_50u_1h'
} elseif ($DryRun) {
    Join-Path $rawRoot "_dry-run/$Escenario/$repetitionName"
} else {
    Join-Path $rawRoot "$Escenario/$repetitionName"
}
if ((Test-Path -LiteralPath $evidenceDirectory -PathType Leaf)) {
    throw "La ruta de evidencia existe y no es un directorio: $evidenceDirectory"
}
if ((Test-Path -LiteralPath $evidenceDirectory) -and
        (Get-ChildItem -LiteralPath $evidenceDirectory -Force | Select-Object -First 1)) {
    throw "El directorio de evidencia ya contiene archivos: $evidenceDirectory"
}
if (-not $DryRun -and $Intento -gt 1) {
    $previousAttempt = $Intento - 1
    $previousName = if ($previousAttempt -eq 1) {
        'rep-{0:D2}' -f $Repeticion
    } else {
        'rep-{0:D2}-attempt-{1:D2}' -f $Repeticion, $previousAttempt
    }
    $previousMetadataPath = Join-Path $rawRoot "$Escenario/$previousName/metadata.json"
    if (-not (Test-Path -LiteralPath $previousMetadataPath -PathType Leaf)) {
        throw "No existe metadata del intento anterior: $previousMetadataPath"
    }
    $previousMetadata = Get-Content -LiteralPath $previousMetadataPath -Raw -Encoding utf8 | ConvertFrom-Json
    $recordedAttempt = if ($null -eq $previousMetadata.attempt) { 1 } else { [int]$previousMetadata.attempt }
    if ($previousMetadata.repetition -ne $Repeticion -or
        $recordedAttempt -ne $previousAttempt -or
        $previousMetadata.execution_completed -eq $true) {
        throw 'El intento anterior no consta como intento inválido auditable.'
    }
}

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
function Invoke-PrometheusQuery([string]$Name, [string]$Query, [double]$Epoch) {
    $encoded = [uri]::EscapeDataString($Query.Trim())
    $timeValue = $Epoch.ToString('F7', [Globalization.CultureInfo]::InvariantCulture)
    Invoke-HttpCapture $Name "$($PrometheusUrl.TrimEnd('/'))/api/v1/query?query=$encoded&time=$timeValue"
}
function Invoke-PrometheusRangeQuery(
    [string]$Name,
    [string]$Query,
    [double]$StartEpoch,
    [double]$EndEpoch
) {
    $encoded = [uri]::EscapeDataString($Query.Trim())
    $startValue = $StartEpoch.ToString('F7', [Globalization.CultureInfo]::InvariantCulture)
    $endValue = $EndEpoch.ToString('F7', [Globalization.CultureInfo]::InvariantCulture)
    $uri = "$($PrometheusUrl.TrimEnd('/'))/api/v1/query_range?query=$encoded&start=$startValue&end=$endValue&step=15"
    Invoke-HttpCapture $Name $uri
}
function Get-UnixEpochSeconds([datetime]$Time) {
    $offset = [DateTimeOffset]$Time
    $whole = $offset.ToUnixTimeSeconds()
    $fraction = ($offset.UtcDateTime.Ticks % [TimeSpan]::TicksPerSecond) / [double][TimeSpan]::TicksPerSecond
    return [double]$whole + $fraction
}
function Capture-ServiceLog(
    [string]$Name,
    [string]$Service,
    [datetime]$Since,
    [datetime]$Until
) {
    $content = & docker compose -f $composePath logs --no-color `
        --since ($Since.ToString('o')) --until ($Until.ToString('o')) $Service 2>&1 | Out-String
    $code = $LASTEXITCODE
    Write-Evidence $Name ($content.TrimEnd())
    if ($code -ne 0) { throw "La captura de logs de $Service falló con código $code." }
    return $content.TrimEnd().Length
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
function Get-GitBranchName {
    $symbolicBranch = (& git -C $repositoryRoot symbolic-ref --quiet --short HEAD 2>$null | Out-String).Trim()
    if ($LASTEXITCODE -eq 0 -and $symbolicBranch) { return $symbolicBranch }

    $pointingRefs = @(& git -C $repositoryRoot for-each-ref --points-at HEAD '--format=%(refname:short)' refs/heads refs/remotes 2>$null)
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo determinar la rama Git.' }
    $localBranch = $pointingRefs | Where-Object { $_ -and -not $_.StartsWith('origin/') } | Select-Object -First 1
    if ($localBranch) { return $localBranch.Trim() }
    $remoteBranch = $pointingRefs | Where-Object { $_ -and $_ -ne 'origin/HEAD' } | Select-Object -First 1
    if ($remoteBranch) { return $remoteBranch.Trim().Replace('origin/', '') }

    $detachedSha = (& git -C $repositoryRoot rev-parse --short HEAD 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or -not $detachedSha) { throw 'No se pudo determinar la referencia Git.' }
    return "detached@$detachedSha"
}
function Get-ExperimentGitStatus {
    $scenarioEvidence = if ($Precheck) { $evidenceDirectory } else { Join-Path $rawRoot $Escenario }
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
    '--users', "$users", '--spawn-rate', "$spawnRate", '--run-time', $config.Duration,
    '--csv', $csvPrefix, '--csv-full-history',
    '--html', (Join-Path $evidenceDirectory 'locust-report.html')
)
$displayCommand = $pythonCommand + ' ' + (($locustArguments | ForEach-Object {
    if ($_ -match '\s') { '"' + $_ + '"' } else { $_ }
}) -join ' ')
$job = 'reservas-solicitudes-service'
$gatewayJob = 'api-gateway'
$range = $config.Range
$fiveXxPercent = "100 * sum(increase(http_server_requests_seconds_count{job=`"$job`",status=~`"5..`"}[$range])) / clamp_min(sum(increase(http_server_requests_seconds_count{job=`"$job`"}[$range])), 1)"
$fiveXxCount = "sum(increase(http_server_requests_seconds_count{job=`"$job`",status=~`"5..`"}[$range]))"
$p95 = "1000 * histogram_quantile(0.95, sum by (le) (increase(http_request_duration_seconds_bucket{job=`"$job`"}[$range])))"
$gatewayStatusByUri = "sum by (job, method, uri, status) (http_server_requests_seconds_count{job=`"$gatewayJob`"})"
$reservasStatusByUri = "sum by (job, method, uri, status) (http_server_requests_seconds_count{job=`"$job`"})"
$gitBranch = Get-GitBranchName
$gitSha = (& git -C $repositoryRoot rev-parse HEAD 2>&1 | Out-String).Trim()
$gitStatusBefore = Get-ExperimentGitStatus
$isCorrectiveReliability = $Escenario -eq 'fiabilidad_nominal_50u_1h_refresh'
if ($isCorrectiveReliability -and -not $DryRun -and -not [string]::IsNullOrEmpty($gitStatusBefore)) {
    throw 'La campaña correctiva exige un árbol Git limpio antes de ejecutar.'
}
$preflightLocustVersion = $null
if ($isCorrectiveReliability -and -not $DryRun) {
    foreach ($command in @('git', 'docker')) {
        if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
            throw "Falta el comando requerido: $command."
        }
    }
    & docker compose version *> $null
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose no está disponible.' }
    if (-not $env:LOCUST_USERNAME -or -not $env:LOCUST_PASSWORD) {
        throw 'LOCUST_USERNAME y LOCUST_PASSWORD son obligatorios.'
    }
    if (-not (Test-Path $composePath -PathType Leaf)) { throw "No existe $composePath." }
    $preflightLocustVersion = (& $pythonCommand -m locust --version 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) { throw "Locust no está disponible en $pythonCommand." }
    foreach ($service in @('api-gateway', 'reservas-solicitudes-service')) {
        $serviceId = (& docker compose -f $composePath ps -q $service 2>&1 | Out-String).Trim()
        if ($LASTEXITCODE -ne 0 -or -not $serviceId) {
            throw "El servicio Compose $service no está accesible."
        }
    }
    $null = Invoke-WebRequest -UseBasicParsing `
        -Uri "$($PrometheusUrl.TrimEnd('/'))/-/healthy" -TimeoutSec 30
    $null = Invoke-WebRequest -UseBasicParsing `
        -Uri "$($HostObjetivo.TrimEnd('/'))/actuator/health" -TimeoutSec 30
}

# La repetición se reserva únicamente después de superar todo el preflight.
New-Item -ItemType Directory -Force -Path $evidenceDirectory | Out-Null
Write-Evidence 'prometheus-5xx-percent.promql' $fiveXxPercent
Write-Evidence 'prometheus-5xx-count.promql' $fiveXxCount
Write-Evidence 'prometheus-p95.promql' $p95
if ($isCorrectiveReliability) {
    Write-Evidence 'prometheus-gateway-status-by-uri.promql' $gatewayStatusByUri
    Write-Evidence 'prometheus-reservas-status-by-uri.promql' $reservasStatusByUri
}
$pythonVersion = (& $pythonCommand --version 2>&1 | Out-String).Trim()
$metadataRepetition = if ($Precheck) { $null } else { $Repeticion }
$metadata = [ordered]@{
    status = if ($DryRun) { 'dry-run' } else { 'planned' }
    precheck = [bool]$Precheck; official = -not [bool]$Precheck
    scenario = $Escenario; repetition = $metadataRepetition; attempt = $Intento; host = $HostObjetivo
    prometheus_url = $PrometheusUrl; users = $users; spawn_rate = $spawnRate
    planned_duration = $config.Duration; planned_duration_seconds = $config.Seconds
    command = $displayCommand; created_at_utc = (Get-Date).ToUniversalTime().ToString('o')
    git_branch = $gitBranch; git_sha = $gitSha
    git_worktree_clean_before = [string]::IsNullOrEmpty($gitStatusBefore)
    git_status_before = $gitStatusBefore; python_version = $pythonVersion
    locust_version = $null; deployment_fingerprint_before = $null
    deployment_fingerprint_after = $null; environment_consistent = $false
    started_at_utc = $null; finished_at_utc = $null; elapsed_seconds = $null
    locust_exit_code = $null; duration_completed = $false
    reservas_log_capture_succeeded = $false; reservas_log_content_length = $null
    gateway_log_capture_succeeded = $false; gateway_log_content_length = $null
    git_sha_after = $null; business_get_count = 0; login_request_count = 0
    refresh_request_count = 0; business_population_valid = $false
    request_names_separated = $false; manifest_entries = 0; manifest_valid = $false
    secret_scan_passed = $false
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
    $metadata.locust_version = if ($preflightLocustVersion) {
        $preflightLocustVersion
    } else {
        (& $pythonCommand -m locust --version 2>&1 | Out-String).Trim()
    }
    if (-not $metadata.locust_version) { throw 'No se pudo consultar la versión de Locust.' }
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
    if ($Escenario -eq 'fiabilidad_nominal_50u_1h_refresh') {
        $env:LOCUST_REQUEST_LOG = Join-Path $evidenceDirectory 'locust_requests.csv'
    }
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
            $epoch = Get-UnixEpochSeconds $finishTime
            $startEpoch = Get-UnixEpochSeconds $startTime
            Invoke-PrometheusQuery 'prometheus-5xx-result.txt' $fiveXxCount $epoch
            Invoke-PrometheusQuery 'prometheus-5xx-percent-result.txt' $fiveXxPercent $epoch
            Invoke-PrometheusQuery 'prometheus-p95-result.txt' $p95 $epoch
            Invoke-HttpCapture 'prometheus-health-after.txt' "$($PrometheusUrl.TrimEnd('/'))/-/healthy"
            Invoke-HttpCapture 'gateway-health-after.json' "$($HostObjetivo.TrimEnd('/'))/actuator/health"
            Invoke-Captured 'reservas-health-after.json' docker @('compose','-f',$composePath,'ps','--format','json','reservas-solicitudes-service')
            Invoke-Captured 'docker-stats-after.txt' docker @('stats','--no-stream')
            Invoke-Captured 'cockroach-containers-after.txt' docker @('compose','-f',$composePath,'ps','--format','json','crdb-e3-1','crdb-e3-2','crdb-e3-3')
            $metadata.reservas_log_content_length = Capture-ServiceLog `
                'reservas-service.log' 'reservas-solicitudes-service' $startTime $finishTime
            $metadata.reservas_log_capture_succeeded = $true
            if ($Escenario -eq 'fiabilidad_nominal_50u_1h_refresh') {
                $metadata.gateway_log_content_length = Capture-ServiceLog `
                    'gateway-service.log' 'api-gateway' $startTime $finishTime
                $metadata.gateway_log_capture_succeeded = $true
                Invoke-PrometheusRangeQuery 'prometheus-gateway-status-by-uri-result.json' `
                    $gatewayStatusByUri $startEpoch $epoch
                Invoke-PrometheusRangeQuery 'prometheus-reservas-status-by-uri-result.json' `
                    $reservasStatusByUri $startEpoch $epoch
                Write-Evidence 'phase-boundaries.json' ([ordered]@{
                    started_at_utc = $startTime.ToString('o')
                    split_at_seconds = 900
                    split_at_utc = $startTime.AddSeconds(900).ToString('o')
                    finished_at_utc = $finishTime.ToString('o')
                    start_epoch = $startEpoch
                    split_epoch = $startEpoch + 900
                    finish_epoch = $epoch
                } | ConvertTo-Json)
            }
        }
        $metadata.deployment_fingerprint_after = Get-DeploymentFingerprint
        Write-Evidence 'deployment-state-after.txt' $metadata.deployment_fingerprint_after
        $branchAfter = Get-GitBranchName
        $shaAfter = (& git -C $repositoryRoot rev-parse HEAD | Out-String).Trim()
        $metadata.git_sha_after = $shaAfter
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
            -not (Test-Path $path -PathType Leaf) -or
                ($_ -ne 'reservas-service.log' -and (Get-Item $path).Length -eq 0)
        })
        $metadata.evidence_complete = ($missing.Count -eq 0 -and $metadata.reservas_log_capture_succeeded)
        if ($missing) { $metadata.launcher_error = "Evidencia ausente o vacía: $($missing -join ', ')" }
    } catch {
        $metadata.launcher_error = $_.Exception.Message; $metadata.evidence_complete = $false
    }
    if ($Escenario -eq 'fiabilidad_nominal_50u_1h_refresh' -and -not $DryRun) {
        $metadata | ConvertTo-Json -Depth 5 | Set-Content $metadataPath -Encoding utf8
        Push-Location $repositoryRoot
        try {
            & $pythonCommand -m experimentos.finalizar_evidencia_iso25010 `
                --evidence-dir $evidenceDirectory --scenario $Escenario `
                --repetition $Repeticion --attempt $Intento
            $finalizerExitCode = $LASTEXITCODE
        } finally {
            Pop-Location
        }
        $metadata = Get-Content -LiteralPath $metadataPath -Raw -Encoding utf8 | ConvertFrom-Json
        if ($finalizerExitCode -ne 0) { $metadata.execution_completed = $false }
    } else {
        $metadata.execution_completed = [bool]($metadata.duration_completed -and
            $metadata.environment_consistent -and $metadata.evidence_complete)
        $metadata.status = if ($metadata.execution_completed) { 'completed' } else { 'aborted' }
        $metadata | ConvertTo-Json -Depth 5 | Set-Content $metadataPath -Encoding utf8
    }
}
Write-Output "Código real de Locust: $locustExitCode"
Write-Output "Ejecución experimental completada: $($metadata.execution_completed)"
if (-not $metadata.execution_completed) { exit 2 }
exit 0
