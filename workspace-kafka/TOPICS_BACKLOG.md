# 🌀 Kafka — Topics Backlog

Interview study roadmap for Kafka, event streaming, and EDA patterns.

**How to use:**
- Topics ordered by priority (P0 = must-know for senior/architect interviews)
- Trigger a deep dive in Slack: `@kafka deep dive on {topic}`
- Check off when you can confidently explain + code + handle follow-up questions
- After a deep dive, each topic becomes a PR + design doc + runnable code

**Progress tracking:** Update the Status column when you've mastered a topic.

---

## P0 — Must Know (Senior/Architect Interviews Will Test These)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 1 | **Saga Pattern** (choreography + orchestration) | 🔥 Advanced | ⬜ Not started | Distributed transactions without 2PC. Core interview topic. |
| 2 | **Exactly-Once Semantics** | 🔥 Advanced | ⬜ Not started | Producer idempotence + transactions + `read_committed`. |
| 3 | **Partition Strategy & Key Design** | 🔸 Intermediate | ⬜ Not started | Hot partitions, rebalancing impact, key selection. |
| 4 | **Consumer Groups & Rebalancing** | 🔸 Intermediate | ⬜ Not started | Eager vs cooperative sticky, rebalance storms. |
| 5 | **CQRS with Kafka** | 🔥 Advanced | ⬜ Not started | Command-query separation using Kafka as the log. |
| 6 | **Event Sourcing** | 🔥 Advanced | ⬜ Not started | Kafka as immutable event log, replay semantics. |
| 7 | **Outbox Pattern / Transactional Outbox** | 🔸 Intermediate | ⬜ Not started | Reliable event publishing from DB. Debezium CDC. |

## P1 — Should Know (Common Questions)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 8 | **Schema Registry** (Avro/Protobuf) | 🔸 Intermediate | ⬜ Not started | Forward/backward compatibility, schema evolution. |
| 9 | **Dead Letter Queue & Retry Topics** | 🔹 Beginner | ⬜ Not started | Failure handling patterns. |
| 10 | **Kafka Streams** (KStream, KTable, windowing) | 🔥 Advanced | ⬜ Not started | Stateful stream processing. |
| 11 | **Consumer Lag Monitoring** | 🔹 Beginner | ⬜ Not started | Burrow, JMX metrics, alerting strategies. |
| 12 | **Backpressure in Consumers** | 🔸 Intermediate | ⬜ Not started | `max.poll.records`, pause/resume, bounded queues. |
| 13 | **Broker Sizing & Capacity Planning** | 🔸 Intermediate | ⬜ Not started | Partition count math, disk throughput, network. |

## P2 — Nice to Have (Shows Depth)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 14 | **KRaft Mode** (ZooKeeper-less) | 🔸 Intermediate | ⬜ Not started | Modern Kafka, controller quorum. |
| 15 | **MirrorMaker 2** (multi-cluster replication) | 🔥 Advanced | ⬜ Not started | Active-passive, active-active patterns. |
| 16 | **ksqlDB** | 🔸 Intermediate | ⬜ Not started | SQL over Kafka Streams. |
| 17 | **Tiered Storage** | 🔸 Intermediate | ⬜ Not started | Confluent Cloud, KIP-405. |
| 18 | **Log Compaction** | 🔸 Intermediate | ⬜ Not started | When and why to use vs retention by time. |
| 19 | **Producer Interceptors** (audit, PII scrubbing) | 🔹 Beginner | ⬜ Not started | Cross-cutting concerns. |
| 20 | **ACLs & Kafka Security** | 🔸 Intermediate | ⬜ Not started | SASL, SSL, ACL patterns. |

---

## Suggested Learning Path

**Week 1 — Foundations:** #3, #4, #11, #14 (partition model, consumer groups, monitoring, KRaft)
**Week 2 — Reliability:** #2, #9, #12 (exactly-once, DLQ, backpressure)
**Week 3 — Patterns:** #1, #7, #5, #6 (saga, outbox, CQRS, event sourcing)
**Week 4 — Advanced:** #10, #8, #15 (streams, schema registry, mirror maker)
**Ongoing:** P2 topics as interest dictates

---

## Status Legend

- ⬜ **Not started** — haven't opened a deep dive yet
- 🔨 **In progress** — doc generated, still studying
- 📝 **Studied** — read the doc, ran the code
- 🎯 **Quiz-ready** — can articulate under interview pressure
- ✅ **Mastered** — passed the quiz mode 3 times without gaps

---

**Last updated:** 2026-04-18
**Total topics:** 20 (7 P0 + 6 P1 + 7 P2)
**Mastered:** 0 / 20
