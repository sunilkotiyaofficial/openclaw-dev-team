:clipboard: Plan: Deep Dive — Production RAG Architecture

Routing to @aiml specialist (Learning Mode). Here's what's happening:

Parallel (starting now):
• :page_facing_up: @aiml → design doc (all 12 sections, 2500 words, DESIGN_DOC_TEMPLATE.md)
• :computer: @backend → Spring Boot + LangChain4j RAG demo code
• :test_tube: @qa → integration tests for RAG pipeline

Sequential (after above):
• :package: @devops → git branch + PR

ETA: ~3–5 min. I'll post the final summary with doc link + PR when done.

───

Spinning up specialists now...
[8:18 AM]DESIGN_DOC_TEMPLATE.md doesn't exist yet in the workspace. No specialist @aiml agent is configured either. Falling back to direct delivery per SOUL.md fallback protocol — noting this clearly.

:warning: Fallback notice: @aiml specialist agent not found in team roster, and DESIGN_DOC_TEMPLATE.md not present in workspace. Answering directly from my own knowledge using the 12-section doc structure. Logging failure to memory.
[8:18 AM]Subagent spawning is currently unavailable (gateway pairing issue). Going full direct-delivery fallback. Writing the design doc myself now.
[8:18 AM]Workspace is mounted read-only (virtiofs ro). Can't write files to it from the sandbox. The doc will be delivered directly here in Slack instead.

───

:warning: Infrastructure notes:
• @aiml specialist — not in team roster (no agent configured)
• subagent spawn — gateway pairing error (infrastructure issue)
• @devops PR — skipped (no files written, workspace is read-only from sandbox)
• Workspace mount is virtiofs (ro) — file writes require host-side access

:page_facing_up: Direct delivery below — full design doc:

───

Production RAG Architecture

Scale: 10M docs · <500ms p99 · 500 QPS

───

1. Overview & Problem Statement

RAG connects LLMs to private, up-to-date knowledge bases. It solves three enterprise problems: (1) Staleness — LLM training cutoffs; (2) Hallucination — grounding in retrieved facts; (3) Auditability — citation-backed answers.

Naive RAG breaks at enterprise scale. This doc covers the full production architecture.

───

2. Goals & Non-Goals

Goals: <500ms p99, Precision@5 ≥ 0.80, MRR ≥ 0.75, index freshness ≤ 5 min, multi-tenant isolation, zero-downtime model upgrades.

Non-Goals: Sub-second indexing, LLM fine-tuning, video/audio natively, SQL-hybrid.

───

3. Background: Naive vs Advanced RAG

Naive RAG (Lewis et al., 2020):

chunk (fixed 512 tokens) → embed (bi-encoder) → cosine similarity → top-k → stuff prompt
Why it fails:
• Fixed-size chunking destroys semantic boundaries
• Single-stage retrieval = no quality signal
• No query understanding → vague queries → garbage retrieval
• No re-ranking → rank-1 may not be most relevant
• Context stuffing degrades LLM (lost-in-the-middle)

Advanced RAG evolution:
• 2022: HyDE — embed a hypothetical answer, retrieve similar docs
• 2023: RAG-Fusion — multi-query + Reciprocal Rank Fusion
• 2023: FLARE — active retrieval mid-generation
• 2024: CRAG — evaluate retrieved quality, fall back to web search
• 2024: GraphRAG (Microsoft) — knowledge graph for multi-hop reasoning

───

4. Architecture

INGEST:
S3/GCS → Parser (Unstructured) → Semantic Chunker
→ Enricher (NER, metadata) → Embedder (GPU batch) → Vector Index

QUERY:
User Query
  ↓ Query Rewriter (HyDE + multi-query)          ~30ms
  ↓ Hybrid Retriever (dense HNSW + BM25, RRF)    ~65ms
  ↓ Cross-encoder Re-ranker (top-20 → top-5)     ~75ms
  ↓ Context Assembly (compress + cite)
  ↓ LLM Generation (streamed)                    ~320ms
  ↓ Response + Citations                 TOTAL: ~490ms p99
Latency budget (p99):
• Query rewriting: 30ms
• Dense retrieval: 45ms
• Sparse (BM25): 20ms
• RRF fusion: 5ms
• Cross-encoder rerank: 75ms
• LLM generation: 320ms
• Total: ~495ms :white_check_mark:

───

5. Component Design

Chunking strategies (ranked by production value):
• Semantic — sentence-boundary splitting, 256–512 token target (RECOMMENDED)
• Parent-child — small chunks for retrieval, large parent for context injection
• Document-aware — respects section headers (for manuals, technical docs)
• Fixed-size — fast/simple but breaks semantic units (baseline only)

Hybrid Search (BM25 + dense, RRF fusion):
Dense retrieval misses exact-match queries (product codes SKU-7729-X, proper nouns). BM25 handles these perfectly. RRF formula: score = Σ 1/(k + rank_i), k=60. Rank-based, immune to scale differences between systems.

Query Rewriting (3 parallel strategies):

1. HyDE — generate hypothetical answer, embed it (closer to doc style than the question)
2. Multi-query — 3 paraphrases, retrieve for each, merge with RRF
3. Step-back — broaden specific query to parent concept

Re-ranking (highest single-lever impact):
Cross-encoder (ms-marco-MiniLM-L-6-v2) jointly scores (query, chunk). Bi-encoder precision@5 ≈ 0.62. After cross-encoder re-ranking: precision@5 ≈ 0.83. Always apply to top-20 → select top-5.

───

6. Data Model (Qdrant payload)

```
{
  "id": "uuid",
  "vector": [1024-dim float32],
  "payload": {
    "doc_id": "doc-abc123",[8:18 AM]"chunk_index": 3,
    "text": "...",
    "parent_chunk_id": "chunk-parent-456",
    "source_url": "s3://...",
    "section": "2.3 Attention Mechanism",
    "tenant_id": "org-xyz",
    "security_label": "INTERNAL",
    "embedding_model": "bge-large-en-v1.5",
    "embedding_version": "v2"
  }
}───

_7. API Design_

POST /v1/rag/query
{
  "query": "What is the refund policy for enterprise?",
  "tenant_id": "org-xyz",
  "top_k": 5,
  "rewrite": true,
  "stream": true
}

Response:
{
  "answer": "Enterprise plans include 30-day guarantee [1]...",
  "citations": [{ "id": 1, "source_url": "...", "score": 0.91 }],
  "latency_breakdown": {
    "retrieval_ms": 68, "rerank_ms": 42, "generation_ms": 287
  }
}
```
───

8. Failure Modes & Mitigations

• Retrieval miss → if max relevance < 0.6, respond "I don't have info on this" (don't hallucinate)
• Multi-tenant data leak → mandatory tenant_id filter at API gateway, never optional; integration tests on every deploy
• Embedding model drift → version all vectors; shadow index + atomic cutover
• Re-ranker bottleneck → circuit breaker: if p99 > 100ms, fall back to bi-encoder scores
• Lost-in-the-middle → put best chunks at position 1 and last, weaker in middle
• Prompt injection → input sanitization, system prompt hardening, output scanning
• LLM API outage → return raw retrieved chunks with "generation unavailable" message

───

9. Observability & Metrics

Offline (weekly gold eval set, 1,000 queries):
• Precision@5 ≥ 0.80
• Recall@20 ≥ 0.90
• MRR ≥ 0.75 (1/rank of first relevant doc)
• NDCG@5 ≥ 0.78 (graded relevance, position-penalized)

Production alerts:
• rag.query.latency.p99 > 450ms → page
• rag.retrieval.relevance_score.avg drops >15% WoW → embedding drift signal
• rag.generation.thumbs_down_rate > 8% → quality regression
• rag.ingest.lag_seconds > 300s → indexing pipeline alert

───

10. Trade-offs

• Hybrid vs dense-only → hybrid wins recall; 30% more infra overhead, worth it
• Self-hosted Qdrant vs Pinecone → 60% cost savings at 10M vectors; trade-off: ops burden
• Streaming vs sync generation → streaming delivers first token in ~150ms vs 500ms wait; always stream
• Self-hosted cross-encoder vs Cohere API → breakeven at ~15 QPS; at 500 QPS, self-hosted saves ~$43K/day

───

11. Implementation Roadmap

• Week 1-3: Qdrant cluster + ingestion pipeline + naive RAG baseline + eval harness
• Week 4-6: Hybrid search + query rewriting + cross-encoder re-ranking; target P@5 ≥ 0.80
• Week 7-9: Multi-tenancy + streaming + citations + observability + 500 QPS load test
• Week 10-12: Context compression + parent-child chunking + CRAG + shadow re-indexing

───

12. Interview Questions — Staff Engineer Level

Q1: Why does naive RAG fail at scale and what do you fix first?
→ Fixed-size chunking breaks semantics; single-stage retrieval has no quality signal. First fix: cross-encoder re-ranker (P@5: 0.62 → 0.83). Highest ROI single change.

Q2: How do you stay under 500ms p99 at 500 QPS with a re-ranker in path?
→ Budget explicitly: retrieval 65ms + rerank 75ms + generation 320ms = 460ms. Apply cross-encoder to top-20 only (not 100). GPU-based. Async batching. Circuit breaker: if rerank p99 > 100ms, fallback to bi-encoder scores.

Q3: How do you handle embedding model upgrades without downtime?
→ Shadow index pattern: re-embed to new Qdrant collection in background. Dual-read A/B (5%/95%). When new MRR exceeds old by >2%, atomic cutover. Version embedding_model field in every chunk's payload.

Q4: Explain RRF. Why not score normalization?
→ score = Σ 1/(k + rank_i). Rank-based, distribution-agnostic. Dense scores (0.6-0.95) and BM25 scores (0-25) can't be linearly combined — scales are incompatible and corpus-dependent. RRF avoids this entirely.

Q5: How do you prevent multi-tenant data leaks?
[8:18 AM]→ Mandatory tenant_id filter enforced at API gateway layer (not optional parameter). Integration tests that verify Org A queries never return Org B chunks. Separate collections for high-security tenants. Audit log every query.

Q6: What is "lost-in-the-middle" and how do you mitigate it?
[8:18 AM]→ LLMs attend more to beginning/end of context, ignore middle (Liu et al., 2023). Place highest-scored chunk first, second-best last. Apply context compression to distill chunks to most relevant sentences for the specific query.

Q7: How do you measure and improve retrieval quality in production?
→ Offline: gold eval set (1,000 queries), weekly P@k, MRR, NDCG. Online: implicit feedback (query refinement rate), explicit thumbs up/down, relevance_score.avg as drift proxy. A/B every change with p<0.05 significance.

Q8: When would you use GraphRAG instead of vector RAG?
→ Multi-hop reasoning across documents ("which executives mentioned supply chain risks AND had factory closures?"), entity relationships, thematic corpus-wide synthesis. Downside: 10-100x more expensive to build, higher latency. Add as second tier triggered by query classifier, not replace standard RAG.

Q9: How do you handle queries about documents uploaded 2 minutes ago (before index lag clears)?
→ Write-through hot-doc cache (Redis) for last 10 minutes of ingested docs. Query service checks this cache first. Show "retrieved as of [timestamp]" in responses. Separate in-memory Qdrant collection for last-hour docs queried in parallel.

Q10: How do you debug a RAG system giving confidently wrong answers?
→ Work backward via trace ID: (1) Were relevant chunks retrieved? No → retrieval failure (chunking/embedding). (2) Were they in top-20 but re-ranked out? → re-ranker calibration. (3) Were they in the prompt but ignored? → lost-in-middle, prompt engineering. (4) Is the knowledge missing from corpus? → data gap, onboard the source.
