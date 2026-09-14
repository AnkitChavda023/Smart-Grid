param(
    [Parameter(Mandatory = $true)][string]$ConfigFile,
    [string]$ConnectUrl = "http://localhost:18083"
)
$ErrorActionPreference = "Stop"

$body = Get-Content $ConfigFile -Raw
$config = $body | ConvertFrom-Json
$name = $config.name

$existing = try { Invoke-RestMethod -Uri "$ConnectUrl/connectors/$name" -Method Get } catch { $null }
if ($existing) {
    Write-Host "Connector '$name' already exists, updating config"
    Invoke-RestMethod -Uri "$ConnectUrl/connectors/$name/config" -Method Put -ContentType "application/json" -Body ($config.config | ConvertTo-Json) | Out-Null
} else {
    Write-Host "Registering connector '$name'"
    Invoke-RestMethod -Uri "$ConnectUrl/connectors" -Method Post -ContentType "application/json" -Body $body | Out-Null
}

Start-Sleep -Seconds 2
$status = Invoke-RestMethod -Uri "$ConnectUrl/connectors/$name/status"
Write-Host "Connector state: $($status.connector.state)"
$status.tasks | ForEach-Object { Write-Host "Task $($_.id) state: $($_.state)" }
