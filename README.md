#   

AI-powered supply chain disruption detection and autonomous rerouting system - 18 Spring Boot microservices, 6 LangChain4j AI agents running on a local LLM by default, a Kafka event backbone, a RAG pipeline, an MCP tool server, and a React dashboard.

## Table of contents

- [Introduction](#introduction)
- [Demo](#demo)
- [Methodology](#methodology)
- [File structure](#file-structure)
- [Getting started](#getting-started)
- [Models](#models)

## Introduction

A company places orders with vendors; vendors sometimes fail to deliver on time, or their reliability quietly degrades. SmartGrid watches every order and every vendor for early signs of trouble, and when it finds one, an AI agent investigates automatically - checking the vendor's real history, cross-referencing recent shipments and SLA breaches - and either fixes it by rebooking a backup vendor, or escalates to a human when it isn't confident enough to act alone.

The two things that make this project non-trivial to build correctly:

- **Real event-driven consistency, not a REST monolith.** An order moving from `PENDING` to `CONFIRMED` depends on three independent services agreeing (stock reserved, vendor confirmed, quote accepted) - a SAGA over Kafka, not a single transaction.
- **AI agents that don't quietly guess.** Every agent decision carries a confidence score, and a decision only auto-publishes above a threshold (0.70 for the disruption detector). Below it, the decision is escalated to a human instead of applied - and because an LLM's self-reported confidence can't be trusted blindly, the score is clamped into a valid `0.0–1.0` range in code before it ever gates a real decision.

The AI layer runs entirely on a local Ollama model out of the box - no cloud API key, no billing account needed to see it work - with OpenAI supported as a drop-in alternative via environment variables only.

## Demo

Two ways to see it populated with real data - pick whichever fits:

| | Script | Manual |
|---|---|---|
| Guide | [DEMO_GUIDE.md](DEMO_GUIDE.md) | [MANUAL_DEMO_GUIDE.md](MANUAL_DEMO_GUIDE.md) |
| Command | `.\scripts\seed-demo-data.ps1` | step-by-step `curl` + UI clicks |
| Best for | populated app fast | narrating each concept live |

Either way, log in at **http://localhost:5173/login**. Register your own account at `/register`, or use the seed script's ready-made account:

```
username: demo.planner
password: Demo12345!
```

## Methodology

**Backend - 18 Spring Boot microservices**, split into three tiers:

1. **Core domain services** - `auth`, `order`, `vendor`, `inventory`, `pricing` (quotes), `shipment`, `contract`, `notification`, `analytics`, `api-gateway`.
2. **AI infrastructure** - `rag-service` (pgvector-backed retrieval), `mcp-server` (Model Context Protocol tools the agents call: stock checks, vendor lookups, contract drafts).
3. **6 AI agents**, each a full Spring Boot service with its own database, LLM analyzer, and confidence gate:

   | Agent | Job |
   |---|---|
   | Disruption Detector | Watches Kafka signals per vendor; decides whether a real disruption is occurring |
   | Reroute Planner | Runs a ReAct tool-calling loop to find a backup vendor, check stock, and accept a quote |
   | Vendor Evaluator | Scores a vendor's health/trend from its real lead-time and breach history |
   | SLA Breach Analyst | Turns breach history into a Poisson-process breach probability |
   | Demand Forecaster | Produces P10/P50/P90 demand forecasts, seasonally adjusted |
   | Contract Negotiation | Drafts revised contract terms; a human always approves before it's real |

**Event backbone**: Kafka with 13 topics and strongly-typed Avro schemas via Confluent Schema Registry - no service calls another service's REST API to react to something that already happened; it consumes the event. Order writes use the **transactional outbox pattern** (an `outbox_event` row written in the same DB transaction as the state change, relayed to Kafka via Debezium/Kafka Connect) so an event is never lost to a mid-transaction crash.

**Order lifecycle (SAGA)**: `PENDING → CONFIRMED → SHIPPED → DELIVERED → CLOSED`, with a `CANCELLED` branch available from `PENDING`/`CONFIRMED` only. `CONFIRMED` requires three independent flags to all be true (stock reserved, vendor confirmed, quote accepted) - enforced by an explicit state machine, not convention.

**AI agents**: LangChain4j 1.18.1, defaulting to a local Ollama model (`llama3.1:8b` for chat, `nomic-embed-text` for embeddings) via Ollama's OpenAI-compatible API - switching to real OpenAI is a pure config change (three env vars), no code changes. Every agent follows the same fallback chain: LLM call → retry once → rule-based heuristic, and the resulting confidence is clamped to `[0.0, 1.0]` before it gates anything.

**Frontend**: React 18 + TypeScript + Vite + Tailwind, with a real light/dark theme, live WebSocket (STOMP) push updates, and role-aware UI (`SUPPLIER` / `PLANNER` / `ADMIN`). Pages: Overview, Orders, Vendors, Disruptions, Reroutes, Analytics, Agent Traces.

## File structure

```
SmartGrid/
├── docker-compose.yml           Layer 0 infra: Kafka, Postgres, Redis, Elasticsearch, observability
├── pom.xml                      Root Maven reactor - registers every module below
├── .env.example                 Env var template for docker compose (copy to .env)
├── infra/                       Infra-as-code: Kafka topics, ES index templates, healthchecks, Postgres init
├── scripts/                     Lifecycle scripts - see the table in Getting Started
├── libs/
│   └── smartgrid-commons/       Shared Avro schemas, Kafka/outbox helpers, exceptions, security, metrics
├── services/
│   ├── auth-service/            JWT issuance, users, roles
│   ├── order-service/           Order lifecycle + SAGA + transactional outbox
│   ├── vendor-service/          Vendor catalog, ranking, VendorConfirmed publishing
│   ├── inventory-service/       Warehouse stock, reservations
│   ├── pricing-service/         Price rules, quotes
│   ├── shipment-service/        Shipment tracking, carriers, route legs (PostGIS)
│   ├── contract-service/        SLA contracts, vendor agreements, breach definitions
│   ├── notification-service/    Push notifications, WebSocket/STOMP fan-out
│   ├── analytics-service/       Kafka → Elasticsearch aggregation, KPI queries
│   ├── api-gateway/             Single entry point, JWT auth enforcement, routing
│   ├── rag-service/             pgvector ingestion + semantic search
│   ├── mcp-server/              MCP tools the agents call (inventory/order/vendor ops)
│   ├── disruption-detector-agent/
│   ├── reroute-planner-agent/
│   ├── vendor-evaluator-agent/
│   ├── sla-breach-analyst-agent/
│   ├── demand-forecaster-agent/
│   └── contract-negotiation-agent/
├── frontend/                    React + TypeScript dashboard

```

## Getting started

### Prerequisites

| Tool | Version | Needed for |
|---|---|---|
| Docker Desktop (WSL2 backend on Windows) | latest | Layer 0 infra |
| JDK | 21+ | all backend services |
| Maven | 3.9+ | building/running services |
| Node.js | 18+ | frontend |
| [Ollama](https://ollama.com) | latest | local LLM (default) - skip only if you're using OpenAI instead |

### 1. Clone and configure the environment

```powershell
git clone https://github.com/AnkitChavda023/Smart-Grid.git
cd Smart-Grid
Copy-Item .env.example .env
```

`.env.example` ships with **empty** values on purpose (it's the file docker compose reads and it's gitignored) - fill `.env` with these local-dev defaults, they're not secrets:

```
ZOOKEEPER_CLIENT_PORT=2181
ZOOKEEPER_TICK_TIME=2000
KAFKA_BROKER_ID=1
KAFKA_HOST_PORT=9092
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
SCHEMA_REGISTRY_HOST_PORT=18081
SCHEMA_REGISTRY_INTERNAL_PORT=8081
KAFKA_UI_HOST_PORT=18080
KAFKA_CONNECT_HOST_PORT=18083
POSTGRES_HOST_PORT=5432
POSTGRES_USER=smartgrid
POSTGRES_PASSWORD=smartgrid_dev_pw
POSTGRES_DB=smartgrid
REDIS_HOST_PORT=6379
ELASTICSEARCH_HOST_PORT=9200
ELASTICSEARCH_TRANSPORT_PORT=9300
ES_JAVA_OPTS=-Xms512m -Xmx512m
ZIPKIN_HOST_PORT=9411
PROMETHEUS_HOST_PORT=9090
GRAFANA_HOST_PORT=3001
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=admin
OPENAI_API_KEY=
```

Leave `OPENAI_API_KEY` blank - the AI agents default to a local Ollama model and don't need it (see [Models](#models)).

### 2. Start Layer 0 infra

```powershell
.\scripts\up.ps1
```

This does four things in order: `docker compose up -d` → wait for every container to report `healthy` → create all 13 Kafka topics → register Elasticsearch index templates → run a full healthcheck and print a pass/fail table. Don't move on until it prints **"All checks passed."**

| Service | URL | Purpose |
|---|---|---|
| Kafka UI | http://localhost:18080 | Browse topics, partitions, schema registry |
| Schema Registry | http://localhost:18081 | Avro schema contracts |
| Elasticsearch | http://localhost:9200 | Full-text search + analytics index |
| Zipkin | http://localhost:9411 | Distributed tracing |
| Prometheus | http://localhost:9090 | Metrics |
| Grafana | http://localhost:3001 (admin/admin) | Dashboards |
| Postgres | localhost:5432 (smartgrid/smartgrid_dev_pw) | 16 per-service databases in one instance |
| Redis | localhost:6379 | Cache, pub/sub, TTL keys, distributed locks |

### 3. Build the shared library

```powershell
mvn -pl libs/smartgrid-commons -am install
```

Requires Layer 0 infra to already be up - `smartgrid-commons`'s integration test registers a real Avro schema against the Schema Registry from step 2.

### 4. Set up the local LLM (Ollama)

```powershell
ollama pull llama3.1:8b        # chat model, ~4.9GB
ollama pull nomic-embed-text   # embedding model, ~274MB
ollama list                    # confirm both are present
```

Ollama runs as a background service on `localhost:11434` once installed - nothing else to start. Skip this step entirely if you're using OpenAI instead (see [Models](#models)).

### 5. Start all 18 backend services

```powershell
.\scripts\start-all.ps1
```

Opens one PowerShell window per service (`mvn spring-boot:run`, staggered by `-DelaySeconds`, default 8s), waits an extra `-GatewayDelay` (default 20s) before starting `api-gateway` last since it needs the others registered. 18 Spring Boot services cold-starting takes a few minutes - be patient.

Prefer to start (or restart) one service by hand instead, e.g. after a code change:

```powershell
cd services\order-service
mvn spring-boot:run
```

| Service | Port | | Service | Port |
|---|---|---|---|---|
| auth-service | 8180 | | mcp-server | 8095 |
| order-service | 8181 | | rag-service | 8096 |
| vendor-service | 8182 | | disruption-detector-agent | 8085 |
| inventory-service | 8183 | | reroute-planner-agent | 8086 |
| notification-service | 8084 | | vendor-evaluator-agent | 8091 |
| shipment-service | 8087 | | sla-breach-analyst-agent | 8092 |
| contract-service | 8088 | | demand-forecaster-agent | 8093 |
| pricing-service | 8089 | | contract-negotiation-agent | 8094 |
| analytics-service | 8090 | | **api-gateway** | **8000** |

The frontend and any external client should only ever talk to **the gateway on :8000** - it's the only service that enforces JWT auth. Everything else is reachable directly by port too (that's how the seed/demo scripts talk to them), but only the gateway is the real front door.

### 6. Start the frontend

```powershell
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**.

### 7. Log in

Register a new account at `/register` (pick `PLANNER` for the most complete role - see `APPLICATION_GUIDE.md` for the full role table), or use the seed script's account from [Demo](#demo) once you've run it.

### 8. Load demo data (optional but recommended)

```powershell
.\scripts\seed-demo-data.ps1
```

See [Demo](#demo) above for both ways to populate the app.

### 9. Verify everything end-to-end (optional)

```powershell
.\scripts\demo-e2e.ps1
```

A scripted, automated walk through all 18 modules against the real running stack (an Indian automotive JIT supply-chain scenario) - useful as a single command to confirm nothing regressed after a change.

### Shutting down / resetting

| Command | Effect |
|---|---|
| `.\scripts\stop-all.ps1` | Stops all 18 backend services (kills the PIDs it started, then frees any port still held) |
| `.\scripts\clear-database.ps1` | Truncates all 16 Postgres databases + clears Elasticsearch + flushes Redis, but **keeps containers and schemas** - fastest way to get back to a clean, empty state |
| `.\scripts\reset.ps1` | `docker compose down -v` - stops and deletes every container **and volume**, full reset from scratch |

### Troubleshooting

- **`up.ps1`/`start-all.ps1` say a `.ps1` script "is not digitally signed"**: your execution policy is fine but the files are flagged as downloaded from the internet (Windows "Mark of the Web" on a zip-extracted folder). Fix once: `Get-ChildItem . -Recurse -Filter *.ps1 | Unblock-File`.
- **A `.ps1` script refuses to run at all**: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.
- **Services fail on startup with `FATAL: sorry, too many clients already`**: Postgres's default `max_connections` (100) is too low for 18 services' connection pools - already raised to 300 in this repo's `docker-compose.yml`; if you changed that file, keep that line.
- **Kafka fails right after a Docker Desktop restart** (`KeeperException$NodeExistsException`): a stale broker registration in ZooKeeper. Run `docker start smartgrid-kafka`, wait ~15s, then re-run `.\scripts\up.ps1`.
- **`java`/`mvn`/`docker` "not recognized" in a newly opened terminal**: Windows PATH changes don't reach already-open terminal tabs - open a genuinely new terminal, or add the tool's `bin` directory to a PowerShell `$PROFILE` script so it's set on every new session.

## Models

The 6 AI agents and `rag-service` default to a **local Ollama model** - zero cloud dependency, zero cost, works offline:

| Use | Model | Notes |
|---|---|---|
| Chat / reasoning | `llama3.1:8b` | ~4.9GB; CPU inference can take up to a few minutes per call without a GPU |
| Embeddings | `nomic-embed-text` | 768-dim; `rag_service`'s `document_chunks.embedding` column is sized for this |

To use OpenAI instead, set these three environment variables before starting an agent or `rag-service` - no code or config file changes needed:

```
OPENAI_API_KEY=sk-...
LLM_BASE_URL=https://api.openai.com/v1
LLM_MODEL=gpt-4o-mini
```

(`rag-service` additionally reads `EMBEDDING_MODEL`/`EMBEDDING_DIMENSIONS` - switching embedding models requires re-ingesting, since the vector column's dimension is fixed at schema level.)

