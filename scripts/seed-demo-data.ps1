<#
.SYNOPSIS
  Populates a running SmartGrid stack with realistic Indian Automotive JIT demo data:
  Tier-1 auto vendors, 8 car assembly SKUs, regional automotive hub warehouses,
  hourly JIT production batch orders (most reaching CONFIRMED), and simulated
  monsoon logistics disruptions for AI agent evaluation.

.DESCRIPTION
  Talks to each backend service directly on its own port (auth is only enforced at the
  API Gateway, per M11 - see docs/modules/M11-api-gateway.md - so seeding doesn't need a
  token). Requires the full stack to already be running: infra (.\scripts\up.ps1) and all
  18 services (.\scripts\start-all.ps1).

.PARAMETER OrderCount
  How many orders to create. Default 50.

.EXAMPLE
  .\scripts\seed-demo-data.ps1
  .\scripts\seed-demo-data.ps1 -OrderCount 100
#>
param(
    [int]$OrderCount = 50
)

$ErrorActionPreference = "Stop"

$Auth       = "http://localhost:8180"
$Order      = "http://localhost:8181"
$Vendor     = "http://localhost:8182"
$Inventory  = "http://localhost:8183"
$Pricing    = "http://localhost:8089"
$Disruption = "http://localhost:8085"

function Invoke-Json {
    param([string]$Method, [string]$Uri, [hashtable]$Body)
    $json = $Body | ConvertTo-Json -Depth 6
    return Invoke-RestMethod -Method $Method -Uri $Uri -ContentType "application/json" -Body $json
}

Write-Host "=== 1. Demo login account ===" -ForegroundColor Cyan
try {
    Invoke-Json -Method Post -Uri "$Auth/auth/register" -Body @{
        username = "demo.planner"
        password = "Demo12345!"
        role     = "PLANNER"
    } | Out-Null
    Write-Host "Created demo.planner / Demo12345! (role: PLANNER - Chakan Assembly Plant)"
} catch {
    Write-Host "demo.planner already exists, skipping" -ForegroundColor DarkGray
}

Write-Host "`n=== 2. Indian Automotive SKU price rules ===" -ForegroundColor Cyan
$skuCatalog = @{
    "sku-1" = @{ name = "ECU Microcontroller & Engine Management"; basePrice = 120.00 }
    "sku-2" = @{ name = "EV 40kWh Traction Battery Pack";          basePrice = 2850.00 }
    "sku-3" = @{ name = "ABS Caliper & Braking Assembly";          basePrice = 145.00 }
    "sku-4" = @{ name = "Engine & Cabin Wiring Harness";            basePrice = 75.00 }
    "sku-5" = @{ name = "6-Speed DCT Transmission Gearbox";         basePrice = 520.00 }
    "sku-6" = @{ name = "10.25in Digital Cockpit & Telematics";     basePrice = 180.00 }
    "sku-7" = @{ name = "High-Tensile Chassis Subframe";           basePrice = 240.00 }
    "sku-8" = @{ name = "Dual SRS Front & Curtain Airbags";         basePrice = 95.00 }
}
$skus = @("sku-1", "sku-2", "sku-3", "sku-4", "sku-5", "sku-6", "sku-7", "sku-8")
$today = Get-Date -Format "yyyy-MM-dd"
$nextYear = (Get-Date).AddYears(1).ToString("yyyy-MM-dd")
$skuBasePrice = @{}
foreach ($sku in $skus) {
    $price = $skuCatalog[$sku].basePrice
    $skuBasePrice[$sku] = $price
    Invoke-Json -Method Post -Uri "$Pricing/price-rules" -Body @{
        skuId     = $sku
        price     = $price
        validFrom = $today
        validTo   = $nextYear
    } | Out-Null
}
Write-Host "Priced $($skus.Count) automotive SKUs ($($skus -join ', '))"

Write-Host "`n=== 3. Indian Regional Hub Warehouse stock ===" -ForegroundColor Cyan
$regions = @("india-west", "india-north", "india-south", "india-west-gujarat", "india-central", "india-south-tech")
$warehouses = @("wh-pune-chakan", "wh-manesar-ncr", "wh-chennai-auto", "wh-sanand-gujarat", "wh-pithampur-mp", "wh-hosur-hub")
foreach ($sku in $skus) {
    foreach ($wh in $warehouses) {
        Invoke-Json -Method Post -Uri "$Inventory/inventory/replenish" -Body @{
            skuId       = $sku
            warehouseId = $wh
            quantity    = Get-Random -Minimum 300 -Maximum 1500 # Lean JIT buffer stock
        } | Out-Null
    }
}
Write-Host "Stocked $($skus.Count) SKUs across $($warehouses.Count) Indian automotive hub warehouses"

Write-Host "`n=== 4. Tier-1 Indian Automotive Vendors ===" -ForegroundColor Cyan
$vendorDefinitions = @(
    @{ name = "Tata AutoComp Systems";         region = "india-west";         capabilities = "EV battery pack assembly; battery thermal management; automotive plastics and interior trim; JIT sequencing" },
    @{ name = "Motherson Sumi Wiring India";   region = "india-north";        capabilities = "high-voltage vehicle wiring harnesses; automotive cabin electrical architecture; wiring looms; precision terminals" },
    @{ name = "Bharat Forge Ltd";              region = "india-west";         capabilities = "forged chassis subframes; high-tensile steel components; CNC machining; powertrain forging; steering knuckles" },
    @{ name = "Bosch India Ltd";               region = "india-south-tech";   capabilities = "electronic control units (ECU); ABS electronic stability control; powertrain sensors; fuel injection systems" },
    @{ name = "Lucas TVS";                     region = "india-south";        capabilities = "starter motors; alternators; brushless DC motors; automotive electrical assemblies; wiper motor drives" },
    @{ name = "Endurance Technologies";        region = "india-west";         capabilities = "ABS disc brake assemblies; aluminum die-cast housings; suspension shock absorbers; hydraulic calipers" },
    @{ name = "Uno Minda Ltd";                 region = "india-north";        capabilities = "connected digital cockpits; steering wheel switches; SRS airbag modules; telematics control units" },
    @{ name = "Varroc Engineering";            region = "india-west";         capabilities = "automotive exterior LED lighting; digital instrument clusters; polymer exterior panels; mirror assemblies" },
    @{ name = "Sona BLW Precision Forgings";   region = "india-north";        capabilities = "dual-clutch transmission gears; differential assemblies; EV traction motor gears; forged bevel gears" },
    @{ name = "Sundram Fasteners";             region = "india-south";        capabilities = "high-tensile engine fasteners; powertrain shafts; precision forged automotive hardware; radiator caps" },
    @{ name = "Brakes India Ltd";              region = "india-south";        capabilities = "hydraulic disc brake calipers; electronic parking brake systems; master cylinders; ABS braking valves" },
    @{ name = "Subros Ltd";                    region = "india-north";        capabilities = "automotive HVAC thermal systems; engine cooling radiators; compressor modules; condenser units" },
    @{ name = "Exide Industries";              region = "india-west";         capabilities = "auxiliary 12V lead-acid batteries; lithium-ion cell modules; power storage systems; battery management" },
    @{ name = "Wheels India Ltd";              region = "india-south";        capabilities = "forged steel wheels; light alloy wheels; commercial vehicle air suspension systems; brake drums" },
    @{ name = "Gabriel India";                 region = "india-west";         capabilities = "front McPherson struts; hydraulic shock absorbers; high-speed ride control systems; gas dampers" },
    @{ name = "Minda Corporation";             region = "india-north";        capabilities = "electronic immobilizers; smart key fobs; vehicle security gateways; telematics wiring; sensor modules" },
    @{ name = "Suprajit Engineering";          region = "india-south-tech";   capabilities = "automotive control cables; gear shifter linkages; halogen and LED automotive lighting; speedo cables" },
    @{ name = "Lumax Auto Technologies";       region = "india-west";         capabilities = "integrated automatic gear shifters; metallic chassis stampings; telematics modules; air intake ducts" }
)

$vendorIds = @()
for ($i = 0; $i -lt $vendorDefinitions.Count; $i++) {
    $vDef = $vendorDefinitions[$i]
    $vendorSkus = Get-Random -InputObject $skus -Count (Get-Random -Minimum 3 -Maximum 5)
    # Ensure Tata AutoComp has all 4 disruption SKUs and backup Varroc has them too
    if ($vDef.name -match "Tata AutoComp") {
        foreach ($s in @("sku-1", "sku-2", "sku-3", "sku-4")) { if (-not ($vendorSkus -contains $s)) { $vendorSkus += $s } }
    }
    if ($vDef.name -match "Varroc Engineering") {
        foreach ($s in @("sku-1", "sku-2", "sku-3", "sku-4")) { if (-not ($vendorSkus -contains $s)) { $vendorSkus += $s } }
    }
    if ($vDef.name -match "Bosch India" -and -not ($vendorSkus -contains "sku-1")) { $vendorSkus += "sku-1" }
    if ($vDef.name -match "Bharat Forge" -and -not ($vendorSkus -contains "sku-7")) { $vendorSkus += "sku-7" }

    $skuPayload = @($vendorSkus | ForEach-Object {
        $base = $skuBasePrice[$_]
        @{
            skuId        = $_
            price        = [math]::Round($base * (Get-Random -Minimum 0.90 -Maximum 1.15), 2)
            leadTimeDays = Get-Random -Minimum 1 -Maximum 5 # Short JIT lead times
        }
    })

    $created = Invoke-Json -Method Post -Uri "$Vendor/vendors" -Body @{
        name         = $vDef.name
        region       = $vDef.region
        capabilities = $vDef.capabilities
        skus         = $skuPayload
    }
    $vendorIds += $created.id
}
Write-Host "Created $($vendorIds.Count) Tier-1 automotive vendors across $($regions.Count) Indian corridors"

Write-Host "`n=== 5. JIT Plant Assembly Line Batch Orders ===" -ForegroundColor Cyan
$requesters = @("arun.sharma", "neha.patel", "vikram.singh", "ananya.deshmukh", "rajesh.verma", "priya.iyer", "sanjay.kulkarni", "deepak.mehta")
$orderIds = @()
for ($i = 0; $i -lt $OrderCount; $i++) {
    if ($i -lt 4) {
        # 4 Distinct Critical Automotive Component SKUs for JIT Assembly Line (all from primary vendor Tata AutoComp)
        $demoSkus = @(
            @{ skuId = "sku-2"; quantity = 30 }, # Order 1: EV 40kWh Traction Battery Pack
            @{ skuId = "sku-1"; quantity = 50 }, # Order 2: ECU Microcontroller & Engine Management
            @{ skuId = "sku-3"; quantity = 40 }, # Order 3: ABS Caliper & Electronic Braking Assembly
            @{ skuId = "sku-4"; quantity = 60 }  # Order 4: Engine & Cabin Wiring Harness
        )
        $items = @($demoSkus[$i])
    } else {
        $itemCount = if ((Get-Random -Minimum 1 -Maximum 100) -le 20) { 2 } else { 1 }
        $items = @(1..$itemCount | ForEach-Object {
            @{ skuId = (Get-Random -InputObject $skus); quantity = Get-Random -Minimum 10 -Maximum 60 }
        })
    }

    $created = Invoke-Json -Method Post -Uri "$Order/orders" -Body @{
        requestedBy       = (Get-Random -InputObject $requesters)
        destinationRegion = "india-west" # Chakan Main Assembly Plant
        items             = $items
    }
    $orderIds += @{ id = $created.id; sku = $items[0].skuId; quantity = $items[0].quantity }

    if (($i + 1) % 10 -eq 0) { Write-Host "  ...$($i + 1)/$OrderCount orders created" -ForegroundColor DarkGray }
}
Write-Host "Created $($orderIds.Count) JIT line-feed orders"

Write-Host "`n=== 6/7. Confirming vendors and quoting roughly half the orders (pushes them to CONFIRMED) ===" -ForegroundColor Cyan
$confirmed = 0
$skippedNoVendor = 0
# Guarantee the 4 disruption demo orders are included in confirmation
$demoOrderBatch = $orderIds | Select-Object -First 4
$otherOrdersToQuote = $orderIds | Select-Object -Skip 4 | Get-Random -Count ([math]::Max(0, [math]::Floor($orderIds.Count / 2) - 4))
$toQuote = @($demoOrderBatch) + @($otherOrdersToQuote)
foreach ($o in $toQuote) {
    $vendorId = $null
    for ($attempt = 0; $attempt -lt 10; $attempt++) {
        $fetchedOrder = Invoke-RestMethod -Uri "$Order/orders/$($o.id)"
        if ($fetchedOrder.vendorId) { $vendorId = $fetchedOrder.vendorId; break }
        Start-Sleep -Milliseconds 1500
    }
    if (-not $vendorId) {
        $skippedNoVendor++
        continue
    }

    try {
        $quote = Invoke-Json -Method Post -Uri "$Pricing/quotes" -Body @{
            orderId  = $o.id
            vendorId = $vendorId
            items    = @(@{ skuId = $o.sku; quantity = $o.quantity })
        }
        Invoke-RestMethod -Method Post -Uri "$Pricing/quotes/$($quote.id)/accept" | Out-Null
        $confirmed++
    } catch {
        Write-Host "  Quote failed for order $($o.id): $($_.Exception.Message)" -ForegroundColor DarkGray
    }
}
Write-Host "Quoted and accepted $confirmed orders ($skippedNoVendor had no vendor available for their SKU after polling)"

Write-Host "`n=== 8. Simulating Mumbai-Pune Expressway Disruption (for AI agent pages) ===" -ForegroundColor Cyan
# Guarantee the primary vendor with the 4 in-flight orders is disrupted, plus up to 2 other vendors
$targetDisruptedVendor = $vendorIds[0] # Tata AutoComp Systems
$otherDisruptedVendors = $vendorIds | Select-Object -Skip 1 | Get-Random -Count ([math]::Min(2, $vendorIds.Count - 1))
$disruptionVendors = @($targetDisruptedVendor) + @($otherDisruptedVendors)
$disruptionResults = @()
foreach ($vid in $disruptionVendors) {
    try {
        $d = Invoke-Json -Method Post -Uri "$Disruption/disruptions/simulate" -Body @{
            vendorId = $vid
            signals  = @(
                @{ type = "SHIPMENT_DELAY"; severity = 2.5; description = "Monsoon landslide on Mumbai-Pune Expressway (Bhor Ghat NH-48). Heavy freight delayed by 6.5 hours." },
                @{ type = "SLA_BREACH";     severity = 3.0; description = "Shipment missed JIT delivery window by 7 hours; Chakan assembly line stoppage imminent." }
            )
        }
        $disruptionResults += $d
        Write-Host "  Simulated a disruption for vendor $vid -> id=$($d.id) status=$($d.status) confidence=$($d.confidence)"
    } catch {
        Write-Host "  Disruption simulation failed for vendor $vid : $($_.Exception.Message)" -ForegroundColor DarkGray
    }
}

Write-Host "`n=== Done ===" -ForegroundColor Green
Write-Host "Log in at http://localhost:5173/login with demo.planner / Demo12345!"
Write-Host "$($vendorIds.Count) Tier-1 vendors, $($orderIds.Count) JIT orders ($confirmed confirmed), $($disruptionVendors.Count) simulated disruptions."
Write-Host ""
if ($disruptionResults | Where-Object { $_.status -ne "PUBLISHED" }) {
    Write-Host "Note: the LLM's confidence for these signals landed below the 0.70 auto-publish threshold," -ForegroundColor Yellow
    Write-Host "so they're routed to PENDING_REVIEW instead of PUBLISHED - by design, not a failure. They won't" -ForegroundColor Yellow
    Write-Host "appear on the Agent Traces page's live timeline, but the real reasoning trace is still there:" -ForegroundColor Yellow
    foreach ($d in $disruptionResults) {
        Write-Host "  curl http://localhost:8085/disruptions/$($d.id)/reasoning" -ForegroundColor Yellow
    }
}
