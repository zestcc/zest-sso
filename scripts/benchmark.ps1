# ZestSSO 简易压测脚本（PowerShell）
# 用法: powershell -File scripts/benchmark.ps1 -BaseUrl http://localhost:9000 -Requests 500 -Concurrency 20 -OutputFile docs/benchmark-report.md

param(
    [string]$BaseUrl = "http://localhost:9000",
    [int]$Requests = 500,
    [int]$Concurrency = 20,
    [string]$OutputFile = ""
)

$endpoints = @(
    "/api/public/health",
    "/oauth2/jwks",
    "/api/public/.well-known/openid-configuration",
    "/scim/v2/ServiceProviderConfig",
    "/login"
)

$startedAt = Get-Date
Write-Host "ZestSSO Benchmark"
Write-Host "BaseUrl: $BaseUrl  Requests: $Requests  Concurrency: $Concurrency"
Write-Host ""

function Test-Endpoint {
    param([string]$Path, [int]$Count, [int]$Parallel)
    $url = "$BaseUrl$Path"
    $times = [System.Collections.Concurrent.ConcurrentBag[double]]::new()
    $errors = 0

    $batchSize = [math]::Ceiling($Count / $Parallel)
    $jobs = @()

    for ($b = 0; $b -lt $Parallel; $b++) {
        $start = $b * $batchSize
        $end = [math]::Min($start + $batchSize, $Count)
        if ($start -ge $end) { continue }

        $jobs += Start-Job -ScriptBlock {
            param($u, $from, $to)
            $localTimes = @()
            $localErrors = 0
            for ($i = $from; $i -lt $to; $i++) {
                $sw = [System.Diagnostics.Stopwatch]::StartNew()
                try {
                    $r = Invoke-WebRequest -Uri $u -UseBasicParsing -TimeoutSec 10
                    if ($r.StatusCode -ge 400) { $localErrors++ }
                } catch {
                    $localErrors++
                }
                $sw.Stop()
                $localTimes += $sw.Elapsed.TotalMilliseconds
            }
            return @{ times = $localTimes; errors = $localErrors }
        } -ArgumentList $url, $start, $end
    }

    $results = $jobs | Wait-Job | Receive-Job
    $jobs | Remove-Job

    foreach ($r in $results) {
        foreach ($t in $r.times) { $times.Add($t) }
        $errors += $r.errors
    }

    $sorted = @($times | Sort-Object)
    if ($sorted.Count -eq 0) {
        return [PSCustomObject]@{
            Endpoint = $Path
            Total    = $Count
            Errors   = $errors
            AvgMs    = 0
            P50Ms    = 0
            P99Ms    = 0
            QPS      = 0
        }
    }

    $p50 = $sorted[[int]([math]::Max(0, $sorted.Count * 0.5 - 1))]
    $p99 = $sorted[[int]([math]::Max(0, $sorted.Count * 0.99 - 1))]
    $avg = ($sorted | Measure-Object -Average).Average
    $sumMs = ($sorted | Measure-Object -Sum).Sum

    [PSCustomObject]@{
        Endpoint = $Path
        Total    = $Count
        Errors   = $errors
        AvgMs    = [math]::Round($avg, 2)
        P50Ms    = [math]::Round($p50, 2)
        P99Ms    = [math]::Round($p99, 2)
        QPS      = if ($sumMs -gt 0) { [math]::Round($Count / ($sumMs / 1000), 2) } else { 0 }
    }
}

$results = @()
foreach ($ep in $endpoints) {
    $result = Test-Endpoint -Path $ep -Count $Requests -Parallel $Concurrency
    $results += $result
    Write-Host "[$($result.Endpoint)]"
    Write-Host "  Requests: $($result.Total)  Errors: $($result.Errors)"
    Write-Host "  Avg: $($result.AvgMs)ms  P50: $($result.P50Ms)ms  P99: $($result.P99Ms)ms  ~$($result.QPS) req/s"
    Write-Host ""
}

if ($OutputFile) {
    $lines = @(
        "# ZestSSO Benchmark Report",
        "",
        "| Item | Value |",
        "|------|-------|",
        "| Started | $($startedAt.ToString('yyyy-MM-dd HH:mm:ss')) |",
        "| Base URL | $BaseUrl |",
        "| Requests per endpoint | $Requests |",
        "| Concurrency | $Concurrency |",
        "",
        "## Results",
        "",
        "| Endpoint | Total | Errors | Avg(ms) | P50(ms) | P99(ms) | QPS |",
        "|----------|-------|--------|---------|---------|---------|-----|"
    )
    foreach ($r in $results) {
        $lines += "| $($r.Endpoint) | $($r.Total) | $($r.Errors) | $($r.AvgMs) | $($r.P50Ms) | $($r.P99Ms) | $($r.QPS) |"
    }
    $lines += ""
    $lines += "## Summary"
    $lines += ""
    $failed = $results | Where-Object { $_.Errors -gt 0 }
    if ($failed.Count -gt 0) {
        $lines += "- Some endpoints returned errors. Verify the server is running and BaseUrl is correct."
    } else {
        $lines += "- All benchmark endpoints succeeded under baseline concurrency."
    }
    $lines += "- Re-run on production-like MySQL + Redis and monitor OAuth token endpoint P99."
    $parent = Split-Path -Parent $OutputFile
    if ($parent -and -not (Test-Path $parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    $content = $lines -join [Environment]::NewLine
    Set-Content -Path $OutputFile -Value $content -Encoding UTF8
    Write-Host "Report written: $OutputFile"
}

Write-Host "Done."
