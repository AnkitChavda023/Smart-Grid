$ErrorActionPreference = "Stop"

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$composeFile = Join-Path $projectRoot "docker-compose.yml"
$topics = Get-Content (Join-Path $PSScriptRoot "topics.json") -Raw | ConvertFrom-Json

foreach ($topic in $topics) {
    $retentionMs = [int64]$topic.retentionDays * 24 * 60 * 60 * 1000
    Write-Host "Creating topic '$($topic.name)' (partitions=$($topic.partitions), key=$($topic.partitionKey), retention=$($topic.retentionDays)d)"

    docker compose -f $composeFile exec -T kafka `
        kafka-topics --bootstrap-server kafka:29092 `
        --create --if-not-exists `
        --topic $topic.name `
        --partitions $topic.partitions `
        --replication-factor 1 `
        --config "retention.ms=$retentionMs"
}

Write-Host ""
Write-Host "Current topic list:"
docker compose -f $composeFile exec -T kafka kafka-topics --bootstrap-server kafka:29092 --list
