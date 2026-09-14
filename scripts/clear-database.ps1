<#
.SYNOPSIS
  Completely resets and clears all SmartGrid state:
  1. Truncates all tables across all 16 PostgreSQL databases.
  2. Clears Elasticsearch analytics and search indices.
  3. Flushes Redis cache keys.
  Keeps all schemas, PostGIS, pgvector extensions, and index templates intact.

.EXAMPLE
  .\scripts\clear-database.ps1
#>

$ErrorActionPreference = "Stop"

$ContainerName = "smartgrid-postgres"
$PostgresUser  = "smartgrid"

$Databases = @(
    "auth_db",
    "order_db",
    "vendor_db",
    "inventory_db",
    "pricing_db",
    "shipment_db",
    "contract_db",
    "notification_db",
    "analytics_db",
    "rag_db",
    "disruption_detector_db",
    "reroute_planner_db",
    "vendor_evaluator_db",
    "sla_breach_analyst_db",
    "demand_forecaster_db",
    "contract_negotiation_db"
)

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " Clearing SmartGrid State (Postgres + ES + Redis) " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# 1. Check & Clear PostgreSQL
$running = docker ps --filter "name=$ContainerName" --filter "status=running" -q
if (-not $running) {
    Write-Error "Container '$ContainerName' is not running. Please start it with 'docker compose up -d postgres' or '.\scripts\up.ps1'."
    exit 1
}

$truncateSql = @"
DO `$do`$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename NOT IN ('spatial_ref_sys', 'geography_columns', 'geometry_columns')
    ) LOOP
        EXECUTE 'TRUNCATE TABLE "' || r.tablename || '" CASCADE;';
    END LOOP;
END `$do`$;
"@

Write-Host "`n[1/3] Truncating PostgreSQL tables..." -ForegroundColor Cyan
foreach ($db in $Databases) {
    Write-Host "  - Truncating $db..." -ForegroundColor Yellow
    $truncateSql | docker exec -i $ContainerName psql -U $PostgresUser -d $db | Out-Null
}

# 2. Clear Elasticsearch Indices
$esRunning = docker ps --filter "name=smartgrid-elasticsearch" --filter "status=running" -q
if ($esRunning) {
    Write-Host "`n[2/3] Clearing Elasticsearch documents..." -ForegroundColor Cyan
    $esIndices = @("analytics_orders*", "analytics_vendors*", "contracts_index", "vendor-catalog*", "rag-chunks*")
    foreach ($idx in $esIndices) {
        try {
            Invoke-RestMethod -Method Post -Uri "http://localhost:9200/$idx/_delete_by_query" -ContentType "application/json" -Body '{"query":{"match_all":{}}}' -ErrorAction SilentlyContinue | Out-Null
            Write-Host "  - Cleared index $idx" -ForegroundColor Yellow
        } catch {
            # Index may not exist yet, safe to ignore
        }
    }
}

# 3. Flush Redis
$redisRunning = docker ps --filter "name=smartgrid-redis" --filter "status=running" -q
if ($redisRunning) {
    Write-Host "`n[3/3] Flushing Redis cache..." -ForegroundColor Cyan
    docker exec smartgrid-redis redis-cli FLUSHALL | Out-Null
    Write-Host "  - Redis cache flushed." -ForegroundColor Yellow
}

Write-Host "`n==================================================" -ForegroundColor Green
Write-Host " Complete system reset successful!                " -ForegroundColor Green
Write-Host " PostgreSQL, Elasticsearch, and Redis are clean.  " -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
