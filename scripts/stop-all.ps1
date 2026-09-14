. "$PSScriptRoot\services.ps1"

# 1) Kill the console windows and their child java processes
if (Test-Path $PidFile) {
    $saved = Get-Content $PidFile -Raw | ConvertFrom-Json
    foreach ($s in $saved) {
        Write-Host "Stopping $($s.Name) (PID $($s.ProcessId))" -ForegroundColor Cyan
        # /T kills the whole tree (powershell -> mvn -> java)
        taskkill /PID $s.ProcessId /T /F 2>$null | Out-Null
    }
    Remove-Item $PidFile -Force
} else {
    Write-Warning "No PID file found - falling back to port cleanup only."
}

# 2) Safety net: kill anything still holding our ports
foreach ($name in $Services.Keys) {
    $port = $Services[$name]
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($c in $conns) {
        Write-Host "Freeing port $port (PID $($c.OwningProcess))" -ForegroundColor Yellow
        Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "`nAll services stopped." -ForegroundColor Green