# 🧪 QA — Topics Backlog

Interview study roadmap for testing strategy, quality gates, and test engineering.

**How to use:**
- Topics ordered by priority (P0 = must-know for senior interviews)
- Trigger a deep dive in Slack: `@qa deep dive on {topic}`
- Check off when you can confidently design + implement + defend the approach

---

## P0 — Must Know (Senior Interviews Will Test These)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 1 | **Testcontainers Strategy** (no H2, no in-memory) | 🔸 Intermediate | ⬜ Not started | Real DB, real Kafka, real Redis. Why H2 lies. |
| 2 | **Test Pyramid in Microservices** | 🔸 Intermediate | ⬜ Not started | Unit/integration/contract/E2E proportions. |
| 3 | **Contract Testing** (Pact, Spring Cloud Contract) | 🔥 Advanced | ⬜ Not started | Consumer-driven vs provider-driven. |
| 4 | **TDD vs BDD** (when each applies) | 🔹 Beginner | ⬜ Not started | Red-green-refactor, Gherkin, who writes what. |
| 5 | **Integration Test Patterns** | 🔸 Intermediate | ⬜ Not started | AAA pattern, Awaitility for async, no Thread.sleep. |
| 6 | **Mutation Testing** (PIT) | 🔥 Advanced | ⬜ Not started | Test quality vs coverage, mutation score. |

## P1 — Should Know (Common Questions)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 7 | **Performance Testing** (Gatling, k6) | 🔸 Intermediate | ⬜ Not started | Load/stress/soak/spike test scenarios. |
| 8 | **Property-Based Testing** (jqwik, junit-quickcheck) | 🔥 Advanced | ⬜ Not started | Generative testing, shrinking. |
| 9 | **Chaos Engineering Basics** | 🔥 Advanced | ⬜ Not started | Failure injection at test time vs prod. |
| 10 | **Code Review Standards** (9-point Java checklist) | 🔹 Beginner | ⬜ Not started | Your review rubric — need to articulate it. |
| 11 | **E2E Testing with Playwright** | 🔸 Intermediate | ⬜ Not started | Auto-waiting, trace viewer, fixtures. |
| 12 | **Test Data Management** | 🔸 Intermediate | ⬜ Not started | Fixtures, factories, cleanup strategies. |

## P2 — Nice to Have (Shows Depth)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 13 | **Snapshot Testing** (pros and cons) | 🔹 Beginner | ⬜ Not started | When it helps, when it hurts. |
| 14 | **Flaky Test Strategies** | 🔸 Intermediate | ⬜ Not started | Quarantine, retry budgets, flakiness dashboards. |
| 15 | **Approval Testing (Golden Master)** | 🔸 Intermediate | ⬜ Not started | For legacy code without clear assertions. |
| 16 | **API Contract Testing at Scale** | 🔥 Advanced | ⬜ Not started | Broker, compatibility matrix, CI gating. |

---

## Suggested Learning Path

**Week 1 — Foundations:** #1, #2, #4, #5 (Testcontainers, pyramid, TDD/BDD, integration patterns)
**Week 2 — Quality Gates:** #6, #10, #14 (mutation, reviews, flakiness)
**Week 3 — Contract & Perf:** #3, #7 (Pact, Gatling)
**Week 4 — Advanced:** #8, #9, #11 (property-based, chaos, Playwright)
**Ongoing:** P2 as interest dictates

---

## Cross-References

- **#1 Testcontainers** pairs with `backend` → Spring Boot test configuration
- **#3 Contract Testing** pairs with `backend` + `frontend` → API versioning
- **#7 Performance Testing** pairs with `devops` → load testing in CI, capacity planning
- **#9 Chaos Engineering** pairs with `devops` → game days, resilience validation

---

## Status Legend

- ⬜ Not started
- 🔨 In progress
- 📝 Studied
- 🎯 Quiz-ready
- ✅ Mastered

---

**Last updated:** 2026-04-18
**Total topics:** 16 (6 P0 + 6 P1 + 4 P2)
**Mastered:** 0 / 16
