# 运行全部测试（单元 + 集成，需本机 MySQL :3306）
# 用法:
#   $env:MYSQL_PASSWORD = "你的密码"
#   powershell -File scripts/test-local.ps1

param(
    [string]$MysqlPassword = $env:MYSQL_PASSWORD
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

if (-not $MysqlPassword) {
    Write-Error "请先设置 MySQL 密码: `$env:MYSQL_PASSWORD = '你的root密码'"
}

$env:MYSQL_PASSWORD = $MysqlPassword
Write-Host ">>> 运行全部测试 (MySQL zest_sso_test, profile mysql-it)..."
mvn -pl zest-sso-server -am test -Pmysql-it -DfailIfNoTests=false
