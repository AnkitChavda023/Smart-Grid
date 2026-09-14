$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $root "docker-compose.yml"

Write-Host "Stopping SmartGrid infrastructure and deleting all volumes..." -ForegroundColor Yellow
docker compose -f $composeFile down -v

Write-Host "Reset complete. Run scripts/up.ps1 to start fresh." -ForegroundColor Green
