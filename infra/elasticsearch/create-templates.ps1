param(
    [string]$EsUrl = "http://localhost:9200"
)
$ErrorActionPreference = "Stop"

Write-Host "Creating component template 'smartgrid-defaults'"
$defaults = Get-Content (Join-Path $PSScriptRoot "templates/smartgrid-defaults.json") -Raw
Invoke-RestMethod -Method Put -Uri "$EsUrl/_component_template/smartgrid-defaults" -ContentType "application/json" -Body $defaults | Out-Null

Write-Host "Creating ILM policy 'analytics-ilm-policy'"
$ilmPolicy = Get-Content (Join-Path $PSScriptRoot "policies/analytics-ilm-policy.json") -Raw
Invoke-RestMethod -Method Put -Uri "$EsUrl/_ilm/policy/analytics-ilm-policy" -ContentType "application/json" -Body $ilmPolicy | Out-Null

Write-Host "Lowering ILM poll interval to 5s (local dev only, default is 10m - too slow to verify rollover in a test run)"
Invoke-RestMethod -Method Put -Uri "$EsUrl/_cluster/settings" -ContentType "application/json" -Body '{"transient":{"indices.lifecycle.poll_interval":"5s"}}' | Out-Null

$indexTemplates = @("vendor-catalog", "contracts", "analytics-orders", "analytics-vendors", "rag-chunks")
foreach ($name in $indexTemplates) {
    Write-Host "Creating index template '$name'"
    $body = Get-Content (Join-Path $PSScriptRoot "templates/$name-template.json") -Raw
    Invoke-RestMethod -Method Put -Uri "$EsUrl/_index_template/$name" -ContentType "application/json" -Body $body | Out-Null
}

$rolloverAliases = @("analytics_orders", "analytics_vendors")
foreach ($alias in $rolloverAliases) {
    $exists = $true
    try { Invoke-RestMethod -Method Get -Uri "$EsUrl/_alias/$alias" -ErrorAction Stop | Out-Null } catch { $exists = $false }
    if (-not $exists) {
        Write-Host "Bootstrapping rollover-alias index '$alias-000001' (alias: $alias)"
        $bootstrapBody = "{ ""aliases"": { ""$alias"": { ""is_write_index"": true } } }"
        Invoke-RestMethod -Method Put -Uri "$EsUrl/$alias-000001" -ContentType "application/json" -Body $bootstrapBody | Out-Null
    } else {
        Write-Host "Rollover-alias '$alias' already exists, skipping bootstrap"
    }
}

Write-Host ""
Write-Host "Registered index templates:"
(Invoke-RestMethod -Method Get -Uri "$EsUrl/_index_template").index_templates | ForEach-Object { $_.name }
