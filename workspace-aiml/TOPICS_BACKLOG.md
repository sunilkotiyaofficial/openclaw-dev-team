# 🤖 AI/ML — Topics Backlog

Interview study roadmap for AI/ML architect interviews. These topics test **decision-making and architecture**, not trivia.

**How to use:**
- Topics ordered by priority (P0 = highest-leverage for AI/ML architect interviews in 2026)
- Trigger a deep dive in Slack: `@aiml deep dive on {topic}`
- AI/ML interviews reward **framework thinking** — practice articulating trade-offs, not memorizing facts

---

## P0 — Must Know (AI/ML Architect Interviews Will Test These)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 1 | **RAG Architecture (naive → advanced)** | 🔸 Intermediate | ⬜ Not started | Core AI architect topic. Query rewriting, re-ranking, hybrid search. |
| 2 | **Vector DB Selection** (Pinecone vs Weaviate vs Qdrant vs pgvector) | 🔸 Intermediate | ⬜ Not started | Know when each shines. Cost + latency + features matrix. |
| 3 | **Embedding Model Selection** | 🔸 Intermediate | ⬜ Not started | OpenAI vs Voyage vs BGE. Dimension/cost/quality trade-offs. |
| 4 | **LLM Evaluation Strategies** | 🔥 Advanced | ⬜ Not started | How do you know it works? Ragas, DeepEval, LLM-as-judge. |
| 5 | **Cost Optimization for LLM Apps** | 🔸 Intermediate | ⬜ Not started | Prompt caching, semantic caching, model routing, token minimization. |
| 6 | **Fine-tuning vs RAG Decision Framework** | 🔥 Advanced | ⬜ Not started | When each is right. Most teams over-fine-tune. |
| 7 | **Agent Architecture** (tool use + multi-step reasoning) | 🔥 Advanced | ⬜ Not started | ReAct, function calling, tool routing, when agents break. |

## P1 — Should Know (Common Questions)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 8 | **Chunking Strategies** | 🔸 Intermediate | ⬜ Not started | Fixed, recursive, semantic, late chunking. Why it matters. |
| 9 | **Prompt Engineering Patterns** | 🔹 Beginner | ⬜ Not started | Zero-shot, few-shot, CoT, ReAct, structured output. |
| 10 | **Agent Frameworks Comparison** | 🔸 Intermediate | ⬜ Not started | LangGraph vs CrewAI vs AutoGen vs Claude Agent SDK. |
| 11 | **Hallucination Mitigation** | 🔸 Intermediate | ⬜ Not started | Grounding, citations, confidence scoring, guardrails. |
| 12 | **Prompt Injection Defense** | 🔸 Intermediate | ⬜ Not started | Prompt leaks, jailbreaks, structured output as defense. |
| 13 | **Semantic Caching** | 🔸 Intermediate | ⬜ Not started | Cost killer — when embeddings match, skip the LLM call. |
| 14 | **Multi-modal LLMs** (text + image + audio) | 🔸 Intermediate | ⬜ Not started | GPT-4o, Claude 3.5 Sonnet vision, Gemini. |
| 15 | **LLM Observability** (LangSmith, Langfuse) | 🔸 Intermediate | ⬜ Not started | Tracing, token usage, error rates, debugging. |

## P2 — Nice to Have (Shows Depth)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 16 | **Open-source LLM Deployment** (vLLM, TGI, Ollama) | 🔥 Advanced | ⬜ Not started | When to self-host vs hosted API. |
| 17 | **LoRA / QLoRA / PEFT** | 🔥 Advanced | ⬜ Not started | Parameter-efficient fine-tuning basics. |
| 18 | **Graph RAG** | 🔥 Advanced | ⬜ Not started | Knowledge graph + vector DB hybrid. |
| 19 | **MCP (Model Context Protocol)** | 🔸 Intermediate | ⬜ Not started | Cutting edge — Anthropic's standard you're actually using. |
| 20 | **Structured Output & Function Calling** | 🔸 Intermediate | ⬜ Not started | JSON Schema, Pydantic AI, Instructor, tool routing. |
| 21 | **AI Safety & Red Teaming** | 🔥 Advanced | ⬜ Not started | Jailbreak resistance, bias, compliance (EU AI Act). |
| 22 | **Embedding Drift & Re-indexing** | 🔸 Intermediate | ⬜ Not started | When models update, how to migrate embeddings. |

---

## Suggested Learning Path

**Week 1 — RAG Foundations:** #1, #2, #3, #8 (RAG, vector DB, embeddings, chunking)
**Week 2 — Quality:** #4, #11 (evaluation, hallucination mitigation)
**Week 3 — Cost & Scale:** #5, #13, #16 (cost optimization, caching, self-hosting)
**Week 4 — Agents:** #7, #9, #10, #20 (agent architecture, prompts, frameworks, function calling)
**Week 5 — Advanced:** #6, #12, #21 (fine-tuning, security, safety)
**Week 6 — Cutting Edge:** #14, #15, #18, #19 (multi-modal, observability, Graph RAG, MCP)

---

## Cross-References (Where Your Java/Backend Strength Compounds)

- **#7 Agent Architecture** pairs with `kafka` → Agent orchestration is Saga pattern for LLM calls
- **#5 LLM Cost Optimization** pairs with `backend` → Rate limiting, circuit breaker (same patterns!)
- **#2 Vector DB Sharding** pairs with `kafka` → Partitioning principles identical
- **#16 LLM Deployment** pairs with `devops` → GPU nodes on GKE, model serving
- **#4 LLM Evaluation** pairs with `qa` → Testing strategy for non-deterministic systems
- **#15 LLM Observability** pairs with `devops` → Metrics + Logs + Traces (same 3 pillars)

## Your Unique Angle as an Interview Candidate

You're **backend architect + AI-fluent** — this intersection is rare and valuable. Interview framing:

- "I've architected distributed systems at scale. Now I'm applying those principles to AI workloads — it's the same problem space (consistency, latency, cost, observability) with different failure modes."
- "I know the patterns that actually work because I've debugged them in production. Most AI teams haven't hit those walls yet."
- "Coming from EDA (Kafka, Saga), multi-agent systems feel natural — they're just distributed transactions with LLM steps."

---

## Status Legend

- ⬜ Not started
- 🔨 In progress
- 📝 Studied
- 🎯 Quiz-ready
- ✅ Mastered

---

**Last updated:** 2026-04-18
**Total topics:** 22 (7 P0 + 8 P1 + 7 P2)
**Mastered:** 0 / 22
