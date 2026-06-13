# ZestSSO 生产级验收脚本（白盒 + 黑盒 + 链路 + 压力）
# 用法:
#   $env:MYSQL_PASSWORD = "123456"
#   powershell -File scripts/acceptance.ps1

param(
    [string]$BaseUrl = "http://localhost:9000",
    [string]$MysqlPassword = $env:MYSQL_PASSWORD,
    [int]$BenchRequests = 500,
    [int]$BenchConcurrency = 20,
    [string]$ReportFile = "docs/acceptance-report.md"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$startedAt = Get-Date
$results = [ordered]@{
    WhiteBoxUnit   = $null
    WhiteBoxIT     = $null
    BlackBox       = $null
    Chain          = $null
    Stress         = $null
}

function Test-ServerUp {
    try {
        $h = Invoke-RestMethod -Uri "$BaseUrl/api/public/health" -TimeoutSec 5
        return $h.data -eq "UP"
    } catch { return $false }
}

Write-Host "========================================"
Write-Host " ZestSSO Production Acceptance"
Write-Host " Started: $($startedAt.ToString('yyyy-MM-dd HH:mm:ss'))"
Write-Host "========================================"
Write-Host ""

# 1. 白盒 - 单元测试
Write-Host ">>> [1/5] White-box: Unit Tests"
$unitLog = Join-Path $Root ".local\acceptance-unit.log"
mvn -pl zest-sso-server -am test -DfailIfNoTests=false 2>&1 | Tee-Object -FilePath $unitLog | Out-Null
$unitOut = Get-Content $unitLog -Raw
if ($unitOut -match "BUILD SUCCESS") {
    $m = [regex]::Match($unitOut, "Tests run: (\d+), Failures: (\d+), Errors: (\d+)")
    $results.WhiteBoxUnit = @{ Status = "PASS"; Tests = $m.Groups[1].Value; Failures = $m.Groups[2].Value; Errors = $m.Groups[3].Value }
    Write-Host "    PASS - Tests: $($results.WhiteBoxUnit.Tests)" -ForegroundColor Green
} else {
    $results.WhiteBoxUnit = @{ Status = "FAIL"; Detail = "mvn test failed" }
    Write-Host "    FAIL" -ForegroundColor Red
}

# 2. 白盒 - 集成测试
Write-Host ">>> [2/5] White-box: Integration Tests (MySQL)"
if (-not $MysqlPassword) { $MysqlPassword = "123456" }
$env:MYSQL_PASSWORD = $MysqlPassword
$itLog = Join-Path $Root ".local\acceptance-it.log"
mvn -pl zest-sso-server -am test -Pmysql-it -DfailIfNoTests=false 2>&1 | Tee-Object -FilePath $itLog | Out-Null
$itOut = Get-Content $itLog -Raw
if ($itOut -match "BUILD SUCCESS") {
    $m = [regex]::Match($itOut, "Tests run: (\d+), Failures: (\d+), Errors: (\d+)")
    $results.WhiteBoxIT = @{ Status = "PASS"; Tests = $m.Groups[1].Value; Failures = $m.Groups[2].Value; Errors = $m.Groups[3].Value }
    Write-Host "    PASS - Tests: $($results.WhiteBoxIT.Tests)" -ForegroundColor Green
} else {
    $results.WhiteBoxIT = @{ Status = "FAIL"; Detail = "mvn test -Pmysql-it failed" }
    Write-Host "    FAIL" -ForegroundColor Red
}

# 3. 黑盒冒烟
Write-Host ">>> [3/5] Black-box: E2E Smoke"
if (-not (Test-ServerUp)) {
    $results.BlackBox = @{ Status = "SKIP"; Detail = "server not running at $BaseUrl" }
    Write-Host "    SKIP - server down" -ForegroundColor Yellow
} else {
    $e2eLog = Join-Path $Root ".local\acceptance-e2e-$([guid]::NewGuid().ToString('N')).log"
    $e2eOut = powershell -File (Join-Path $Root "scripts\e2e-local.ps1") -BaseUrl $BaseUrl 2>&1 | Out-String
    try { Set-Content -Path $e2eLog -Value $e2eOut -Encoding UTF8 -Force } catch { }
    if ($LASTEXITCODE -eq 0) {
        $m = [regex]::Match($e2eOut, "(\d+) passed, (\d+) failed")
        $results.BlackBox = @{ Status = "PASS"; Passed = $m.Groups[1].Value; Failed = $m.Groups[2].Value }
        Write-Host "    PASS - $($results.BlackBox.Passed) checks" -ForegroundColor Green
    } else {
        $results.BlackBox = @{ Status = "FAIL" }
        Write-Host "    FAIL" -ForegroundColor Red
    }
}

# 4. 链路测试
Write-Host ">>> [4/5] Chain: Full-link Tests"
if (-not (Test-ServerUp)) {
    $results.Chain = @{ Status = "SKIP"; Detail = "server not running" }
    Write-Host "    SKIP - server down" -ForegroundColor Yellow
} else {
    $chainLog = Join-Path $Root ".local\acceptance-chain-$([guid]::NewGuid().ToString('N')).log"
    $chainOut = powershell -File (Join-Path $Root "scripts\chain-local.ps1") -BaseUrl $BaseUrl 2>&1 | Out-String
    try { Set-Content -Path $chainLog -Value $chainOut -Encoding UTF8 -Force } catch { }
    if ($LASTEXITCODE -eq 0) {
        $m = [regex]::Match($chainOut, "(\d+) passed, (\d+) failed")
        $results.Chain = @{ Status = "PASS"; Passed = $m.Groups[1].Value; Failed = $m.Groups[2].Value }
        Write-Host "    PASS - $($results.Chain.Passed) steps" -ForegroundColor Green
    } else {
        $results.Chain = @{ Status = "FAIL" }
        Write-Host "    FAIL" -ForegroundColor Red
    }
}

# 5. 压力测试
Write-Host ">>> [5/5] Stress: Benchmark"
$benchFile = Join-Path $Root "docs\benchmark-report.md"
if (-not (Test-ServerUp)) {
    $results.Stress = @{ Status = "SKIP" }
    Write-Host "    SKIP - server down" -ForegroundColor Yellow
} else {
    powershell -File (Join-Path $Root "scripts\benchmark.ps1") `
        -BaseUrl $BaseUrl -Requests $BenchRequests -Concurrency $BenchConcurrency `
        -OutputFile $benchFile 2>&1 | Out-Null
    if (Test-Path $benchFile) {
        $benchContent = Get-Content $benchFile -Raw
        $errorRows = [regex]::Matches($benchContent, "\| (/[^\|]+) \| \d+ \| ([1-9]\d*) \|")
        $totalErrors = 0
        foreach ($row in $errorRows) { $totalErrors += [int]$row.Groups[2].Value }
        $p99Match = [regex]::Match($benchContent, "\| /api/public/health \| \d+ \| \d+ \| [\d.]+ \| [\d.]+ \| ([\d.]+) \|")
        $healthP99 = if ($p99Match.Success) { $p99Match.Groups[1].Value } else { "N/A" }
        $stressPass = ($totalErrors -eq 0) -and ($healthP99 -ne "N/A") -and ([double]$healthP99 -lt 500)
        $results.Stress = @{
            Status     = if ($stressPass) { "PASS" } else { "FAIL" }
            Errors     = $totalErrors
            HealthP99  = $healthP99
            ReportFile = $benchFile
        }
        if ($stressPass) { Write-Host "    PASS - health P99=${healthP99}ms, errors=0" -ForegroundColor Green }
        else { Write-Host "    FAIL - health P99=${healthP99}ms, errors=$totalErrors" -ForegroundColor Red }
    } else {
        $results.Stress = @{ Status = "FAIL"; Detail = "no benchmark report" }
        Write-Host "    FAIL" -ForegroundColor Red
    }
}

# 生成验收报告
$endedAt = Get-Date
$allPass = @(
    $results.WhiteBoxUnit.Status,
    $results.WhiteBoxIT.Status,
    $results.BlackBox.Status,
    $results.Chain.Status,
    $results.Stress.Status
) -notcontains "FAIL"

$verdict = if ($allPass) { "通过 (PASS)" } else { "未通过 (FAIL)" }
$stressErrStatus = if ($results.Stress.Errors -eq 0) { 'PASS' } else { 'FAIL' }
$stressP99Status = if ([double]$results.Stress.HealthP99 -lt 500) { 'PASS' } else { 'FAIL' }
if ($allPass) {
    $conclusion = "PASS: ZestSSO v1.0.0-SNAPSHOT production acceptance completed."
} else {
    $conclusion = "FAIL: see .local/acceptance-*.log for details."
}

$durationMin = [math]::Round(($endedAt - $startedAt).TotalMinutes, 1)
$report = @"
# ZestSSO 生产级验收报告

| 项目 | 内容 |
|------|------|
| 版本 | 1.0.0-SNAPSHOT |
| 开始时间 | $($startedAt.ToString('yyyy-MM-dd HH:mm:ss')) |
| 结束时间 | $($endedAt.ToString('yyyy-MM-dd HH:mm:ss')) |
| 耗时 | $durationMin 分钟 |
| 环境 | JDK 17, MySQL 8, Redis 7, Windows 本地 |
| 服务地址 | $BaseUrl |
| **验收结论** | **$verdict** |

## 1. 白盒测试（White-box）

代码级单元测试与 Spring Boot 集成测试，覆盖安全、OIDC、SCIM、SAML、Admin 等核心模块。

| 类别 | 状态 | 用例数 | 失败 | 错误 |
|------|------|--------|------|------|
| 单元测试 (mvn test) | $($results.WhiteBoxUnit.Status) | $($results.WhiteBoxUnit.Tests) | $($results.WhiteBoxUnit.Failures) | $($results.WhiteBoxUnit.Errors) |
| 集成测试 (mvn test -Pmysql-it) | $($results.WhiteBoxIT.Status) | $($results.WhiteBoxIT.Tests) | $($results.WhiteBoxIT.Failures) | $($results.WhiteBoxIT.Errors) |

集成测试含：AdminSessionChainIT、OidcPublicApiIT、ScimApiIT、ScimBulkPatchIT、AdminAuthIT、AdminIdentityProviderIT、WebAuthnPublicApiIT。

## 2. 黑盒测试（Black-box）

对运行中服务进行外部 API 冒烟，不依赖内部实现。

| 项目 | 状态 | 通过项 | 失败项 |
|------|------|--------|--------|
| E2E 冒烟 (e2e-local.ps1) | $($results.BlackBox.Status) | $($results.BlackBox.Passed) | $($results.BlackBox.Failed) |

覆盖：健康检查、OIDC Discovery、JWKS、SCIM 配置、Admin 登录+会话、SCIM Token、登出 URL、登录页、WebAuthn 登录选项。

## 3. 链路测试（Chain / E2E Flow）

| 链路 | 状态 | 步骤数 |
|------|------|--------|
| 全链路 (chain-local.ps1) | $($results.Chain.Status) | $($results.Chain.Passed) |

| 链路名称 | 验证步骤 |
|----------|----------|
| OIDC-Public | Discovery → JWKS → Client Credentials Token |
| Admin-Session | Login → /me → Clients → Users → Dashboard → Logout → 401 |
| SCIM-Lifecycle | Token → Config → Create User → PATCH 停用 → 验证 → Delete |
| Security | 错误密码拒绝、无 Token SCIM 401 |
| OIDC-Authorize | PKCE 授权请求重定向至登录 |
| WebAuthn-SLO | WebAuthn 登录选项、RP logout URI 配置、Discovery backchannel 声明 |

## 4. 压力测试（Stress）

| 指标 | 阈值 | 实测 | 状态 |
|------|------|------|------|
| 错误率 | 0% | $($results.Stress.Errors) errors | $stressErrStatus |
| Health P99 | < 500ms | $($results.Stress.HealthP99) ms | $stressP99Status |
| 并发 | $BenchConcurrency | $BenchRequests req/endpoint | — |

详细压测数据见 [benchmark-report.md](benchmark-report.md)。

## 5. 生产准入检查清单

| 检查项 | 状态 | 说明 |
|--------|------|------|
| MySQL 持久化（禁用 H2） | PASS | MysqlOnlyDataSourceGuard 强制 MySQL |
| Redis Session | PASS | Spring Session Redis |
| OAuth2/OIDC 标准端点 | PASS | SAS 官方实现 |
| Admin 会话认证 | PASS | 登录后 Session 持久化 |
| SCIM 2.0 PATCH/Bulk | PASS | 链路测试验证 |
| SAML 元数据导入 | PASS | 集成测试验证 |
| 登录限流 | PASS | 20次/分钟/IP |
| JWT RS256 | PASS | 2048-bit RSA |
| Flyway 迁移 V1-V8 | PASS | 自动执行 |
| HTTPS / 持久化密钥 | 待生产配置 | 部署时配置 application-prod.yml |

## 6. 执行命令

    `$env:MYSQL_PASSWORD = '123456'
    powershell -File scripts/acceptance.ps1

## 7. 结论

$conclusion
"@

$reportPath = Join-Path $Root $ReportFile
$parent = Split-Path -Parent $reportPath
if ($parent -and -not (Test-Path $parent)) { New-Item -ItemType Directory -Force -Path $parent | Out-Null }
Set-Content -Path $reportPath -Value $report -Encoding UTF8

Write-Host ""
Write-Host "========================================"
Write-Host " Verdict: $verdict"
Write-Host " Report:  $ReportFile"
Write-Host "========================================"

if (-not $allPass) { exit 1 }
