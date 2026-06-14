# ZestSSO Back-Channel Logout 联调（内置测试 RP）
# 用法: powershell -File scripts/sso-backchannel-e2e.ps1 [-BaseUrl http://localhost:9000]
# 前置: IdP 以 dev/test profile 运行且 zest.sso.test.backchannel-receiver-enabled=true

param(
    [string]$BaseUrl = "http://localhost:9000",
    [string]$TestClientId = "backchannel-test-rp"
)

$ErrorActionPreference = "Stop"
$passed = 0
$failed = 0

function Pass($name) { Write-Host "  PASS $name" -ForegroundColor Green; $script:passed++ }
function Fail($name, $detail) { Write-Host "  FAIL $name - $detail" -ForegroundColor Red; $script:failed++ }

Write-Host "ZestSSO Back-Channel E2E @ $BaseUrl"
Write-Host ""

try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/api/public/health" -TimeoutSec 5
    if ($health.data -eq "UP") { Pass "health" } else { Fail "health" "not UP" }
} catch { Fail "health" $_.Exception.Message }

try {
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/public/test/backchannel/clear" -TimeoutSec 5 | Out-Null
    Pass "test-receiver-enabled"
} catch {
    Fail "test-receiver-enabled" "test RP disabled; use dev profile with zest.sso.test.backchannel-receiver-enabled=true"
}

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
try {
    $login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/auth/login" `
        -WebSession $session -ContentType "application/json" `
        -Body '{"username":"admin","password":"admin123"}'
    if ($login.code -eq 0) { Pass "admin-login" } else { Fail "admin-login" $login.message }
} catch { Fail "admin-login" $_.Exception.Message }

try {
    $seed = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/public/test/backchannel/seed" `
        -Body @{ clientId = $TestClientId }
    if ($seed.code -eq 0) { Pass "seed-authorization" } else { Fail "seed-authorization" $seed.message }
} catch { Fail "seed-authorization" $_.Exception.Message }

try {
    $logout = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/auth/logout" -WebSession $session
    if ($logout.code -eq 0) { Pass "admin-logout" } else { Fail "admin-logout" $logout.message }
} catch { Fail "admin-logout" $_.Exception.Message }

Start-Sleep -Seconds 3

try {
    $last = Invoke-RestMethod -Uri "$BaseUrl/api/public/test/rp/backchannel-logout/last" -TimeoutSec 5
    if ($last.code -eq 0 -and $last.data.received -eq $true -and $last.data.principalName -eq "admin") {
        Pass "rp-received-logout-token"
    } else {
        Fail "rp-received-logout-token" "logout_token not received"
    }
} catch { Fail "rp-received-logout-token" $_.Exception.Message }

try {
    $session2 = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $null = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/auth/login" `
        -WebSession $session2 -ContentType "application/json" `
        -Body '{"username":"admin","password":"admin123"}'
    $deliveries = Invoke-RestMethod -Uri "$BaseUrl/api/admin/logout-deliveries?principalName=admin&page=1&size=5" `
        -WebSession $session2 -TimeoutSec 5
    $ok = $deliveries.data.records | Where-Object { $_.clientId -eq $TestClientId -and $_.status -eq "SUCCESS" } | Select-Object -First 1
    if ($ok) { Pass "delivery-record-success" } else { Fail "delivery-record-success" "no SUCCESS delivery record" }
} catch { Fail "delivery-record-success" $_.Exception.Message }

Write-Host ""
Write-Host "Back-Channel E2E: $passed passed, $failed failed"
if ($failed -gt 0) { exit 1 }
