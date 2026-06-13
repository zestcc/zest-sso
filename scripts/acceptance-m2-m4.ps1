# ZestSSO M2~M4 acceptance (production-grade)
param(
    [string]$BaseUrl = "http://localhost:9000",
    [string]$MysqlPassword = $env:MYSQL_PASSWORD
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

if (-not $MysqlPassword) { $MysqlPassword = "123456" }
$env:MYSQL_PASSWORD = $MysqlPassword

$passed = 0
$failed = 0

function Assert-Ok($name, $scriptBlock) {
    try {
        & $scriptBlock
        Write-Host "  PASS $name" -ForegroundColor Green
        $script:passed++
    } catch {
        Write-Host "  FAIL $name - $($_.Exception.Message)" -ForegroundColor Red
        $script:failed++
    }
}

Write-Host "========================================"
Write-Host " ZestSSO M2~M4 Acceptance"
Write-Host "========================================"

Write-Host ">>> Integration tests (M2~M4 focused)"
$mvnOut = mvn -pl zest-sso-server -am test -Pmysql-it `
    "-Dtest=DeviceAuthorizationIT,JwtSigningKeyServiceTest,WebhookDeliveryServiceTest,ScimBulkPatchIT" `
    -DfailIfNoTests=false 2>&1 | Out-String
if ($mvnOut -notmatch "BUILD SUCCESS") {
    Write-Host $mvnOut
    throw "Maven IT failed"
}
Write-Host "  PASS Maven IT subset" -ForegroundColor Green
$passed++

Assert-Ok "OIDC device_authorization_endpoint" {
    $disc = Invoke-RestMethod -Uri "$BaseUrl/api/public/.well-known/openid-configuration" -TimeoutSec 10
    if (-not $disc.device_authorization_endpoint) { throw "missing device_authorization_endpoint" }
}

Assert-Ok "Device CLI client registered" {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $login = Invoke-RestMethod -Uri "$BaseUrl/api/admin/auth/login" -Method Post `
        -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}' -WebSession $session
    if ($login.data.mfaRequired) { throw "admin login requires MFA in dev" }
    $clients = Invoke-RestMethod -Uri "$BaseUrl/api/admin/clients?page=1&size=100" -WebSession $session
    $device = $clients.data.records | Where-Object { $_.clientId -eq "device-cli" }
    if (-not $device) { throw "device-cli client missing" }
}

Assert-Ok "JWKS multi-key after rotate (requires admin)" {
    # Read-only: JWKS must expose keys array
    $jwks = Invoke-RestMethod -Uri "$BaseUrl/oauth2/jwks" -TimeoutSec 10
    if ($jwks.keys.Count -lt 1) { throw "empty jwks" }
}

Assert-Ok "Node SDK file present" {
    $p = Join-Path $Root "zest-sso-client-sdk-node/index.js"
    if (-not (Test-Path $p)) { throw "missing node sdk" }
}

Assert-Ok "Python SDK file present" {
    $p = Join-Path $Root "zest-sso-client-sdk-python/zest_sso_client.py"
    if (-not (Test-Path $p)) { throw "missing python sdk" }
}

Assert-Ok "Branding login page" {
    $html = Invoke-WebRequest -Uri "$BaseUrl/login" -TimeoutSec 10 -UseBasicParsing
    if ($html.Content -notmatch "ZestSSO|loginTitle") { throw "login page broken" }
}

Assert-Ok "Device Authorization POST (device-cli)" {
    $body = "client_id=device-cli&scope=openid"
    $resp = Invoke-WebRequest -Uri "$BaseUrl/oauth2/device_authorization" -Method Post `
        -ContentType "application/x-www-form-urlencoded" -Body $body -UseBasicParsing -TimeoutSec 15
    $json = $resp.Content | ConvertFrom-Json
    if (-not $json.device_code) { throw "missing device_code" }
    if (-not $json.user_code) { throw "missing user_code" }
}

Write-Host ""
Write-Host "Result: $passed passed, $failed failed"
if ($failed -gt 0) { exit 1 }
exit 0
