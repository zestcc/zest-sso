# 构建 ZestSSO Admin 并集成到后端静态资源
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$AdminDir = Join-Path $Root "zest-sso-admin"
$TargetDir = Join-Path $Root "zest-sso-server\src\main\resources\static\admin"

Write-Host ">>> 构建 Admin 前端..."
Set-Location $AdminDir
npm run build

Write-Host ">>> 复制构建产物到后端 static/admin ..."
if (Test-Path $TargetDir) {
    Remove-Item $TargetDir -Recurse -Force
}
New-Item -ItemType Directory -Path $TargetDir -Force | Out-Null
Copy-Item -Path (Join-Path $AdminDir "dist\*") -Destination $TargetDir -Recurse -Force

Write-Host "完成。生产访问地址: http://localhost:9000/admin/"
