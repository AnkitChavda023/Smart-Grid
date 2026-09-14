$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $root "docker-compose.yml"

Write-Host "Starting SmartGrid infrastructure..." -ForegroundColor Cyan
docker compose -f $composeFile up -d

Write-Host "Waiting for all services to report healthy..." -ForegroundColor Cyan
$maxWaitSeconds = 300
$elapsed = 0
while ($true) {
    $states = docker compose -f $composeFile ps --format json | ConvertFrom-Json
    $unhealthy = $states | Where-Object { $_.Health -and $_.Health -ne "healthy" }
    if (-not $unhealthy -or @($unhealthy).Count -eq 0) { break }
    if ($elapsed -ge $maxWaitSeconds) {
        Write-Host "Timed out waiting for services to become healthy." -ForegroundColor Red
        docker compose -f $composeFile ps
        exit 1
    }
    Start-Sleep -Seconds 5
    $elapsed += 5
}

Write-Host "Creating Kafka topics..." -ForegroundColor Cyan
& (Join-Path $root "infra/kafka/create-topics.ps1")

Write-Host "Creating Elasticsearch index templates..." -ForegroundColor Cyan
& (Join-Path $root "infra/elasticsearch/create-templates.ps1")

Write-Host "Running verification checks..." -ForegroundColor Cyan
& (Join-Path $root "infra/healthchecks/check-all.ps1")
