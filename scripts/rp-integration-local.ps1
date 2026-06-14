# ZestSSO × ZestFlow × ZestLLM 本地 RP 联调
# 端口规划（避开 8088 占用）:
#   ZestSSO        :9000
#   ZestFlow Admin :8089   UI :5177
#   ZestLLM Admin  :8090   UI :5176
#
# 用法:
#   powershell -File scripts/rp-integration-local.ps1 -Action start
#   powershell -File scripts/rp-integration-local.ps1 -Action test
#   powershell -File scripts/rp-integration-local.ps1 -Action stop

param(
    [ValidateSet('start', 'test', 'stop', 'status')]
    [string]$Action = 'status',
    [string]$SsoUrl = 'http://localhost:9000',
    [string]$ZestFlowUrl = 'http://localhost:8089',
    [string]$ZestLlmUrl = 'http://localhost:8090',
    [string]$MysqlPassword = $env:MYSQL_PASSWORD
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
$ZestFlowRoot = 'D:\project\zestflow'
$ZestLlmRoot = 'D:\project\zest\zest-llm'
$PidFile = Join-Path $Root '.local\rp-integration.pids.json'

if (-not $MysqlPassword) { $MysqlPassword = '123456' }
$env:MYSQL_PASSWORD = $MysqlPassword

function Test-HttpUp([string]$Url) {
    try {
        $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 4
        return $r.StatusCode -ge 200 -and $r.StatusCode -lt 500
    } catch { return $false }
}

function Save-Pids([hashtable]$map) {
    New-Item -ItemType Directory -Force -Path (Split-Path $PidFile) | Out-Null
    [ordered]@{
        sso = $map.sso
        zestflow = $map.zestflow
        zestllm = $map.zestllm
    } | ConvertTo-Json | Set-Content -Path $PidFile -Encoding UTF8
}

function Stop-Pids() {
    if (-not (Test-Path $PidFile)) { return }
    $p = Get-Content $PidFile -Raw | ConvertFrom-Json
    foreach ($name in @('sso', 'zestflow', 'zestllm')) {
        $procId = $p.$name
        if ($procId) {
            Stop-Process -Id ([int]$procId) -Force -ErrorAction SilentlyContinue
            Write-Host "Stopped $name (PID $procId)"
        }
    }
    Remove-Item $PidFile -ErrorAction SilentlyContinue
}

function Start-Idp() {
    $jar = Get-ChildItem "$Root\zest-sso-server\target\zest-sso-server-*.jar" |
        Where-Object { $_.Name -notmatch 'original' } |
        Sort-Object { [version]($_.BaseName -replace 'zest-sso-server-','') } -Descending |
        Select-Object -First 1
    if (-not $jar) {
        Write-Host '[build] zest-sso...'
        Set-Location $Root
        mvn -q -pl zest-sso-server -am package -DskipTests
        $jar = Get-ChildItem "$Root\zest-sso-server\target\zest-sso-server-*.jar" |
            Where-Object { $_.Name -notmatch 'original' } |
            Sort-Object { [version]($_.BaseName -replace 'zest-sso-server-','') } -Descending |
            Select-Object -First 1
    }
    $env:ZEST_SSO_ISSUER = $SsoUrl
    $p = Start-Process java -ArgumentList @('-jar', $jar.FullName, '--spring.profiles.active=dev') -WorkingDirectory $Root -PassThru -WindowStyle Hidden
    return $p.Id
}

function Start-ZestFlowAdmin() {
    $jar = Get-ChildItem "$ZestFlowRoot\zestflow-admin\target\zestflow-admin-*.jar" |
        Where-Object { $_.Name -notmatch 'original' } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $jar) {
        Write-Host '[build] zestflow-admin...'
        Set-Location $ZestFlowRoot
        mvn -q -pl zestflow-admin -am package -DskipTests
        $jar = Get-ChildItem "$ZestFlowRoot\zestflow-admin\target\zestflow-admin-*.jar" |
            Where-Object { $_.Name -notmatch 'original' } |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
    }
    $p = Start-Process java -ArgumentList @('-jar', $jar.FullName, '--spring.profiles.active=local') `
        -WorkingDirectory $ZestFlowRoot -PassThru -WindowStyle Hidden
    return $p.Id
}

function Start-ZestLlmAdmin() {
    $jar = Get-ChildItem "$ZestLlmRoot\zest-llm-admin\target\zest-llm-admin-*.jar" |
        Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
    if (-not $jar) {
        Write-Host '[build] zest-llm-admin...'
        Set-Location $ZestLlmRoot
        mvn -q -pl zest-llm-admin -am package -DskipTests
        $jar = Get-ChildItem "$ZestLlmRoot\zest-llm-admin\target\zest-llm-admin-*.jar" |
            Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
    }
    $p = Start-Process java -ArgumentList @('-jar', $jar.FullName, '--spring.profiles.active=local') `
        -WorkingDirectory $ZestLlmRoot -PassThru -WindowStyle Hidden
    return $p.Id
}

function Wait-Up([string]$Name, [string]$Url, [int]$Seconds = 180) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        if (Test-HttpUp $Url) { Write-Host "  UP  $Name $Url" -ForegroundColor Green; return $true }
        Start-Sleep 3
    } while ((Get-Date) -lt $deadline)
    Write-Host "  DOWN $Name $Url" -ForegroundColor Red
    return $false
}

switch ($Action) {
    'status' {
        Write-Host "ZestSSO:    $(if (Test-HttpUp "$SsoUrl/api/public/health") { 'UP' } else { 'DOWN' }) $SsoUrl"
        Write-Host "ZestFlow:   $(if (Test-HttpUp "$ZestFlowUrl/api/zestflow/auth/sso/config") { 'UP' } else { 'DOWN' }) $ZestFlowUrl"
        Write-Host "ZestLLM:    $(if (Test-HttpUp "$ZestLlmUrl/swagger-ui.html") { 'UP' } else { 'DOWN' }) $ZestLlmUrl"
    }
    'stop' { Stop-Pids }
    'start' {
        Stop-Pids
        Write-Host '>>> Installing zest-sso-client-sdk (local)...'
        Set-Location $Root
        mvn -q install -pl zest-sso-client-sdk -DskipTests
        Write-Host '>>> Starting ZestSSO...'
        $ssoPid = Start-Idp
        Write-Host '>>> Starting ZestFlow Admin (:8089)...'
        $zfPid = Start-ZestFlowAdmin
        Write-Host '>>> Starting ZestLLM Admin (:8090)...'
        $llmPid = Start-ZestLlmAdmin
        Save-Pids @{ sso = $ssoPid; zestflow = $zfPid; zestllm = $llmPid }
        $ok = $true
        $ok = (Wait-Up 'ZestSSO' "$SsoUrl/api/public/health") -and $ok
        $ok = (Wait-Up 'ZestFlow' "$ZestFlowUrl/api/zestflow/auth/sso/config") -and $ok
        $ok = (Wait-Up 'ZestLLM' "$ZestLlmUrl/swagger-ui.html") -and $ok
        if (-not $ok) { Write-Host '部分服务未就绪，请查看日志后重试' -ForegroundColor Yellow; exit 1 }
        Write-Host ''
        Write-Host '联调地址:'
        Write-Host "  ZestSSO     $SsoUrl"
        Write-Host "  ZestFlow    $ZestFlowUrl  (UI http://localhost:5177)"
        Write-Host "  ZestLLM     $ZestLlmUrl  (UI http://localhost:5176)"
    }
    'test' {
        $fail = 0
        Write-Host '>>> ZestSSO backchannel (builtin RP)'
        powershell -File (Join-Path $Root 'scripts\sso-backchannel-e2e.ps1') -BaseUrl $SsoUrl
        if ($LASTEXITCODE -ne 0) { $fail++ }
        if (Test-Path (Join-Path $ZestFlowRoot 'scripts\sso-backchannel-e2e.ps1')) {
            Write-Host '>>> ZestFlow backchannel'
            powershell -File (Join-Path $ZestFlowRoot 'scripts\sso-backchannel-e2e.ps1') -SsoUrl $SsoUrl -ZfUrl $ZestFlowUrl
            if ($LASTEXITCODE -ne 0) { $fail++ }
        }
        $llmE2e = Join-Path $ZestLlmRoot 'deploy\scripts\sso-backchannel-e2e.ps1'
        if (Test-Path $llmE2e) {
            Write-Host '>>> ZestLLM backchannel'
            powershell -File $llmE2e -SsoUrl $SsoUrl -LlmUrl $ZestLlmUrl
            if ($LASTEXITCODE -ne 0) { $fail++ }
        }
        if ($fail -gt 0) { exit 1 }
        Write-Host 'RP integration tests PASS' -ForegroundColor Green
    }
}
