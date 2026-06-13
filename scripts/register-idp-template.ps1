# ZestSSO 从 JSON 模板注册 OIDC 身份源
param(
    [string]$BaseUrl = "http://localhost:9000",
    [Parameter(Mandatory = $true)]
    [ValidateSet("feishu", "dingtalk")]
    [string]$Template,
    [Parameter(Mandatory = $true)]
    [string]$ClientId,
    [Parameter(Mandatory = $true)]
    [string]$ClientSecret,
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin123"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$templateFile = switch ($Template) {
    "feishu" { Join-Path $Root "docs/templates/idp/feishu-oidc.json" }
    "dingtalk" { Join-Path $Root "docs/templates/idp/dingtalk-oidc.json.example" }
}

if (-not (Test-Path $templateFile)) { throw "Template not found: $templateFile" }

$body = Get-Content $templateFile -Raw | ConvertFrom-Json
$body.clientId = $ClientId
$body.clientSecret = $ClientSecret
if (-not $body.adapterKey) {
    $body | Add-Member -NotePropertyName adapterKey -NotePropertyValue $Template -Force
}
if ($body._comment) { $body.PSObject.Properties.Remove('_comment') }

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/auth/login" `
    -ContentType "application/json" `
    -Body (@{ username = $AdminUser; password = $AdminPassword } | ConvertTo-Json) `
    -WebSession $session
if ($login.data.mfaRequired) { throw "Admin MFA required; complete MFA or use dev profile" }

$result = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/identity-providers" `
    -ContentType "application/json" `
    -Body ($body | ConvertTo-Json -Depth 5) `
    -WebSession $session

Write-Host "Registered IdP: $($result.data.alias) ($($result.data.displayName))"
Write-Host "Login button: $BaseUrl/login"
