# ZestSSO local E2E smoke script
# Usage: powershell -File scripts/e2e-local.ps1 -BaseUrl http://localhost:9000

param(
    [string]$BaseUrl = "http://localhost:9000",
    [int]$TimeoutSec = 15
)

$ErrorActionPreference = "Continue"
$passed = 0
$failed = 0

function Pass([string]$Name) {
    Write-Host "[PASS] $Name" -ForegroundColor Green
    $script:passed++
}

function Fail([string]$Name, [string]$Reason) {
    Write-Host "[FAIL] $Name - $Reason" -ForegroundColor Red
    $script:failed++
}

Write-Host "ZestSSO E2E Smoke @ $BaseUrl"
Write-Host ""

try {
    $r = Invoke-RestMethod "$BaseUrl/api/public/health" -TimeoutSec $TimeoutSec
    if ($r) { Pass "health" } else { Fail "health" "empty response" }
} catch { Fail "health" $_.Exception.Message }

try {
    $r = Invoke-RestMethod "$BaseUrl/api/public/.well-known/openid-configuration" -TimeoutSec $TimeoutSec
    if ($r.issuer) { Pass "oidc-discovery" } else { Fail "oidc-discovery" "missing issuer" }
} catch { Fail "oidc-discovery" $_.Exception.Message }

try {
    $r = Invoke-RestMethod "$BaseUrl/oauth2/jwks" -TimeoutSec $TimeoutSec
    if ($r.keys) { Pass "jwks" } else { Fail "jwks" "missing keys" }
} catch { Fail "jwks" $_.Exception.Message }

try {
    $r = Invoke-RestMethod "$BaseUrl/scim/v2/ServiceProviderConfig" -TimeoutSec $TimeoutSec
    if ($r.bulk.supported) { Pass "scim-config" } else { Fail "scim-config" "bulk not supported" }
} catch { Fail "scim-config" $_.Exception.Message }

try {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $body = '{"username":"admin","password":"admin123"}'
    $r = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/auth/login" -ContentType "application/json" -Body $body -WebSession $session -TimeoutSec $TimeoutSec
    if ($r.code -eq 0) { Pass "admin-login" } else { Fail "admin-login" $r.message }

    $clients = Invoke-RestMethod -Uri "$BaseUrl/api/admin/clients?page=1&size=10" -WebSession $session -TimeoutSec $TimeoutSec
    if ($clients.code -eq 0) { Pass "admin-session-clients" } else { Fail "admin-session-clients" $clients.message }
} catch { Fail "admin-login" $_.Exception.Message }

try {
    $token = Invoke-RestMethod -Method Post -Uri "$BaseUrl/oauth2/token" -ContentType "application/x-www-form-urlencoded" -Body @{
        grant_type = "client_credentials"
        client_id = "scim-provisioner"
        client_secret = "change-me-in-production"
        scope = "scim"
    } -TimeoutSec $TimeoutSec
    if ($token.access_token) {
        $users = Invoke-RestMethod -Uri "$BaseUrl/scim/v2/Users" -Headers @{ Authorization = "Bearer $($token.access_token)" } -TimeoutSec $TimeoutSec
        if ($null -ne $users.totalResults) { Pass "scim-token" } else { Fail "scim-token" "invalid list response" }
    } else { Fail "scim-token" "no access_token" }
} catch { Fail "scim-token" $_.Exception.Message }

try {
    $logout = Invoke-RestMethod "$BaseUrl/api/public/logout-url?redirect_uri=http://localhost:5173/login" -TimeoutSec $TimeoutSec
    if ($logout.code -eq 0 -and $logout.data -like "*connect/logout*") { Pass "logout-url" } else { Fail "logout-url" "unexpected response" }
} catch { Fail "logout-url" $_.Exception.Message }

try {
    $html = Invoke-WebRequest -Uri "$BaseUrl/login" -UseBasicParsing -TimeoutSec $TimeoutSec
    if ($html.StatusCode -lt 400) { Pass "login-page" } else { Fail "login-page" "status $($html.StatusCode)" }
} catch { Fail "login-page" $_.Exception.Message }

try {
    $wa = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/public/webauthn/login/options" `
        -ContentType "application/json" -Headers @{ Origin = "http://localhost:5173" } -Body "{}" -TimeoutSec $TimeoutSec
    if ($wa.code -eq 0 -and $wa.data.sessionToken) { Pass "webauthn-login-options" } else { Fail "webauthn-login-options" $wa.message }
} catch { Fail "webauthn-login-options" $_.Exception.Message }

Write-Host ""
Write-Host "Result: $passed passed, $failed failed"
if ($failed -gt 0) { exit 1 }
