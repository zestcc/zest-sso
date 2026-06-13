# ZestSSO 生产备份脚本（Windows / PowerShell）
param(
    [string]$MysqlHost = "127.0.0.1",
    [int]$MysqlPort = 3306,
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = $env:MYSQL_PASSWORD,
    [string]$MysqlDatabase = "zest_sso",
    [string]$RedisHost = "127.0.0.1",
    [int]$RedisPort = 6379,
    [string]$OutDir = ".\backups",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$ts = Get-Date -Format "yyyyMMdd-HHmmss"
$dest = Join-Path $OutDir $ts
if (-not $DryRun) { New-Item -ItemType Directory -Path $dest -Force | Out-Null }

Write-Host "ZestSSO backup -> $dest"

if (-not $MysqlPassword) { throw "Set MYSQL_PASSWORD or -MysqlPassword" }

$sqlFile = Join-Path $dest "zest_sso.sql"
$redisFile = Join-Path $dest "redis-dump.rdb"

if ($DryRun) {
    Write-Host "[DRY] mysqldump -h $MysqlHost -P $MysqlPort -u $MysqlUser -p*** $MysqlDatabase > $sqlFile"
    Write-Host "[DRY] redis-cli -h $RedisHost -p $RedisPort BGSAVE + copy dump.rdb -> $redisFile"
    exit 0
}

$mysqldump = Get-Command mysqldump -ErrorAction SilentlyContinue
if (-not $mysqldump) { throw "mysqldump not found in PATH" }

& mysqldump -h $MysqlHost -P $MysqlPort -u $MysqlUser "-p$MysqlPassword" `
    --single-transaction --routines --triggers $MysqlDatabase | Set-Content -Encoding UTF8 $sqlFile
Write-Host "MySQL backup: $sqlFile ($((Get-Item $sqlFile).Length) bytes)"

$redisCli = Get-Command redis-cli -ErrorAction SilentlyContinue
if ($redisCli) {
    & redis-cli -h $RedisHost -p $RedisPort BGSAVE | Out-Null
    Start-Sleep -Seconds 2
    Write-Host "Redis: BGSAVE triggered; copy RDB from redis data dir manually if needed"
} else {
    Write-Host "WARN: redis-cli not found, skip Redis backup"
}

Write-Host "Backup completed: $dest"
