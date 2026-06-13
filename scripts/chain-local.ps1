# ZestSSO 链路测试（黑盒）：OIDC / Admin / SCIM 全链路
# 用法: powershell -File scripts/chain-local.ps1 -BaseUrl http://localhost:9000

param(
    [string]$BaseUrl = "http://localhost:9000"
)

$ErrorActionPreference = "Continue"
$passed = 0
$failed = 0
$chains = @()

function Pass([string]$Chain, [string]$Step) {
    Write-Host "[PASS] [$Chain] $Step" -ForegroundColor Green
    $script:passed++
}

function Fail([string]$Chain, [string]$Step, [string]$Reason) {
    Write-Host "[FAIL] [$Chain] $Step - $Reason" -ForegroundColor Red
    $script:failed++
}

function New-Session {
    return New-Object Microsoft.PowerShell.Commands.WebRequestSession
}

Write-Host "ZestSSO Chain Tests @ $BaseUrl"
Write-Host ""

# --- Chain 1: OIDC 公开链路 Discovery → JWKS → Client Credentials ---
$chain = "OIDC-Public"
try {
    $disc = Invoke-RestMethod "$BaseUrl/api/public/.well-known/openid-configuration"
    if ($disc.issuer -and $disc.token_endpoint -and $disc.jwks_uri) {
        Pass $chain "discovery"
    } else { Fail $chain "discovery" "missing fields" }

    $jwks = Invoke-RestMethod $disc.jwks_uri
    if ($jwks.keys.Count -gt 0) { Pass $chain "jwks" } else { Fail $chain "jwks" "no keys" }

    $token = Invoke-RestMethod -Method Post -Uri $disc.token_endpoint `
        -ContentType "application/x-www-form-urlencoded" -Body @{
            grant_type    = "client_credentials"
            client_id     = "scim-provisioner"
            client_secret = "change-me-in-production"
            scope         = "scim"
        }
    if ($token.access_token -and $token.token_type -eq "Bearer") {
        Pass $chain "client-credentials-token"
    } else { Fail $chain "client-credentials-token" "invalid token response" }
} catch { Fail $chain "chain-error" $_.Exception.Message }

# --- Chain 2: Admin 会话链路 Login → Me → Clients → Users ---
$chain = "Admin-Session"
$admin = New-Session
try {
    $login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/auth/login" `
        -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}' `
        -WebSession $admin
    if ($login.code -ne 0) { Fail $chain "login" $login.message; throw "login failed" }
    Pass $chain "login"

    $me = Invoke-RestMethod -Uri "$BaseUrl/api/admin/auth/me" -WebSession $admin
    if ($me.data.username -eq "admin") { Pass $chain "me" } else { Fail $chain "me" "unexpected user" }

    $clients = Invoke-RestMethod -Uri "$BaseUrl/api/admin/clients?page=1&size=10" -WebSession $admin
    if ($clients.code -eq 0 -and $null -ne $clients.data.records) { Pass $chain "list-clients" } else { Fail $chain "list-clients" "bad response" }

    $users = Invoke-RestMethod -Uri "$BaseUrl/api/admin/users?page=1&size=10" -WebSession $admin
    if ($users.code -eq 0) { Pass $chain "list-users" } else { Fail $chain "list-users" $users.message }

    $stats = Invoke-RestMethod -Uri "$BaseUrl/api/admin/dashboard/stats" -WebSession $admin
    if ($stats.code -eq 0) { Pass $chain "dashboard-stats" } else { Fail $chain "dashboard-stats" $stats.message }

    $logout = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/auth/logout" -WebSession $admin
    if ($logout.code -eq 0) { Pass $chain "logout" } else { Fail $chain "logout" $logout.message }

    try {
        Invoke-RestMethod -Uri "$BaseUrl/api/admin/clients?page=1&size=10" -WebSession $admin
        Fail $chain "post-logout-deny" "still accessible after logout"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 401) { Pass $chain "post-logout-deny" }
        else { Fail $chain "post-logout-deny" $_.Exception.Message }
    }
} catch {
    if ($_.Exception.Message -ne "login failed") { Fail $chain "chain-error" $_.Exception.Message }
}

# --- Chain 3: SCIM 生命周期 Token → List → Create → PATCH → Delete ---
$chain = "SCIM-Lifecycle"
try {
    $token = Invoke-RestMethod -Method Post -Uri "$BaseUrl/oauth2/token" `
        -ContentType "application/x-www-form-urlencoded" -Body @{
            grant_type    = "client_credentials"
            client_id     = "scim-provisioner"
            client_secret = "change-me-in-production"
            scope         = "scim"
        }
    $hdr = @{ Authorization = "Bearer $($token.access_token)" }
    Pass $chain "token"

    $cfg = Invoke-RestMethod -Uri "$BaseUrl/scim/v2/ServiceProviderConfig" -Headers $hdr
    if ($cfg.patch.supported -and $cfg.bulk.supported) { Pass $chain "service-provider-config" } else { Fail $chain "service-provider-config" "missing capabilities" }

    $extId = "acceptance-" + [guid]::NewGuid().ToString("N").Substring(0, 12)
    $username = "scim_$extId"
    $createBody = @{
        schemas  = @("urn:ietf:params:scim:schemas:core:2.0:User")
        userName = $username
        name     = @{ formatted = "Acceptance User" }
        emails   = @(@{ value = "$username@test.local"; primary = $true })
        active   = $true
    } | ConvertTo-Json -Depth 5

    $created = Invoke-RestMethod -Method Post -Uri "$BaseUrl/scim/v2/Users" `
        -Headers $hdr -ContentType "application/scim+json" -Body $createBody
    if ($created.id) { Pass $chain "create-user" } else { Fail $chain "create-user" "no id" }

    $patchBody = @{
        schemas    = @("urn:ietf:params:scim:api:messages:2.0:PatchOp")
        Operations = @(@{ op = "replace"; path = "active"; value = $false })
    } | ConvertTo-Json -Depth 5
    Invoke-WebRequest -Method PATCH -Uri "$BaseUrl/scim/v2/Users/$($created.id)" `
        -Headers $hdr -ContentType "application/scim+json" -Body $patchBody -UseBasicParsing | Out-Null
    Pass $chain "patch-deactivate"

    $fetched = Invoke-RestMethod -Uri "$BaseUrl/scim/v2/Users/$($created.id)" -Headers $hdr
    if ($fetched.active -eq $false) { Pass $chain "verify-deactivated" } else { Fail $chain "verify-deactivated" "still active" }

    Invoke-RestMethod -Method Delete -Uri "$BaseUrl/scim/v2/Users/$($created.id)" -Headers $hdr | Out-Null
    Pass $chain "delete-user"
} catch { Fail $chain "chain-error" $_.Exception.Message }

# --- Chain 4: 安全链路 错误密码 / 无 Token ---
$chain = "Security"
try {
    try {
        Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/auth/login" `
            -ContentType "application/json" -Body '{"username":"admin","password":"wrong"}'
        Fail $chain "bad-password-deny" "should reject"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -in 401, 403) { Pass $chain "bad-password-deny" }
        else { Fail $chain "bad-password-deny" $_.Exception.Message }
    }

    try {
        Invoke-RestMethod -Uri "$BaseUrl/scim/v2/Users"
        Fail $chain "scim-no-token-deny" "should reject"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 401) { Pass $chain "scim-no-token-deny" }
        else { Fail $chain "scim-no-token-deny" $_.Exception.Message }
    }
} catch { Fail $chain "chain-error" $_.Exception.Message }

# --- Chain 5: OIDC Authorize 重定向链路 ---
$chain = "OIDC-Authorize"
try {
    $verifier = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 43 | ForEach-Object { [char]$_ })
    $sha = [System.Security.Cryptography.SHA256]::Create()
    $hash = $sha.ComputeHash([Text.Encoding]::ASCII.GetBytes($verifier))
    $challenge = [Convert]::ToBase64String($hash).TrimEnd('=').Replace('+', '-').Replace('/', '_')
    $authUrl = "$BaseUrl/oauth2/authorize?response_type=code&client_id=zestflow-admin" +
        "&redirect_uri=http://localhost:5173/login/callback&scope=openid%20profile" +
        "&code_challenge=$challenge&code_challenge_method=S256&state=acceptance-test"
    $status = curl.exe -s -o NUL -w "%{http_code}" --max-time 10 $authUrl
    if ($status -in @("302", "303", "307", "200")) { Pass $chain "redirect-to-login" }
    else { Fail $chain "redirect-to-login" "status $status" }
} catch { Fail $chain "redirect-to-login" $_.Exception.Message }

# --- Chain 6: WebAuthn + SLO 客户端配置 ---
$chain = "WebAuthn-SLO"
$admin2 = New-Session
try {
    $null = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/auth/login" `
        -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}' `
        -WebSession $admin2

    $wa = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/public/webauthn/login/options" `
        -ContentType "application/json" -Headers @{ Origin = "http://localhost:5173" } -Body "{}"
    if ($wa.code -eq 0 -and $wa.data.sessionToken) { Pass $chain "webauthn-login-options" }
    else { Fail $chain "webauthn-login-options" $wa.message }

    $clients = Invoke-RestMethod -Uri "$BaseUrl/api/admin/clients?page=1&size=20" -WebSession $admin2
    $llm = $clients.data.records | Where-Object { $_.clientId -eq "zest-llm-admin" } | Select-Object -First 1
    if ($llm.backchannelLogoutUri -and $llm.frontchannelLogoutUri) { Pass $chain "rp-logout-uris-configured" }
    else { Fail $chain "rp-logout-uris-configured" "zest-llm-admin missing logout URIs" }

    $disc = Invoke-RestMethod "$BaseUrl/api/public/.well-known/openid-configuration"
    if ($disc.backchannel_logout_supported -eq $true) { Pass $chain "discovery-backchannel" }
    else { Fail $chain "discovery-backchannel" "not declared" }
} catch { Fail $chain "chain-error" $_.Exception.Message }

Write-Host ""
Write-Host "Chain Result: $passed passed, $failed failed"
if ($failed -gt 0) { exit 1 }
