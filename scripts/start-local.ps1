# ZestSSO 本地联调启动脚本（Docker MySQL，密码 root）
# 用法: powershell -File scripts/start-local.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

Write-Host ">>> 启动 Docker 依赖 (MySQL + Redis)..."
Set-Location $Root
docker compose up -d

Write-Host ">>> 等待 MySQL 就绪..."
Start-Sleep -Seconds 8

Write-Host ">>> 编译 ZestSSO..."
mvn -q -pl zest-sso-server -am package -DskipTests -DfailIfNoTests=false

Write-Host ">>> 启动 ZestSSO (端口 9000)..."
$jar = Get-ChildItem "$Root\zest-sso-server\target\zest-sso-server-*.jar" | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
if (-not $jar) {
    Write-Error "未找到构建产物，请先 mvn package"
}

# 仅 Docker 场景覆盖数据源；勿在本机 MySQL(123456) 终端里残留这些变量
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
$env:SPRING_PROFILES_ACTIVE = "default"
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://127.0.0.1:3306/zest_sso?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "root"
$env:SPRING_DATA_REDIS_HOST = "127.0.0.1"
$env:ZEST_SSO_ISSUER = "http://localhost:9000"

Start-Process java -ArgumentList "-jar", $jar.FullName -WorkingDirectory $Root

Write-Host ""
Write-Host "ZestSSO 已启动: http://localhost:9000"
Write-Host "管理控制台:     http://localhost:5175  (cd zest-sso-admin && npm run dev)"
Write-Host "健康检查:       http://localhost:9000/api/public/health"
Write-Host "默认账号:       admin / admin123"
Write-Host ""
Write-Host "本机 MySQL 请用: powershell -File scripts/start-local-mysql.ps1"
