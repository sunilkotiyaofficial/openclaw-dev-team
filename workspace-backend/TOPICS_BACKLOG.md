# ☕ Backend — Topics Backlog

Interview study roadmap for Java / Spring Boot / distributed systems backend.

**How to use:**
- Topics ordered by priority (P0 = must-know for senior/architect interviews)
- Trigger a deep dive in Slack: `@backend deep dive on {topic}`
- Check off when you can confidently explain + code + handle follow-up questions
- After a deep dive, each topic becomes a PR + design doc + runnable code

---

## P0 — Must Know (Senior/Architect Interviews Will Test These)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 1 | **Circuit Breaker (Resilience4j)** | 🔸 Intermediate | ⬜ Not started | States (closed/open/half-open), thresholds, fallbacks. |
| 2 | **Idempotency Patterns** | 🔸 Intermediate | ⬜ Not started | Idempotency keys, retry safety, conflict detection. |
| 3 | **Virtual Threads (Java 21)** | 🔸 Intermediate | ⬜ Not started | When to use, pinning pitfalls, vs platform threads. |
| 4 | **Spring WebFlux / Reactive Streams** | 🔥 Advanced | ⬜ Not started | Mono/Flux, backpressure, when NOT to use. |
| 5 | **JWT & OAuth2 with Spring Security** | 🔸 Intermediate | ⬜ Not started | Token flow, refresh, revocation, stateless vs stateful. |
| 6 | **Strangler Fig Migration** | 🔸 Intermediate | ⬜ Not started | Your Papa John's story — have a concrete example ready. |
| 7 | **Transactional Patterns in Microservices** | 🔥 Advanced | ⬜ Not started | Saga, TCC, outbox — when to use which. |

## P1 — Should Know (Common Questions)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 8 | **Distributed Tracing (OpenTelemetry)** | 🔸 Intermediate | ⬜ Not started | Spans, context propagation, W3C Trace Context. |
| 9 | **Rate Limiting (Bucket4j, token bucket)** | 🔹 Beginner | ⬜ Not started | Per-user, per-IP, per-API patterns. |
| 10 | **Spring Data MongoDB Patterns** | 🔸 Intermediate | ⬜ Not started | Reactive driver, aggregation pipelines, optimistic locking. |
| 11 | **Concurrency Patterns in Java** | 🔥 Advanced | ⬜ Not started | CompletableFuture, virtual threads, parallel streams pitfalls. |
| 12 | **Java Records, Sealed Classes, Pattern Matching** | 🔹 Beginner | ⬜ Not started | Java 21 features — senior interviewers expect fluency. |
| 13 | **Bulkhead Pattern** | 🔹 Beginner | ⬜ Not started | Isolating failures, thread pool separation. |
| 14 | **Caching Strategy** | 🔸 Intermediate | ⬜ Not started | Cache-aside, write-through, Caffeine vs Redis. |

## P2 — Nice to Have (Shows Depth)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 15 | **Hexagonal Architecture (Ports & Adapters)** | 🔸 Intermediate | ⬜ Not started | Clean separation of domain from infrastructure. |
| 16 | **Domain-Driven Design** (aggregate, bounded context) | 🔥 Advanced | ⬜ Not started | Strategic + tactical patterns. |
| 17 | **Spring Cloud Gateway / API Gateway Patterns** | 🔸 Intermediate | ⬜ Not started | Routing, filters, rate limiting at gateway level. |
| 18 | **gRPC with Spring Boot** | 🔸 Intermediate | ⬜ Not started | When to pick over REST. |
| 19 | **GraphQL with Spring** | 🔸 Intermediate | ⬜ Not started | N+1 problem, dataloader, federation. |
| 20 | **Kotlin Coroutines + Spring** | 🔸 Intermediate | ⬜ Not started | Flow vs Reactor, structured concurrency. |

---

## Suggested Learning Path

**Week 1 — Reliability:** #1, #2, #13 (circuit breaker, idempotency, bulkhead)
**Week 2 — Java 21:** #3, #11, #12 (virtual threads, concurrency, records)
**Week 3 — Security & Scale:** #5, #9, #14 (auth, rate limiting, caching)
**Week 4 — Architecture:** #7, #15, #16 (transactions, hexagonal, DDD)
**Week 5 — Reactive:** #4, #8 (WebFlux, tracing)
**Ongoing:** #6 (Strangler Fig — tie to Papa John's story), P2 as interest dictates

---

## Cross-References

- **#1 Circuit Breaker** pairs with `devops` → observability/alerting
- **#7 Transactional Patterns** pairs with `kafka` → Saga + Outbox
- **#5 JWT/OAuth2** pairs with `devops` → Workload Identity, secret management
- **#8 Distributed Tracing** pairs with `devops` → Cloud Trace / Jaeger

---

## Status Legend

- ⬜ Not started
- 🔨 In progress
- 📝 Studied
- 🎯 Quiz-ready
- ✅ Mastered

---

**Last updated:** 2026-04-18
**Total topics:** 20 (7 P0 + 7 P1 + 6 P2)
**Mastered:** 0 / 20
