# Production RAG Architecture

**Date:** 2026-04-26
**Agent:** @aiml (delivered via @orchestrator fallback mode)
**Topic slug:** production-rag-architecture
**Level:** Staff Engineer / AI Architect
**Scale targets:** 10M docs · <500ms p99 · 500 QPS

---

## 1. Overview & Problem Statement

RAG connects LLMs to private, up-to-date knowledge bases. It solves three enterprise problems:

1. **Staleness** — LLM training cutoffs
2. **Hallucination** — grounding in retrieved facts
3. **Auditability** — citation-backed answers

Naive RAG breaks at enterprise scale. This doc covers the full production architecture.

---

## 2. Goals & Non-Goals

**Goals:**
- <500ms p99 latency
- Precision@5 ≥ 0.80
- MRR ≥ 0.75
- Index freshness ≤ 5 min
- Multi-tenant isolation
- Zero-downtime model upgrades

**Non-Goals:**
- Sub-second indexing
- LLM fine-tuning
- Video/audio natively
- SQL-hybrid

---

## 3. Background: Naive vs Advanced RAG

### Naive RAG (Lewis et al., 2020)

```
chunk (fixed 512 tokens) → embed (bi-encoder) → cosine similarity → top-k → stuff prompt
```

### Why Naive Fails

- Fixed-size chunking destroys semantic boundaries
- Single-stage retrieval = no quality signal
- No query understanding → vague queries → garbage retrieval
- No re-ranking → rank-1 may not be most relevant
- Context stuffing degrades LLM (lost-in-the-middle)

### Advanced RAG Evolution

| Year | Technique | What it adds |
|---|---|---|
| 2022 | **HyDE** | Embed a hypothetical answer, retrieve similar docs |
| 2023 | **RAG-Fusion** | Multi-query + Reciprocal Rank Fusion |
| 2023 | **FLARE** | Active retrieval mid-generation |
| 2024 | **CRAG** | Evaluate retrieved quality, fall back to web search |
| 2024 | **GraphRAG** (Microsoft) | Knowledge graph for multi-hop reasoning |

---

## 4. Architecture

### Ingest Pipeline

```
S3/GCS → Parser (Unstructured) → Semantic Chunker
       → Enricher (NER, metadata) → Embedder (GPU batch) → Vector Index
```

### Query Pipeline

```
User Query
  ↓ Query Rewriter (HyDE + multi-query)          ~30ms
  ↓ Hybrid Retriever (dense HNSW + BM25, RRF)    ~65ms
  ↓ Cross-encoder Re-ranker (top-20 → top-5)     ~75ms
  ↓ Context Assembly (compress + cite)
  ↓ LLM Generation (streamed)                    ~320ms
  ↓ Response + Citations                 TOTAL: ~490ms p99
```

### Latency Budget (p99)

| Stage | Time |
|---|---|
| Query rewriting | 30ms |
| Dense retrieval | 45ms |
| Sparse (BM25) | 20ms |
| RRF fusion | 5ms |
| Cross-encoder rerank | 75ms |
| LLM generation | 320ms |
| **Total** | **~495ms** |

---

## 5. Component Design

### Chunking Strategies (Ranked by Production Value)

1. **Semantic** — sentence-boundary splitting, 256–512 token target ✅ **RECOMMENDED**
2. **Parent-child** — small chunks for retrieval, large parent for context injection
3. **Document-aware** — respects section headers (for manuals, technical docs)
4. **Fixed-size** — fast/simple but breaks semantic units (baseline only)

### Hybrid Search (BM25 + Dense, RRF Fusion)

Dense retrieval misses exact-match queries (product codes `SKU-7729-X`, proper nouns). BM25 handles these perfectly.

**RRF formula:** `score = Σ 1/(k + rank_i)`, k=60.

Rank-based, immune to scale differences between systems.

### Query Rewriting (3 Parallel Strategies)

1. **HyDE** — generate hypothetical answer, embed it (closer to doc style than the question)
2. **Multi-query** — 3 paraphrases, retrieve for each, merge with RRF
3. **Step-back** — broaden specific query to parent concept

### Re-ranking (Highest Single-Lever Impact)

Cross-encoder (`ms-marco-MiniLM-L-6-v2`) jointly scores (query, chunk).

| Approach | Precision@5 |
|---|---|
| Bi-encoder only | 0.62 |
| Bi-encoder + cross-encoder rerank | **0.83** |

Always apply to top-20 → select top-5.

---

## 6. Data Model (Qdrant Payload)

```json
{
  "id": "uuid",
  "vector": [1024-dim float32],
  "payload": {
    "doc_id": "doc-abc123",
    "chunk_index": 3,
    "text": "...",
    "parent_chunk_id": "chunk-parent-456",
    "source_url": "s3://...",
    "section": "2.3 Attention Mechanism",
    "tenant_id": "org-xyz",
    "security_label": "INTERNAL",
    "embedding_model": "bge-large-en-v1.5",
    "embedding_version": "v2"
  }
}
```

---

## 7. API Design

### Request

```http
POST /v1/rag/query
{
  "query": "What is the refund policy for enterprise?",
  "tenant_id": "org-xyz",
  "top_k": 5,
  "rewrite": true,
  "stream": true
}
```

### Response

```json
{
  "answer": "Enterprise plans include 30-day guarantee [1]...",
  "citations": [
    { "id": 1, "source_url": "...", "score": 0.91 }
  ],
  "latency_breakdown": {
    "retrieval_ms": 68,
    "rerank_ms": 42,
    "generation_ms": 287
  }
}
```

---

## 8. Failure Modes & Mitigations

| Failure mode | Mitigation |
|---|---|
| **Retrieval miss** | If max relevance < 0.6, respond "I don't have info on this" (don't hallucinate) |
| **Multi-tenant data leak** | Mandatory `tenant_id` filter at API gateway, never optional; integration tests on every deploy |
| **Embedding model drift** | Version all vectors; shadow index + atomic cutover |
| **Re-ranker bottleneck** | Circuit breaker: if p99 > 100ms, fall back to bi-encoder scores |
| **Lost-in-the-middle** | Put best chunks at position 1 and last, weaker in middle |
| **Prompt injection** | Input sanitization, system prompt hardening, output scanning |
| **LLM API outage** | Return raw retrieved chunks with "generation unavailable" message |

---

## 9. Observability & Metrics

### Offline (Weekly Gold Eval Set, 1,000 Queries)

- **Precision@5** ≥ 0.80
- **Recall@20** ≥ 0.90
- **MRR** ≥ 0.75 (1/rank of first relevant doc)
- **NDCG@5** ≥ 0.78 (graded relevance, position-penalized)

### Production Alerts

- `rag.query.latency.p99` > 450ms → **page**
- `rag.retrieval.relevance_score.avg` drops >15% WoW → embedding drift signal
- `rag.generation.thumbs_down_rate` > 8% → quality regression
- `rag.ingest.lag_seconds` > 300s → indexing pipeline alert

---

## 10. Trade-offs

| Decision | Choice | Rationale |
|---|---|---|
| **Hybrid vs dense-only** | Hybrid | Wins recall; 30% more infra overhead, worth it |
| **Self-hosted Qdrant vs Pinecone** | Self-hosted at scale | 60% cost savings at 10M vectors; trade-off: ops burden |
| **Streaming vs sync generation** | Always stream | Streaming delivers first token in ~150ms vs 500ms wait |
| **Self-hosted cross-encoder vs Cohere API** | Self-hosted | Breakeven at ~15 QPS; at 500 QPS, self-hosted saves ~$43K/day |

---

## 11. Implementation Roadmap

### Week 1-3 — Foundation
- Qdrant cluster + ingestion pipeline
- Naive RAG baseline
- Eval harness

### Week 4-6 — Quality
- Hybrid search
- Query rewriting
- Cross-encoder re-ranking
- Target: P@5 ≥ 0.80

### Week 7-9 — Production
- Multi-tenancy
- Streaming + citations
- Observability
- 500 QPS load test

### Week 10-12 — Advanced
- Context compression
- Parent-child chunking
- CRAG
- Shadow re-indexing

---

## 12. Interview Questions — Staff Engineer Level

### Q1: Why does naive RAG fail at scale and what do you fix first?

> Fixed-size chunking breaks semantics; single-stage retrieval has no quality signal. **First fix: cross-encoder re-ranker (P@5: 0.62 → 0.83). Highest ROI single change.**

### Q2: How do you stay under 500ms p99 at 500 QPS with a re-ranker in path?

> Budget explicitly: retrieval 65ms + rerank 75ms + generation 320ms = 460ms.
> Apply cross-encoder to **top-20 only** (not 100). GPU-based. Async batching.
> Circuit breaker: if rerank p99 > 100ms, fallback to bi-encoder scores.

### Q3: How do you handle embedding model upgrades without downtime?

> **Shadow index pattern**: re-embed to new Qdrant collection in background.
> Dual-read A/B (5%/95%). When new MRR exceeds old by >2%, atomic cutover.
> Version `embedding_model` field in every chunk's payload.

### Q4: Explain RRF. Why not score normalization?

> `score = Σ 1/(k + rank_i)`. Rank-based, distribution-agnostic.
> Dense scores (0.6-0.95) and BM25 scores (0-25) can't be linearly combined — scales are incompatible and corpus-dependent.
> RRF avoids this entirely.

### Q5: How do you prevent multi-tenant data leaks?

> Mandatory tenant_id filter enforced at API gateway layer (not optional parameter).
> Integration tests that verify Org A queries never return Org B chunks.
> Separate collections for high-security tenants. Audit log every query.

### Q6: What is "lost-in-the-middle" and how do you mitigate it?

> LLMs attend more to beginning/end of context, ignore middle (Liu et al., 2023).
> Place highest-scored chunk first, second-best last.
> Apply context compression to distill chunks to most relevant sentences for the specific query.

### Q7: How do you measure and improve retrieval quality in production?

> **Offline:** gold eval set (1,000 queries), weekly P@k, MRR, NDCG.
> **Online:** implicit feedback (query refinement rate), explicit thumbs up/down, relevance_score.avg as drift proxy.
> A/B every change with p<0.05 significance.

### Q8: When would you use GraphRAG instead of vector RAG?

> Multi-hop reasoning across documents ("which executives mentioned supply chain risks AND had factory closures?"), entity relationships, thematic corpus-wide synthesis.
> **Downside:** 10-100x more expensive to build, higher latency.
> Add as second tier triggered by query classifier, not replace standard RAG.

### Q9: How do you handle queries about documents uploaded 2 minutes ago (before index lag clears)?

> Write-through hot-doc cache (Redis) for last 10 minutes of ingested docs.
> Query service checks this cache first.
> Show "retrieved as of [timestamp]" in responses.
> Separate in-memory Qdrant collection for last-hour docs queried in parallel.

### Q10: How do you debug a RAG system giving confidently wrong answers?

> Work backward via trace ID:
> 1. Were relevant chunks retrieved? No → retrieval failure (chunking/embedding).
> 2. Were they in top-20 but re-ranked out? → re-ranker calibration.
> 3. Were they in the prompt but ignored? → lost-in-middle, prompt engineering.
> 4. Is the knowledge missing from corpus? → data gap, onboard the source.

---

## Reading Order for Interview Recall

For maximum recall under interview pressure, read in this order:

1. **Section 1** (TL;DR / Overview) — the 3 problems RAG solves
2. **Section 12** (10 Q&A) — quotable answers you can deploy
3. **Section 5** (Component Design) — concrete techniques to mention
4. **Section 4** (Architecture diagram + latency budget) — system design backbone
5. **Section 8** (Failure modes) — interviewers love these
6. **Section 10** (Trade-offs) — shows judgment
7. **Sections 3, 6, 7, 9, 11** — deeper context

---

## Killer One-Liners (Memorize for Interviews)

- "Cross-encoder re-ranking is the highest-ROI single change in RAG quality (P@5: 0.62 → 0.83)."
- "RRF fuses ranks not scores — scales between dense and BM25 are incompatible."
- "Lost-in-the-middle is real; place best chunks at position 1 and last."
- "Self-hosted Qdrant at 10M vectors saves 60% vs Pinecone; ops burden is the trade-off."
- "Multi-tenant data leak is the #1 production failure — mandatory tenant_id filter at API gateway, integration-tested every deploy."

---

## Related Topics to Deep Dive Next

- Vector DB Selection (Pinecone vs Weaviate vs Qdrant vs pgvector) — comparison matrix
- Embedding Model Selection (OpenAI vs Voyage vs BGE vs Cohere)
- LLM Cost Optimization (prompt caching, semantic caching, model routing)
- Agent Architecture (RAG as a tool in LangGraph)
- LLM Evaluation Strategies (Ragas, DeepEval)

---

**Doc version:** 1.0
**Last updated:** 2026-04-26
**Quiz yourself:** DM `@orchestrator quiz me on RAG architecture` in Slack
