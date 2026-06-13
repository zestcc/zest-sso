# Generate RSA JWT key pair for ZestSSO (PKCS#8 PEM)
param(
    [string]$OutDir = ".\.local\keys"
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$rsa = [System.Security.Cryptography.RSA]::Create(2048)
$privateBytes = $rsa.ExportPkcs8PrivateKey()
$publicBytes = $rsa.ExportSubjectPublicKeyInfo()

$privateB64 = [Convert]::ToBase64String($privateBytes, [Base64FormattingOptions]::InsertLineBreaks)
$publicB64 = [Convert]::ToBase64String($publicBytes, [Base64FormattingOptions]::InsertLineBreaks)

$privatePem = "-----BEGIN PRIVATE KEY-----`n$privateB64`n-----END PRIVATE KEY-----`n"
$publicPem = "-----BEGIN PUBLIC KEY-----`n$publicB64`n-----END PUBLIC KEY-----`n"

$privatePath = Join-Path $OutDir "jwt-private.pem"
$publicPath = Join-Path $OutDir "jwt-public.pem"
Set-Content -Path $privatePath -Value $privatePem -Encoding ascii -NoNewline
Set-Content -Path $publicPath -Value $publicPem -Encoding ascii -NoNewline

Write-Host "JWT keys written:"
Write-Host "  private: $privatePath"
Write-Host "  public:  $publicPath"
Write-Host ""
Write-Host "Configure in application-prod.yml:"
Write-Host "  zest.sso.jwt.private-key-path: $privatePath"
Write-Host "  zest.sso.jwt.public-key-path:  $publicPath"
