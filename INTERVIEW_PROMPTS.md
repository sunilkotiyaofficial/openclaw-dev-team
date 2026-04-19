# 🎯 Interview Prep Prompt Library

Your daily driver for interview preparation using the OpenClaw multi-agent system.

**How to use:** Copy any prompt below → paste into Slack DM with OpenClawBot → replace `{placeholders}` with your topic → send. All responses arrive in the same Slack thread.

**Primary model:** Claude Sonnet 4.6 (configured as default)
**Fallbacks:** Claude Haiku → Gemini Flash → Ollama gemma4:e4b

---

## Table of Contents

- [🤖 Agent Routing Cheat Sheet](#-agent-routing-cheat-sheet)
- [Prompt Patterns](#prompt-patterns)
  - [Pattern 1: Quick Brief](#-pattern-1-quick-brief)
  - [Pattern 2: Deep Dive](#-pattern-2-deep-dive)
  - [Pattern 3: Runnable Code](#-pattern-3-runnable-code)
  - [Pattern 4: Mock Interview](#-pattern-4-mock-interview)
  - [Pattern 5: Trade-off Matrix](#️-pattern-5-trade-off-matrix)
  - [Pattern 6: Cheat Sheet](#-pattern-6-cheat-sheet)
  - [Pattern 7: System Design](#-pattern-7-system-design-walkthrough)
  - [Pattern 8: Behavioral + STAR](#-pattern-8-behavioral--star-framing)
  - [Pattern 9: Gap Finder](#-pattern-9-why-probe-find-gaps)
  - [Pattern 10: Daily Sprint](#-pattern-10-daily-sprint)
- [📅 Daily Workflow](#-daily-workflow)
- [🎁 Pro Tips](#-pro-tips)
- [📚 Ready-Made Prompt Collections](#-ready-made-prompt-collections-by-topic)

---

## 🤖 Agent Routing Cheat Sheet

| I want to learn about... | Ask this agent |
|---|---|
| Java 8+ / Spring Boot / OOP / concurrency / Virtual Threads | `@backend` |
| Microservices patterns (Circuit Breaker, Outbox, Idempotency) | `@backend` |
| MongoDB / reactive data / persistence | `@backend` |
| Kafka / EDA / Saga / CQRS / Event Sourcing | `@kafka` |
| Kafka Streams / ksqlDB / stream processing | `@kafka` |
| GCP / AWS / Azure / GKE / Cloud Run | `@devops` |
| Terraform / CI/CD / Docker / K8s manifests | `@devops` |
| Testing strategy / Testcontainers / Gatling / mutation testing | `@qa` |
| React / TypeScript / Vite / state management | `@frontend` |
| LLM architecture / RAG / vector DBs / embeddings | `@aiml` |
| Agent frameworks / LangGraph / fine-tuning | `@aiml` |
| System design / distributed systems / behavioral | `@orchestrator` |
| Algorithms / DSA (once agent added) | `@dsa` |

---

## Prompt Patterns

### 🎯 Pattern 1: Quick Brief

**Use for:** daily refresher, commute reading, warm-up before deeper study
**Response time:** ~2-4 seconds (Sonnet)
**Token cost:** ~$0.01

```
@{agent} in exactly 3 sentences, explain {topic} at senior developer level
```

**Examples:**
- `@kafka in exactly 3 sentences, explain why exactly-once semantics requires transactions`
- `@backend in exactly 3 sentences, explain Virtual Threads vs Platform Threads in Java 21`
- `@backend in exactly 3 sentences, explain the Circuit Breaker pattern's three states`
- `@aiml in exactly 3 sentences, explain why RAG often beats fine-tuning`
- `@devops in exactly 3 sentences, explain Workload Identity vs service account keys`
- `@frontend in exactly 3 sentences, explain React Server Components vs Client Components`
- `@qa in exactly 3 sentences, explain why H2 is a bad choice for integration tests`

---

### 📖 Pattern 2: Deep Dive

**Use for:** weekend study, mastering a hard topic
**Response time:** ~30-60 seconds
**Token cost:** ~$0.30-0.50

```
@{agent} generate a complete design doc on "{topic}" following all 12 sections of /DESIGN_DOC_TEMPLATE.md at project root. Include:
- TL;DR (3 sentences)
- Problem it solves
- Design approach with Mermaid diagram
- Implementation with Java/Spring code snippets
- Trade-offs (pros, cons, alternatives)
- Failure handling (failure modes, detection, recovery)
- Limitations
- Scope (in/out)
- Benefits (5 interview-ready one-liners)
- Phase 1 vs Phase 2 planning
- 10 interview questions this doc unlocks
- Further reading

Length target: 2000-3000 words. Post full content in this thread — I'll save to workspace.
```

**Examples:**
- `@kafka generate a complete design doc on "Saga Pattern: Choreography vs Orchestration"...`
- `@backend generate a complete design doc on "Circuit Breaker Pattern with Resilience4j"...`
- `@backend generate a complete design doc on "Transactional Outbox Pattern with Debezium"...`
- `@aiml generate a complete design doc on "Production RAG Architecture"...`
- `@devops generate a complete design doc on "GKE Autoscaling: HPA + VPA + Cluster Autoscaler"...`
- `@frontend generate a complete design doc on "React Server Components in production"...`

**Follow-up workflow:**
1. Copy the full response content
2. Save to `workspace-{agent}/docs/{YYYY-MM-DD}-{topic-slug}.md`
3. `git add workspace-{agent}/docs/ && git commit -m "docs({agent}): add {topic} deep dive" && git push`

---

### 💻 Pattern 3: Runnable Code

**Use for:** seeing how theory translates to real Java/Spring/React code
**Response time:** ~15-30 seconds

```
@{agent} write production-grade code for {topic} in {language/framework}. Include:
- Complete class with all imports
- @Configuration setup (if Spring)
- Error handling (no swallowed exceptions)
- A matching @Test using Testcontainers (Java) or Vitest (React)

Format in ```java (or ```typescript) blocks. Explain non-obvious decisions inline with comments. Keep classes under 100 lines each.
```

**Examples:**
- `@backend write production-grade code for JWT authentication with refresh token rotation using Spring Security 6.3...`
- `@backend write production-grade code for an idempotent POST /orders endpoint using Redis for idempotency keys...`
- `@kafka write production-grade code for a transactional Kafka producer using outbox pattern with MongoDB...`
- `@backend write production-grade code for a saga orchestrator managing order → payment → inventory → shipping steps with compensating actions...`
- `@frontend write production-grade code for a form with React Hook Form + Zod validation + error handling...`

---

### 🎤 Pattern 4: Mock Interview

**Use for:** articulation practice, finding gaps in your knowledge
**Response time:** Interactive — back-and-forth

```
@{agent} mock interview me on {topic} at senior architect level. Ask ONE hard question, wait for my answer, then:
1. Evaluate against senior expectations (correct? complete? articulate?)
2. Flag any gaps
3. Coach stronger phrasing
4. Ask a harder follow-up

Don't give me the answer until I've attempted. Keep going until I say "stop" or "summarize". At the end, tell me my score 1-10 and which sub-topics to re-study.
```

**Examples:**
- `@orchestrator mock interview me on distributed systems fundamentals at senior architect level...`
- `@kafka mock interview me on EDA patterns at senior architect level. Topics to cover: Saga, CQRS, Event Sourcing, Outbox, Exactly-Once...`
- `@backend mock interview me on Java concurrency. Topics: Virtual Threads, synchronized vs ReentrantLock, ExecutorService patterns, CompletableFuture...`
- `@aiml mock interview me on AI architecture for senior roles. Topics: RAG design, vector DB selection, LLM cost optimization...`
- `@devops mock interview me on GCP infrastructure. Topics: Workload Identity, GKE autoscaling, private clusters, VPC Service Controls...`

---

### ⚖️ Pattern 5: Trade-off Matrix

**Use for:** "when would you pick X vs Y" interview questions

```
@{agent} create a trade-off matrix comparing {X} vs {Y} vs {Z}.

Columns:
- Use Case
- Latency
- Cost ($)
- Complexity
- Operational Overhead
- When to Choose
- Red Flags (when to reject)

Format as a clean markdown table. Add a concluding paragraph: "My default pick when starting a new project, and when I'd deviate."
```

**Examples:**
- `@backend create a trade-off matrix comparing PostgreSQL vs MongoDB vs Cassandra`
- `@kafka create a trade-off matrix comparing Kafka vs RabbitMQ vs AWS SQS vs Google Pub/Sub`
- `@devops create a trade-off matrix comparing Kubernetes (GKE) vs Cloud Run vs AWS Lambda vs Azure Container Apps`
- `@aiml create a trade-off matrix comparing LangChain vs LangGraph vs CrewAI vs Claude Agent SDK`
- `@backend create a trade-off matrix comparing Saga Choreography vs Saga Orchestration vs 2PC vs Eventual Consistency only`
- `@frontend create a trade-off matrix comparing Zustand vs Redux Toolkit vs Jotai vs Context API`

---

### 📋 Pattern 6: Cheat Sheet

**Use for:** night before interview, subway ride to interview, last-minute recall

```
@{agent} create a one-page cheat sheet for {topic}.

Constraints:
- Max 15 bullet points
- Under 200 words total

Sections:
- What it is (1 line)
- When to use (3 bullets)
- When NOT to use (2 bullets)
- Key configs / magic numbers (3 bullets)
- Common gotchas (3 bullets)
- One-liner for interviewer recall (1 memorable sentence)

Optimize for recall under pressure.
```

**Examples:**
- `@kafka create a one-page cheat sheet for Kafka Streams`
- `@kafka create a one-page cheat sheet for Exactly-Once Semantics`
- `@backend create a one-page cheat sheet for Spring WebFlux`
- `@backend create a one-page cheat sheet for Resilience4j Circuit Breaker`
- `@aiml create a one-page cheat sheet for vector database selection`
- `@aiml create a one-page cheat sheet for RAG chunking strategies`
- `@devops create a one-page cheat sheet for Workload Identity on GKE`

---

### 🏗️ Pattern 7: System Design Walkthrough

**Use for:** senior/staff/architect interview prep

```
@orchestrator design a system for {business requirement}. Scale: {numbers — TPS, data volume, users}. Walk me through:

1. Requirements clarification (5 questions an architect would ask the interviewer)
2. High-level architecture with Mermaid diagram
3. Data models (key entities + relationships)
4. Key APIs (method + path + sample request/response)
5. Scaling strategy (how does this handle 10x load?)
6. Failure modes (what breaks first, how to detect, how to recover)
7. Trade-offs I made (3 explicit decisions + why)
8. "Questions an interviewer would push back on" (3-5)

Length target: 2000 words. Include cost estimate ($/month at stated scale).
```

**Classic System Design Prompts (copy-ready):**
- `@orchestrator design a system for a ride-sharing backend. Scale: 10M riders, 1M active drivers, 100K rides/sec peak, 50 cities...`
- `@orchestrator design a system for a real-time analytics pipeline. Scale: 1M events/sec, 100TB/day, 90-day retention...`
- `@orchestrator design a system for a multi-tenant SaaS CRM. Scale: 1000 tenants, 10K-100K users each, strict data isolation required...`
- `@orchestrator design a system for a video streaming platform. Scale: 50M users, 1M concurrent streams, global delivery...`
- `@orchestrator design a system for an e-commerce order flow (cart → checkout → payment → fulfillment). Scale: Black Friday spike 10x normal, 1M orders/hour peak...`
- `@orchestrator design a system for a payment processor. Scale: 10K TPS, strong consistency required, PCI-DSS compliance, multi-region...`
- `@orchestrator design a system for a feature flag platform (like LaunchDarkly). Scale: 1M clients, 100K flags, <50ms decision latency...`
- `@orchestrator design a system for a real-time collaboration editor (like Google Docs). Scale: 10K concurrent docs, 100 users per doc...`

---

### 🎭 Pattern 8: Behavioral + STAR Framing

**Use for:** "tell me about a time" questions

```
@orchestrator I need to articulate my {experience: Papa John's migration / Kafka implementation / GCP deployment / microservices redesign} as a STAR-format answer for the interview question:

"Tell me about a time you {solved a hard problem / made a significant trade-off / disagreed with a team / owned an outage / scaled a system}."

Process:
1. Ask me 5 probing questions to build the story (situation, constraints, decisions, outcomes, metrics)
2. Help me draft a 90-second version I can memorize
3. Flag any weak spots (missing metrics, vague outcomes, too much detail)
4. Give me 3 follow-up questions an interviewer might ask
```

**Examples:**
- `@orchestrator I need to articulate my Papa John's monolith-to-microservices migration as a STAR answer for "tell me about a time you owned a hard migration"...`
- `@orchestrator I need to articulate my Kafka event-driven redesign as a STAR answer for "tell me about a significant technical decision you made"...`
- `@orchestrator I need to articulate a time I disagreed with a team on architecture. Help me find a real example from my experience through questions...`

---

### 🔬 Pattern 9: "Why" Probe (Find Knowledge Gaps)

**Use for:** testing depth of understanding, identifying weak spots

```
@{agent} I claim to understand {topic}. Ask me 5 progressively harder "why" questions. Don't give answers — just questions. After I attempt all 5:

- Score my understanding 1-10
- Tell me exactly which sub-topics I should re-study
- Recommend 3 next topics to deep dive based on my gaps
```

**Examples:**
- `@kafka I claim to understand Kafka Consumer Groups. Ask me 5 progressively harder "why" questions...`
- `@backend I claim to understand Spring Transactions (@Transactional). Ask me 5 progressively harder "why" questions...`
- `@aiml I claim to understand RAG. Ask me 5 progressively harder "why" questions...`

---

### 🏃 Pattern 10: Daily Sprint (15-min Warm-Up)

**Use for:** morning warm-up during interview prep season

```
@orchestrator for today's interview prep sprint:

1. Pick ONE topic from any workspace-*/TOPICS_BACKLOG.md that I haven't marked mastered yet (prefer P0 topics)
2. Give me:
   - 3-sentence brief
   - The ONE gotcha interviewers love to test on this
   - One killer one-liner I should memorize

Keep total under 100 words. Optimize for a 15-minute study session.
```

---

## 📅 Daily Workflow

### 🌅 Morning (10-15 min) — Warm-Up
```
@orchestrator for today's interview prep sprint...
```
→ Pattern 10. Light morning review.

### ☕ Lunch / Coffee Break (5-10 min) — Quick Wins
```
@{agent} in exactly 3 sentences, explain {weak topic}
```
→ Pattern 1. Do 3-5 per session. Tag weak areas.

### 🌙 Evening (30-60 min) — Main Study
```
@{agent} generate a complete design doc on...
```
→ Pattern 2. Read fully. Copy to `workspace-{agent}/docs/`. Commit to GitHub.

### 🛋️ Weekend (2 hours) — Mock Interviews
```
@orchestrator mock interview me on...
```
→ Pattern 4. Track gaps in `workspace-orchestrator/memory/gaps.md`.

### 🌌 Night Before Interview
```
@{agent} create a one-page cheat sheet for {topic}
```
→ Pattern 6. Save 3-5 cheat sheets. Re-read in bed. Sleep.

---

## 🎁 Pro Tips

### 1. Pin this file in VS Code / IntelliJ
Keep `INTERVIEW_PROMPTS.md` open in a side pane while Slacking. Quick copy-paste access.

### 2. Use thread replies for follow-ups
When a response has something interesting, reply in-thread (Slack's 💬 icon on the message):
```
Elaborate on the "X" point you made — give a concrete example from e-commerce
```
Keeps context. Continues the learning thread.

### 3. Force Opus for the hardest topics
Default is Sonnet (saves budget). For architect-level deep dives:
```
@{agent} using claude-opus model, design a system for...
```
May or may not work depending on OpenClaw routing — try it.

### 4. Save breakthroughs to MEMORY.md
When something finally clicks ("OH so THAT's why exactly-once needs transactions"), write it:
```
echo "- 2026-04-19: Finally understood why Kafka exactly-once needs BOTH producer transactions AND consumer read_committed — they're two halves of the same atomic commit" >> ~/projects/openclaw-dev-team/workspace-kafka/MEMORY.md
```
Future-you will thank you.

### 5. Name your study sessions
Before a study block, declare your goal in Slack:
```
@orchestrator for the next hour I'm studying Kafka. Track this as a "Kafka Deep Dive Session" in my memory/sessions.md. I'll report what I learned at the end.
```
This gives the system context for the session.

### 6. End-of-day reflection
```
@orchestrator summarize what I studied today based on my Slack messages. Update memory/interview-progress.md with today's topics. Flag any topics I struggled with.
```

### 7. Track your weak spots
Keep a running list at `workspace-orchestrator/memory/gaps.md`:
```markdown
# Gaps to Close

- [ ] Kafka transactional semantics at consumer-level (why read_committed is needed)
- [ ] Saga compensation with partial failures (what if compensation itself fails?)
- [ ] Spring WebFlux backpressure handling
- [ ] RAG retrieval evaluation metrics (what's MRR? NDCG?)
```
Ask the agent to quiz you specifically on these regularly.

### 8. Build a "one-liner bank"
Keep `workspace-orchestrator/memory/one-liners.md`:
```markdown
# Killer One-Liners (Memorize These)

- **Kafka partitioning**: "Partition count sets the hard ceiling for consumer parallelism."
- **Circuit breaker**: "Fail fast when the dependency is known-broken, rather than cascading timeouts."
- **Saga**: "Distributed transactions without 2PC — each step owns its local commit and its compensation."
- **RAG**: "Retrieval first, generation second. Don't ask the model what it doesn't know."
```
Review before interviews. These are the quotable moments.

---

## 📚 Ready-Made Prompt Collections by Topic

Copy these entire blocks as session-starters.

### ☕ Java + Spring Boot Crash Course (Week 1)

Day 1 — Monday:
```
@backend in exactly 3 sentences, explain Virtual Threads vs Platform Threads in Java 21
```

Day 2 — Tuesday:
```
@backend generate a complete design doc on "Spring Boot 3.3 Virtual Threads configuration and when to use them"
```

Day 3 — Wednesday:
```
@backend write production-grade code for a @RestController using Virtual Threads with Tomcat in Spring Boot 3.3
```

Day 4 — Thursday:
```
@backend create a one-page cheat sheet for Virtual Threads
```

Day 5 — Friday:
```
@backend mock interview me on Java 21 concurrency at senior developer level
```

### 🌀 Kafka Deep-Dive Path (Week 2)

- Day 1: `@kafka generate a complete design doc on "Consumer Groups and Rebalancing"`
- Day 2: `@kafka generate a complete design doc on "Exactly-Once Semantics"`
- Day 3: `@kafka generate a complete design doc on "Saga Pattern with Kafka"`
- Day 4: `@kafka generate a complete design doc on "Outbox Pattern with Debezium CDC"`
- Day 5: `@kafka mock interview me on EDA patterns at senior architect level`

### ☁️ GCP Architecture Path (Week 3)

- Day 1: `@devops generate a complete design doc on "GKE Autoscaling (HPA + VPA + Cluster Autoscaler)"`
- Day 2: `@devops generate a complete design doc on "Workload Identity vs static service account keys"`
- Day 3: `@devops generate a complete design doc on "Blue-Green vs Canary deployment on GKE"`
- Day 4: `@devops generate a complete design doc on "Multi-region GKE topology"`
- Day 5: `@devops create a trade-off matrix comparing GKE vs Cloud Run vs Cloud Functions`

### 🤖 AI/ML Architect Path (Week 4)

- Day 1: `@aiml generate a complete design doc on "Production RAG architecture"`
- Day 2: `@aiml generate a complete design doc on "Vector database selection framework"`
- Day 3: `@aiml generate a complete design doc on "LLM cost optimization strategies"`
- Day 4: `@aiml generate a complete design doc on "Agent architecture with LangGraph"`
- Day 5: `@aiml mock interview me on AI/ML architecture at senior architect level`

---

## 🎯 Pre-Interview Power Session (Day Of)

Paste this the morning of a big interview:

```
@orchestrator I have an interview today for a Senior Java Developer / Architect role focusing on microservices + Kafka + GCP + AI/ML awareness. Give me:

1. The top 5 questions I'm MOST likely to get asked based on my interview library (scan all workspace-*/docs)
2. For each, a 60-second answer template
3. Three killer one-liners I should work into the conversation
4. Two "questions to ask the interviewer" that make me look thoughtful
5. Final pep talk (30 words)

Format as an interview prep "one-pager" I can re-read right before the call.
```

---

## 🔄 When Things Feel Stale

If your prompt responses feel generic or repetitive:

```
@{agent} your recent responses feel generic. Rewrite your last response but:
- Ground it in real-world scale numbers (10K TPS, 1M users, etc.)
- Include one specific production anecdote you'd tell in an interview
- Add one counter-intuitive insight most developers miss
- Cut the fluff — every sentence must earn its place
```

---

## 📝 Updating This File

This is a living document. Add your own prompts that work well. Remove ones that don't.

**To update:**
```bash
cd ~/projects/openclaw-dev-team
code INTERVIEW_PROMPTS.md   # or nano / vim
git add INTERVIEW_PROMPTS.md
git commit -m "docs: update interview prompts with new pattern X"
git push
```

---

**File version:** 1.0
**Created:** 2026-04-19
**Primary model:** Claude Sonnet 4.6
**Total patterns:** 10 | **Ready-made prompts:** 60+

Good luck, Sunil. This is your personal interview coach now. 🦞
