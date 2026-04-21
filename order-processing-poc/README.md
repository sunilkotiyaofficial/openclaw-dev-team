# Event-Driven Order Processing System

Production-grade Saga pattern POC — Java 21 + Spring Boot 3.3 + Kafka 3.7 KRaft + MongoDB + React 18.

## Architecture

4 microservices coordinated by choreography-based Saga over Kafka:
- **order-service** (port 8081) — Owns saga state machine
- **payment-service** (port 8082) — Mock payment gateway
- **inventory-service** (port 8083) — Stock reservations with optimistic locking
- **shipping-service** (port 8084) — Mock carrier assignment

## Quick Start

```bash
git clone <repo>
cd order-processing-poc
cp .env.example .env
docker-compose up --build
```

Services available:
- Frontend Dashboard: http://localhost:3000
- Order Service API: http://localhost:8081/actuator/health
- Kafdrop (Kafka UI): http://localhost:9000

## Test the happy path

```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust_001",
    "items": [{"skuId": "SKU-001", "quantity": 2, "unitPrice": 29.99}],
    "shippingAddress": {"street": "123 Main St", "city": "Austin", "state": "TX", "zip": "78701", "country": "US"},
    "paymentMethodId": "pm_mock_success"
  }'
```

Then watch the React dashboard at http://localhost:3000 animate through:
PENDING → PAYMENT_PROCESSING → INVENTORY_RESERVING → SHIPPING_SCHEDULED → COMPLETED

## Test failure path

Use `"paymentMethodId": "pm_mock_fail"` to trigger PaymentFailed → CANCELLED compensation.

## Project Structure

```
├── common-events/       Shared event envelope + 8 payload records
├── common-kafka/        Kafka producer/consumer config + IdempotentEventHandler
├── kafka-config/        Topic definitions + consumer group constants
├── order-service/       Order API + saga state machine + SSE stream
├── payment-service/     Mock payment gateway + PaymentCompleted/Failed events
├── inventory-service/   Stock reservations + InventoryReserved/Depleted events
├── shipping-service/    Mock carrier + ShippingScheduled event
├── frontend/            React 18 dashboard (Vite + TanStack Query + Tailwind)
├── docker-compose.yml   Full local dev stack (Kafka + 4x MongoDB + 4 services + UI)
└── .github/workflows/   GitHub Actions CI (test → build → push to GHCR)
```

## Design Decisions

- **Choreography Saga** (not orchestration) — 4 services is ideal for choreography; no SPOF coordinator
- **Partitioned by orderId** — all Kafka topics use orderId as partition key, guaranteeing per-order event ordering
- **One MongoDB per service** — strict bounded context isolation, no cross-service DB joins
- **Idempotent consumers** — every handler checks eventId before processing to handle re-delivery safely
- **Optimistic locking** — inventory @Version field prevents overselling under concurrency
- **Mock externals** — payment gateway and carrier are mocked; replace in Phase 2

## Phase Roadmap

| Phase | Scope |
|-------|-------|
| 1 (Now) | Happy path POC — 4 services, mock externals, Docker Compose |
| 2 (Weeks 2-3) | Compensation paths, Outbox pattern, circuit breakers, Resilience4j |
| 3 (Month 2+) | GKE, GitHub Actions deploy, Prometheus+Grafana, Gatling load tests |
