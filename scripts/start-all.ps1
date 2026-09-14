param(
    [int]$DelaySeconds = 8,        # gap between service starts
    [int]$GatewayDelay = 20        # extra wait before api-gateway
)

. "$PSScriptRoot\services.ps1"

$repoRoot = Split-Path $PSScriptRoot -Parent
$running = @()

foreach ($name in $Services.Keys) {
    $dir = Join-Path $repoRoot "services\$name"

    if (-not (Test-Path $dir)) {
        Write-Warning "Skipping $name - folder not found: $dir"
        continue
    }

    if ($name -eq "api-gateway") {
        Write-Host "Waiting $GatewayDelay s before starting the gateway..." -ForegroundColor Yellow
        Start-Sleep -Seconds $GatewayDelay
    }

    $port = $Services[$name]
    Write-Host "Starting $name on :$port" -ForegroundColor Cyan

    $cmd = "`$Host.UI.RawUI.WindowTitle = '$name :$port'; " +
           "Set-Location '$dir'; " +
           "mvn spring-boot:run"

    $proc = Start-Process powershell.exe `
        -ArgumentList "-NoExit", "-Command", $cmd `
        -PassThru

    $running += [pscustomobject]@{
        Name      = $name
        Port      = $port
        ProcessId = $proc.Id
    }

    Start-Sleep -Seconds $DelaySeconds
}

$running | ConvertTo-Json | Set-Content -Path $PidFile
Write-Host "`nLaunched $($running.Count) services. PIDs saved to $PidFile" -ForegroundColor Green
$running | Format-Table -AutoSize