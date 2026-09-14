<#
.SYNOPSIS
  Wrapper alias for seed-demo-data.ps1 to support commands referenced in testing guides.
#>
param(
    [int]$OrderCount = 50
)

& "$PSScriptRoot\seed-demo-data.ps1" -OrderCount $OrderCount
