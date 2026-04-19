# Design Doc Template

**Purpose:** Every agent in this project uses this 12-section template when generating a deep-dive design document. Consistent structure means you can scan any topic doc and know exactly where to find trade-offs, failure handling, or interview questions.

**When to use:** Triggered by phrases like `deep dive`, `explain`, `teach me`, `design doc`, `write up` in Slack.

**Output location:** `workspace-{agent}/docs/{YYYY-MM-DD}-{topic-slug}.md`

**Length target:** Medium (~2000-3000 words). Detailed enough for interview prep, concise enough to re-read in 10 minutes.

---

# TEMPLATE START — everything below is what the agent generates

---

# {Topic Title}

**Date:** {YYYY-MM-DD}
**Agent:** {agent-name}
**Topic slug:** {topic-slug}
**Level:** Senior Developer / Architect
**Related code:** `workspace-{agent}/code/{YYYY-MM-DD}-{topic-slug}-demo/`
**Related tests:** see code project's `src/test/`
**Related Postman:** `workspace-{agent}/code/{YYYY-MM-DD}-{topic-slug}-demo/postman/`
**PR:** {PR URL once created}

---

## 1. TL;DR

{2-3 sentences. If the reader could only remember this much, these are the facts. Optimized for interview recall — state the pattern, its core mechanism, and its primary use case.}

**Good TL;DR example (for Saga Pattern):**
> Saga pattern coordinates distributed transactions across microservices without 2PC (two-phase commit). Each step has a compensating action that undoes it if a later step fails. Use choreography for simple flows (<5 services), orchestration for complex ones.

---

## 2. Problem It Solves

### The situation without this pattern
{Concrete scenario. What goes wrong? What data inconsistency or failure mode arises when you DON'T use this?}

### When you need this
- {Trigger condition 1 — be specific}
- {Trigger condition 2}
- {Trigger condition 3}

### When you DON'T need this (anti-pattern warning)
- {When a simpler solution suffices}
- {Common over-engineering trap to avoid}

---

## 3. Design Approach

### Core idea
{1-2 paragraphs explaining the fundamental mechanism in plain language.}

### Architecture diagram

```mermaid
{Agent generates a Mermaid flowchart, sequence, or class diagram here.
Copy the text and paste into https://mermaid.live to visualize,
or install `brew install mermaid-cli` to render to PNG locally.}
```

### Key components
- **{Component 1}**: {its role and responsibility}
- **{Component 2}**: {its role}
- **{Component 3}**: {its role}

### Data / control flow
1. {Step 1}
2. {Step 2}
3. {Step 3}
4. {...}

---

## 4. Implementation (Code)

**Full runnable project:** `workspace-{agent}/code/{YYYY-MM-DD}-{topic-slug}-demo/`
Run with: `docker-compose up` (see project README for details)

### Key snippet 1: {what this snippet demonstrates}

```{language}
// {One-line explanation}
{~20-40 line code sample showing the core pattern}
```

### Key snippet 2: {what this demonstrates}

```{language}
// {comment}
{another snippet — focus on what's different from "basic" code}
```

### Key snippet 3 (optional): {what this demonstrates}

```{language}
// {comment}
{snippet}
```

### Critical configuration

{Show the non-obvious YAML/properties/env settings that make this pattern work. Example:}

```yaml
spring:
  kafka:
    consumer:
      enable-auto-commit: false
      isolation-level: read_committed
    producer:
      transactional-id-prefix: orders-tx-
      acks: all
```

---

## 5. Trade-offs

### Pros (why choose this)
- ✅ **{Benefit 1}**: {specific advantage — avoid generic "it's scalable"}
- ✅ **{Benefit 2}**: {specific advantage}
- ✅ **{Benefit 3}**: {specific advantage}

### Cons (what you give up)
- ❌ **{Drawback 1}**: {honest limitation — interviewers love this honesty}
- ❌ **{Drawback 2}**: {operational complexity, cognitive load, etc.}
- ❌ **{Drawback 3}**: {cost, latency, consistency trade-off}

### Alternatives you could've picked

| Alternative | Best fit when | Why not here |
|---|---|---|
| {Alt 1} | {specific situation} | {reason} |
| {Alt 2} | {specific situation} | {reason} |
| {Alt 3} | {specific situation} | {reason} |

---

## 6. Failure Handling

### What can go wrong
1. **{Failure mode 1}**: {symptom visible to user/operator} ← caused by {root cause}
2. **{Failure mode 2}**: {symptom} ← caused by {root cause}
3. **{Failure mode 3}**: {symptom} ← caused by {root cause}

### How to detect
- **Metrics to monitor**: {specific metrics — e.g., `kafka_consumer_lag`, `circuit_breaker_state`}
- **Alerts to set**: {threshold-based alerts to wire up}
- **Log patterns**: {what error logs to grep for}

### How to recover
- {Recovery action 1 — manual or automated}
- {Recovery action 2}
- {When to page the on-call}
- **Runbook link**: {if applicable, or mark TODO}

### Chaos test scenarios
{3 failure scenarios a chaos engineer would throw at this. Good for interview system design.}

---

## 7. Limitations

{What this pattern does NOT solve. Common misconceptions. Hard scaling limits.}

- **{Limitation 1}**: {honest statement}
- **{Limitation 2}**: {where the pattern breaks down}
- **{Limitation 3}**: {boundary condition}

---

## 8. Scope

### In scope for this pattern
- {Responsibility 1}
- {Responsibility 2}
- {Responsibility 3}

### Out of scope
- {What this pattern won't handle — use X instead}
- {Another orthogonal concern}

### Where it fits in the bigger architecture
{1 paragraph positioning this pattern relative to other patterns — e.g., "Saga handles distributed transaction coordination; use CQRS for read/write separation and Event Sourcing for auditability. They compose cleanly."}

---

## 9. Benefits (Interview-Ready Bullet Points)

**These are one-liners you can recall under pressure. Each = 1 sentence, quotable, memorable.**

1. **{Benefit name}** — {one-line claim with a concrete reason}
2. **{Benefit name}** — {one-liner}
3. **{Benefit name}** — {one-liner}
4. **{Benefit name}** — {one-liner}
5. **{Benefit name}** — {one-liner}

**Example style (for Saga):**
1. **Avoids 2PC complexity** — No distributed transaction coordinator needed; each service owns its local transaction.
2. **Scales horizontally** — Event-driven choreography adds services without central coordination changes.
3. **Observable failures** — Compensating actions are explicit events, visible in audit logs.

---

## 10. Phase 1 vs Phase 2

### Phase 1 — MVP (ship this first)
**Scope:** {minimum viable implementation}
- {Feature 1 — must have}
- {Feature 2 — must have}
- {Feature 3 — must have}

**Acceptance criteria:**
- {Testable outcome 1}
- {Testable outcome 2}

**Estimated effort:** {sprints or weeks}

### Phase 2 — Scale (add when growing past initial threshold)
**Trigger:** {concrete metric threshold — e.g., "when TPS > 500"}

- {Enhancement 1}
- {Enhancement 2}
- {Enhancement 3}

### Phase 3 — Advanced (only if needed)
- {Optional future work}

---

## 11. Interview Questions This Unlocks

**10 questions an interviewer could ask after reading this doc. Practice articulating answers out loud.**

1. {Conceptual question — "Explain the pattern"}
2. {Trade-off question — "When would you NOT use this?"}
3. {Failure question — "What happens if step N fails after step N-1 committed?"}
4. {Scaling question — "How does this behave under 10x load?"}
5. {Alternative question — "Why this over X?"}
6. {Integration question — "How does this interact with pattern Y?"}
7. {Experience question — "Tell me about a time you implemented/debugged this"}
8. {Coding question — "Write the code for step N"}
9. {System design question — "Design a system that uses this for use case Z"}
10. {Gotcha question — "What's the edge case that trips up most implementations?"}

---

## 12. Further Reading

### Official docs
- {Link 1}
- {Link 2}

### Recommended books / chapters
- {Book title, chapter or page range}
- {Another book}

### Blog posts / conference talks
- {Authoritative blog post}
- {Conference talk — include speaker + year}

### Related docs in this library
- [`workspace-{agent}/docs/{date}-{related-topic}.md`]({relative-path})
- {Topic that pairs well with this one}

---

**Doc version:** 1.0
**Last updated:** {YYYY-MM-DD}
**Quiz yourself:** DM `@orchestrator quiz me on {topic-slug}` in Slack
**Feedback:** If this doc is incomplete, comment in the PR or open an issue.

---

# TEMPLATE END

---

## Notes for Agents Filling This Template

1. **Never leave `{placeholders}` in the final output** — either fill them or delete the line.
2. **Mermaid diagrams must be valid** — test your diagram text in https://mermaid.live before posting.
3. **Code snippets must be runnable** — the full project in `code/` must actually work with `docker-compose up`.
4. **Interview questions must be specific** — avoid generic "What is X?" questions; ask the hard edge cases.
5. **Phase 1 must be minimal** — resist gold-plating; if Phase 1 has >5 bullets, split some to Phase 2.
6. **Keep TL;DR to 3 sentences max** — this is the section users will re-read most.
7. **Length target: 2000-3000 words total** — longer = less likely to be re-read.
