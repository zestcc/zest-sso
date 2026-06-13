# ZestSSO 本地启动（本机 MySQL + Redis，无需 Docker）
# 用法: powershell -File scripts/start-local-mysql.ps1
# 环境变量: MYSQL_PASSWORD（默认 123456，与 application-dev.yml 一致）

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$LocalDir = Join-Path $Root ".local"
$RedisDir = Join-Path $LocalDir "redis"
$RedisZip = Join-Path $LocalDir "redis.zip"
$RedisUrl = "https://github.com/tporadowski/redis/releases/download/v5.0.14.1/Redis-x64-5.0.14.1.zip"
$MysqlPassword = if ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { "123456" }

function Ensure-PortableRedis {
    $redisServer = Get-ChildItem -Path $RedisDir -Recurse -Filter "redis-server.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($redisServer) {
        return $redisServer.FullName
    }

    Write-Host "[redis] Downloading portable Redis..."
    New-Item -ItemType Directory -Force -Path $LocalDir | Out-Null
    Invoke-WebRequest -Uri $RedisUrl -OutFile $RedisZip -UseBasicParsing
    Expand-Archive -Path $RedisZip -DestinationPath $RedisDir -Force
    Remove-Item $RedisZip -Force

    $redisServer = Get-ChildItem -Path $RedisDir -Recurse -Filter "redis-server.exe" | Select-Object -First 1
    if (-not $redisServer) {
        throw "redis-server.exe not found after extract"
    }
    return $redisServer.FullName
}

function Test-PortOpen([int]$Port) {
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $client.Connect("127.0.0.1", $Port)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

Set-Location $Root

if (-not (Test-PortOpen 3306)) {
    throw "MySQL 未在 127.0.0.1:3306 监听，请先安装并启动 MySQL，或执行 docker compose up -d"
}

$redisProc = Get-Process -Name "redis-server" -ErrorAction SilentlyContinue
if (-not $redisProc) {
    $redisExe = Ensure-PortableRedis
    Write-Host "[redis] Starting $redisExe"
    Start-Process -FilePath $redisExe -WorkingDirectory (Split-Path $redisExe) -WindowStyle Hidden
    Start-Sleep -Seconds 2
}

if (-not (Test-PortOpen 6379)) {
    throw "Redis 未在 127.0.0.1:6379 监听"
}
Write-Host "[mysql] Using 127.0.0.1:3306/zest_sso"
Write-Host "[redis] Ready on :6379"

Write-Host "[build] mvn package..."
mvn -q -pl zest-sso-server -am package -DskipTests -DfailIfNoTests=false

$jar = Get-ChildItem "$Root\zest-sso-server\target\zest-sso-server-*.jar" | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
if (-not $jar) {
    throw "Build output jar not found"
}

Write-Host "[run] Starting ZestSSO on http://localhost:9000 (profile: dev, MySQL)"
Remove-Item Env:SPRING_DATASOURCE_URL -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_DATASOURCE_USERNAME -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
$env:SPRING_PROFILES_ACTIVE = "dev"
if ($MysqlPassword) { $env:MYSQL_PASSWORD = $MysqlPassword }
$env:ZEST_SSO_ISSUER = "http://localhost:9000"
Start-Process java -ArgumentList "-jar", $jar.FullName -WorkingDirectory $Root

Write-Host ""
Write-Host "ZestSSO started (local MySQL + Redis)."
Write-Host "Health: http://localhost:9000/api/public/health"
Write-Host "Login:  admin / admin123"
