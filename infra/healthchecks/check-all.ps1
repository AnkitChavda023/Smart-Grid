$ErrorActionPreference = "Continue"

$projectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$composeFile = Join-Path $projectRoot "docker-compose.yml"
$results = @()

function Test-Check {
    param([string]$Name, [scriptblock]$Test)
    try {
        $ok = & $Test
        $status = if ($ok) { "PASS" } else { "FAIL" }
    } catch {
        $status = "FAIL"
    }
    $script:results += [PSCustomObject]@{ Check = $Name; Status = $status }
}

Test-Check "All containers running" {
    $states = docker compose -f $composeFile ps --format json | ConvertFrom-Json
    $states.Count -gt 0 -and (($states | Where-Object { $_.State -ne "running" }).Count -eq 0)
}

Test-Check "Kafka: all 13 topics exist" {
    $topics = docker compose -f $composeFile exec -T kafka kafka-topics --bootstrap-server kafka:29092 --list
    $expected = @("order-events","vendor-events","disruption-signals","disruption-detected","reroute-decisions","notifications","inventory-events","shipment-events","quote-events","sla-events","analytics-aggregates","dlq-all","contract-sync-events")
    ($expected | Where-Object { $topics -notcontains $_ }).Count -eq 0
}

Test-Check "Schema Registry reachable" {
    (Invoke-WebRequest -Uri "http://localhost:18081/subjects" -UseBasicParsing).StatusCode -eq 200
}

Test-Check "Elasticsearch cluster health is green or yellow" {
    $health = Invoke-RestMethod -Uri "http://localhost:9200/_cluster/health"
    $health.status -in @("green", "yellow")
}

Test-Check "Redis PING returns PONG" {
    $pong = docker compose -f $composeFile exec -T redis redis-cli ping
    $pong.Trim() -eq "PONG"
}

Test-Check "Zipkin UI reachable on :9411" {
    (Invoke-WebRequest -Uri "http://localhost:9411/zipkin/" -UseBasicParsing).StatusCode -eq 200
}

Test-Check "Postgres accepting connections" {
    $out = docker compose -f $composeFile exec -T postgres pg_isready -U smartgrid
    $out -match "accepting connections"
}

Test-Check "Prometheus healthy" {
    (Invoke-WebRequest -Uri "http://localhost:9090/-/healthy" -UseBasicParsing).StatusCode -eq 200
}

Test-Check "Grafana healthy" {
    (Invoke-RestMethod -Uri "http://localhost:3001/api/health").database -eq "ok"
}

$results | Format-Table -AutoSize

$failures = $results | Where-Object { $_.Status -eq "FAIL" }
if ($failures.Count -gt 0) {
    Write-Host "$($failures.Count) check(s) failed." -ForegroundColor Red
    exit 1
} else {
    Write-Host "All checks passed." -ForegroundColor Green
}
