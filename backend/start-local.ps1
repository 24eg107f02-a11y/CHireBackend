$ErrorActionPreference = "Stop"

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Maven was not found. Install Maven and make sure mvn is on PATH."
}

$env:SERVER_PORT = "8081"
$env:DB_URL = "jdbc:mysql://localhost:3306/campushire_demo?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = Read-Host "MySQL username [root]"
if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME)) {
    $env:DB_USERNAME = "root"
}

$securePassword = Read-Host "MySQL password (press Enter if blank)" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password

if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    $jwtBytes = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
    $env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
}

Set-Location $PSScriptRoot
mvn spring-boot:run
