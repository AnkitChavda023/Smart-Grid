# Order matters: api-gateway must be last
$Services = [ordered]@{
    "auth-service"                = 8180
    "order-service"               = 8181
    "vendor-service"              = 8182
    "inventory-service"           = 8183
    "shipment-service"            = 8087
    "contract-service"            = 8088
    "pricing-service"             = 8089
    "notification-service"        = 8084
    "analytics-service"           = 8090
    "rag-service"                 = 8096
    "mcp-server"                  = 8095
    "disruption-detector-agent"   = 8085
    "reroute-planner-agent"       = 8086
    "vendor-evaluator-agent"      = 8091
    "sla-breach-analyst-agent"    = 8092
    "demand-forecaster-agent"     = 8093
    "contract-negotiation-agent"  = 8094
    "api-gateway"                 = 8000
}

$PidFile = Join-Path $PSScriptRoot ".running-services.json"