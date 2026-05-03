# Knowledge Hub — Project Overview & Interview Mastery Guide

> A self-built reference application that doubles as a structured interview-prep tracker, demonstrating ~30+ enterprise patterns across backend, frontend, and infrastructure.

---

## 1. Executive Summary (the elevator pitch)

**Knowledge Hub** is a full-stack web application I built to track my interview-prep topics while simultaneously serving as a *living portfolio* of the patterns I'm comfortable discussing in interviews. Every feature corresponds to a real architectural choice.

In 30 seconds: *"It's a Spring Boot 3.3 + Java 21 backend with a React 18 + TypeScript frontend. The same app demos imperative JPA on PostgreSQL AND reactive WebFlux on MongoDB side-by-side. Spring Security with stateless JWT and role-based access. WebClient calls wrapped in Resilience4j circuit breaker + retry + timeout. Micrometer metrics, Zipkin tracing, Prometheus dashboards. Containerized with Docker Compose."*

That single sentence covers ~15 interview topics.

---

## 2. Architecture at a Glance

```
┌────────────────────────────────────────────────────────────────────┐
│                        Browser (User)                              │
└──────────────────────────────┬─────────────────────────────────────┘
                               │
                  HTTP/HTTPS   │   localhost:3000
                               ▼
┌────────────────────────────────────────────────────────────────────┐
│  React 18 + TypeScript (Vite)                                      │
│  ┌────────────┐ ┌──────────────┐ ┌─────────────┐ ┌──────────────┐  │
│  │ Auth       │ │ TanStack     │ │ Hook Form   │ │ Tailwind +   │  │
│  │ Context    │ │ Query        │ │ + Zod       │ │ Lucide icons │  │
│  └────────────┘ └──────────────┘ └─────────────┘ └──────────────┘  │
└──────────────────────────────┬─────────────────────────────────────┘
                               │  /api/* proxied to backend
                               ▼
┌────────────────────────────────────────────────────────────────────┐
│  Spring Boot 3.3.5 / Java 21      localhost:8080                   │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  Spring Security Filter Chain                                │  │
│  │  CORS → JwtAuthFilter → AuthorizationFilter                  │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌─────────────────────┐  ┌────────────────┐  ┌────────────────┐   │
│  │ TopicController     │  │ NoteController │  │ ResourceCtrl   │   │
│  │ (blocking JPA)      │  │ (reactive Mongo)│ │ (WebClient)    │   │
│  └─────────────────────┘  └────────────────┘  └────────────────┘   │
│           │                       │                   │            │
│           ▼                       ▼                   ▼            │
│  ┌─────────────────┐   ┌────────────────────┐  ┌──────────────┐    │
│  │ TopicService    │   │ NoteService        │  │ ResourceSvc  │    │
│  │ + Streams demo  │   │ Mono/Flux pipelines│  │ Resilience4j │    │
│  │ + Virtual Thrds │   │                    │  │ (CB+R+T+B)   │    │
│  └─────────────────┘   └────────────────────┘  └──────────────┘    │
│           │                       │                   │            │
│           ▼                       ▼                   ▼            │
│  ┌─────────────────┐   ┌────────────────────┐  ┌──────────────┐    │
│  │  PostgreSQL 16  │   │   MongoDB 7        │  │ External API │    │
│  │  (JPA / Hibern.)│   │  (Reactive Driver) │  │              │    │
│  └─────────────────┘   └────────────────────┘  └──────────────┘    │
└────────────────────────────────────────────────────────────────────┘
            │                            │
            ▼                            ▼
   ┌──────────────────┐        ┌────────────────────┐
   │ Micrometer →     │        │ OpenTelemetry →    │
   │ Prometheus →     │        │ Zipkin             │
   │ Grafana          │        │ (distributed       │
   │                  │        │  tracing)          │
   └──────────────────┘        └────────────────────┘
```

---

## 3. Backend Deep Dive

### 3.1 Tech Stack & Versions

| Layer            | Choice                       | Version    | Why                                                        |
|------------------|------------------------------|------------|------------------------------------------------------------|
| Runtime          | Java                         | 21 (LTS)   | Virtual Threads, records, sealed types, pattern matching   |
| Framework        | Spring Boot                  | 3.3.5      | Auto-config, production-ready defaults                     |
| Web              | Spring Web MVC + WebFlux     | 6.1        | MVC for REST, WebFlux for reactive endpoints               |
| Security         | Spring Security              | 6.3        | JWT filter chain, method-level RBAC                        |
| ORM              | Spring Data JPA + Hibernate  | 6.5        | PostgreSQL — relational data                               |
| NoSQL            | Spring Data MongoDB Reactive | 4.3        | MongoDB — document/notes data                              |
| HTTP Client      | Spring WebClient             | 6.1        | Non-blocking, reactive, replaces RestTemplate              |
| Resilience       | Resilience4j                 | 2.2.0      | Circuit breaker, retry, timeout, bulkhead                  |
| JWT              | jjwt                         | 0.12.6     | HS256 signing/verification                                 |
| Password Hash    | BCrypt                       | (built-in) | Strength 12 = ~250ms per hash                              |
| Metrics          | Micrometer + Prometheus      | 1.13       | `/actuator/prometheus` endpoint                            |
| Tracing          | OpenTelemetry                | 2.6        | OTLP export to Zipkin                                      |
| Build            | Maven                        | 3.9.x      | Standard, well-understood                                  |
| Test             | JUnit 5 + Mockito + Testcontainers | latest | Real DB integration tests                                  |

### 3.2 Modules & What They Demonstrate

**`controller/`**
- `AuthController` — `/api/auth/{register, login, refresh, users/{id}/roles/{role}}`
- `TopicController` — JPA CRUD with Bean Validation
- `NoteController` — Reactive `Mono`/`Flux` returns
- `ResourceController` — calls external APIs through WebClient

**`service/`**
- `TopicService` — blocking JPA + Java Streams aggregations + Virtual Threads demo
- `NoteService` — pure reactive composition of `Mono`/`Flux`
- `ResourceService` — stacked `@CircuitBreaker @Retry @TimeLimiter @Bulkhead` annotations
- `StatsService` — 12 reference Java Streams patterns (groupingBy, partitioningBy, reducing, joining, mapMulti, gatherers)
- `UserService` — registration, role management, privilege-escalation defense

**`security/`**
- `SecurityConfig` — filter chain, CORS, public/private route matchers
- `ApplicationConfig` — `UserDetailsService`, `PasswordEncoder`, `AuthenticationProvider` (split out to break circular dependency — interview talking point)
- `JwtService` — token generation/parsing with jjwt 0.12.x fluent API
- `JwtAuthenticationFilter` — `OncePerRequestFilter` extracting `Authorization: Bearer …`

**`domain/jpa/`**
- `Topic` (with `@Version` optimistic locking, `@PrePersist`, `@OneToMany` to Resource)
- `Resource` (owning side of the relationship, `@ManyToOne`)
- `User` (implements `UserDetails` directly, `@ElementCollection` for roles)
- `Role` (enum with `authority()` method returning `"ROLE_"+name()`)

**`domain/mongo/`**
- `Note` (document with embedded `NoteVersion` and `Attachment` records — Java records as embedded value types)

**`exception/`**
- `GlobalExceptionHandler` — `@RestControllerAdvice` mapping exceptions to RFC-7807-style error bodies with proper HTTP statuses
- Specific handlers: `ResourceNotFoundException` → 404, `MethodArgumentNotValidException` → 400, `BadCredentialsException`/`AuthenticationException` → 401, `AccessDeniedException` → 403, fallback `Exception` → 500

**`config/` (implicit via `application.yml`)**
- DB connection pools (HikariCP)
- Resilience4j instance configs
- Tracing/metrics endpoints
- JWT secret + token TTLs

---

## 4. Frontend Deep Dive

### 4.1 Tech Stack & Versions

| Layer            | Choice                | Version  | Why                                                       |
|------------------|-----------------------|----------|-----------------------------------------------------------|
| UI Library       | React                 | 18.3     | Industry standard, concurrent features                    |
| Language         | TypeScript            | 5.6      | Strict mode for type safety end-to-end                    |
| Build Tool       | Vite                  | 5.4      | Native ES modules in dev, instant HMR                     |
| Routing          | React Router          | 6.27     | Nested routes via `<Outlet>`                              |
| Server State     | TanStack Query        | 5.59     | Cache, refetch, optimistic updates — replaces Redux       |
| Forms            | React Hook Form + Zod | 7.53/3.23| Schema-first validation, typed end-to-end                 |
| Styling          | Tailwind CSS          | 3.4      | Utility-first, no CSS files to maintain                   |
| Icons            | Lucide React          | 0.453    | Tree-shakeable SVG icons                                  |
| Class merging    | clsx                  | 2.1      | Conditional className composition                         |

### 4.2 Modules & Patterns Demonstrated

**`auth/`**
- `AuthContext` — React Context holding the user + JWT in memory; `useAuth()` hook exposes `login`, `logout`, `hasAnyRole`
- `ProtectedRoute` — wrapper component that redirects to `/login` if not authenticated; can also check role

**`lib/api.ts`**
- Single `ApiClient` class with a `request<T>` method that:
  - Auto-attaches `Authorization: Bearer …` header
  - Auto-logs out on 401 (clears token, redirects)
  - Parses structured error bodies into typed `ApiError`
  - JSON serialization in/out

**`hooks/useTopics.ts`**
- `useQuery` for fetching, `useMutation` for create/update/delete
- Automatic cache invalidation on mutation success — list refreshes without manual refetch

**`pages/`**
- `LoginPage` / `RegisterPage` — Hook Form + Zod schema validation
- `DashboardPage` — `useTopics` + client-side aggregation (mirrors Java `Collectors.groupingBy`)
- `TopicsPage` — table with create/edit/delete; uses optimistic update pattern
- `NotesPage` — calls reactive backend; demonstrates streaming UI pattern

**`components/Layout.tsx`**
- Sidebar nav with role-conditional rendering (`hasAnyRole('ADMIN')` controls Admin link visibility)

### 4.3 Vite proxy + dev experience

`vite.config.ts` proxies `/api/*` to `http://localhost:8080` — this avoids CORS issues in dev and mirrors how a production reverse proxy (nginx/traefik) would route requests. Production build uses Rollup for optimized chunks; dev uses native ES modules for instant feedback.

---

## 5. Infrastructure & DevOps Layer

| Component        | Image / Tool             | Purpose                                                |
|------------------|--------------------------|--------------------------------------------------------|
| PostgreSQL       | `postgres:16-alpine`     | Relational data (Topics, Resources, Users, Roles)      |
| MongoDB          | `mongo:7.0`              | Document data (Notes with versioning + attachments)    |
| Prometheus       | `prom/prometheus`        | Scrapes `/actuator/prometheus` every 10s               |
| Grafana          | `grafana/grafana`        | Visualizes metrics from Prometheus                     |
| Zipkin           | `openzipkin/zipkin`      | Receives OpenTelemetry traces; `/zipkin` UI            |
| Docker Compose   | -                        | Single `docker-compose up -d` brings up the whole infra|
| Health checks    | `/actuator/health`       | Composite health: db, mongo, ping, diskSpace           |

---

## 6. Interview Topics Covered — File-to-Question Mapping

This is your *cheat sheet*. When asked about X in an interview, point at file Y and explain.

| Topic                              | Where in code                                          | What you say                                                                       |
|-----------------------------------|--------------------------------------------------------|------------------------------------------------------------------------------------|
| **Spring Security filter chain**   | `SecurityConfig.java`                                  | "JWT filter inserted before UsernamePasswordAuthenticationFilter"                  |
| **Stateless JWT**                  | `JwtService.java`, `JwtAuthenticationFilter.java`      | "No session store; horizontal scalability without sticky sessions"                 |
| **BCrypt password hashing**        | `ApplicationConfig.passwordEncoder()`                  | "Strength 12 = 2^12 iterations, ~250ms — slow by design"                            |
| **RBAC**                           | `SecurityConfig` (URL rules) + `@PreAuthorize`         | "Two-level authorization: URL-based filter + method-level for fine grain"          |
| **Circular dependency fix**        | `ApplicationConfig.java` split from `SecurityConfig`   | "Refactor over @Lazy — separate auth beans into their own config class"            |
| **Optimistic locking**             | `Topic.java` `@Version`                                | "Prevents lost updates without locking; throws `OptimisticLockException`"           |
| **JPA lifecycle callbacks**        | `Topic.java` `@PrePersist`, `@PreUpdate`               | "Audit timestamps without an entity listener class"                                |
| **Bean Validation**                | `Topic.name` `@NotBlank`, `User.email` `@Email`        | "Triggered by `@Valid` in controller; mapped to 400 by global handler"             |
| **Global exception handling**      | `GlobalExceptionHandler.java`                          | "Specific handlers BEFORE catch-all; same opaque message for auth failures"        |
| **Java Streams patterns**          | `StatsService.java`                                    | "groupingBy with downstream collectors, partitioningBy, reducing, joining"         |
| **Virtual Threads (Loom)**         | `TopicService.runOnVirtualThreads()`                   | "Cheap-to-create user-mode threads; ideal for blocking I/O fan-out"                |
| **Reactive (Mono/Flux)**           | `NoteService.java`                                     | "Backpressure-aware streams; non-blocking I/O via reactor-netty"                    |
| **WebClient + Resilience4j**       | `ResourceService.java`                                 | "CB protects from cascading failure; retry with backoff; timeout; bulkhead"         |
| **Method security**                | `@PreAuthorize("hasRole('ADMIN')")`                    | "SpEL evaluated before method body; deny goes through AccessDeniedException → 403" |
| **Spring profiles**                | `application.yml` + `application-dev.yml`              | "Different beans/configs per environment"                                          |
| **Actuator + observability**       | `/actuator/{health,info,prometheus}`                   | "Health check exposes composite status; prometheus endpoint scrapes metrics"       |
| **Distributed tracing**            | `application.yml` `management.tracing.*`               | "Each request gets a traceId; visualized in Zipkin"                                 |
| **Custom UserDetails**             | `User implements UserDetails`                          | "Single class is JPA entity + Security principal — pragmatic over adapter"          |
| **Records for DTOs**               | `AuthDto`, `Note.Attachment`                           | "Immutable, less boilerplate, work great as embedded Mongo types"                  |
| **OpenAPI docs (if added)**        | (gap — see enhancements)                               |                                                                                    |
| **React hooks discipline**         | `useAuth`, `useTopics`                                 | "Custom hooks isolate side effects and dependencies"                               |
| **Server state vs UI state**       | TanStack Query (server) + Context (UI)                 | "Server state belongs in a cache, not a Redux store"                                |
| **Form validation**                | `LoginPage` with Hook Form + Zod                       | "Schema-first; types flow from Zod schema to TypeScript"                            |
| **Auth interceptor pattern**       | `lib/api.ts` request method                            | "Single place to attach token, handle 401, parse errors"                            |
| **Optimistic updates**             | `useTopics.update` mutation                            | "Update cache immediately, roll back on error"                                     |

---

## 7. The 60-Second Live Demo (memorize this)

1. **Open `localhost:3000/login`** → "Spring Security with stateless JWT, BCrypt strength 12."
2. **Sign in** → lands on dashboard → "JWT in memory only, never localStorage — XSS-safety choice."
3. **Click into Topics** → "Reading from PostgreSQL via JPA. The `+ New Topic` button is only enabled because my JWT carries the ADMIN role."
4. **Create a topic** → "POST hits the controller, Bean Validation runs first, then service layer enforces uniqueness — a 400 with a structured error if the name already exists, no 500 leakage."
5. **Click Notes** → "Different store entirely — MongoDB via Spring Data Reactive. Same app demos blocking JPA AND non-blocking WebFlux side-by-side."
6. **Open `localhost:8080/actuator/prometheus`** → "Micrometer feeds Prometheus; Grafana visualizes; OpenTelemetry traces go to Zipkin. Production observability built-in."

That's six clicks, six paragraphs, ~20 interview topics covered.

---

## 8. What's Missing — High-Leverage Enhancements

Now the strategic part. Here's a ranked list of additions that would meaningfully expand your interview surface area, with a rough effort estimate and which interview topics they unlock.

### Tier 1 — Highest ROI (do these first)

#### 1. **Database migrations with Flyway** (~30 min)
**What:** Add Flyway with `V1__init.sql`, `V2__add_indexes.sql` etc. in `src/main/resources/db/migration/`.
**Unlocks:** Schema versioning, rollback strategies, "blue/green DB migration" discussion, why `ddl-auto=update` is dangerous in production.
**Interview soundbite:** *"`ddl-auto=update` is a development crutch. Production deploys go through Flyway — versioned, repeatable, auditable, and reversible."*

#### 2. **Redis caching with `@Cacheable`** (~1 hour)
**What:** Add Spring Data Redis, cache Topic GETs, demonstrate cache eviction on mutation.
**Unlocks:** Caching strategies (cache-aside, write-through), TTL vs LRU, cache stampede, Redis as a session store, distributed cache invalidation.
**Interview soundbite:** *"`@Cacheable` is cache-aside — read miss populates, write evicts. For high-traffic reads, this is the single biggest performance win, often 100x faster than DB hit."*

#### 3. **Comprehensive test suite** (~2-3 hours)
**What:** Unit tests with Mockito for services; integration tests with `@SpringBootTest` + Testcontainers spinning up real Postgres/Mongo; controller tests with `MockMvc`/`WebTestClient`.
**Unlocks:** Testing pyramid, why unit tests must NOT touch DB, Testcontainers vs H2, contract testing, mutation testing.
**Interview soundbite:** *"Testcontainers gives you real Postgres in your test, not H2. The schemas drift between H2 and prod Postgres — ask anyone who's hit a JSONB or array-type test pass and prod fail."*

#### 4. **OpenAPI / Swagger UI** (~30 min)
**What:** Add `springdoc-openapi`, get auto-generated `/swagger-ui.html`.
**Unlocks:** API documentation as code, contract-first design, code generation for clients (TypeScript, Java).
**Interview soundbite:** *"OpenAPI is the source of truth — frontend types are generated from the spec, not hand-written. Drift is impossible."*

#### 5. **Pagination & sorting on Topic list** (~30 min)
**What:** Use Spring Data's `Pageable`, return `Page<Topic>` with metadata.
**Unlocks:** Cursor vs offset pagination, why offset breaks at scale, keyset pagination.
**Interview soundbite:** *"Offset pagination is fine up to ~10k rows. Past that, every page request scans-and-discards earlier rows — keyset pagination using `WHERE id > lastId LIMIT n` is O(log n) regardless of page depth."*

### Tier 2 — Strong differentiators

#### 6. **Kafka producer + consumer** (~2 hours)
**What:** Publish a `TopicCreatedEvent` to Kafka when a topic is added; consume it in a separate listener that writes to an audit collection in Mongo.
**Unlocks:** Event-driven architecture, exactly-once semantics, idempotent consumers, transactional outbox pattern, dead-letter queues.
**Interview soundbite:** *"Synchronous chains are brittle. Publishing an event decouples the write from downstream reactions — auditing, search indexing, notifications — each scales independently."*

#### 7. **Outbox pattern** (~1.5 hours, requires Kafka)
**What:** Write `TopicCreatedEvent` to an `outbox` table inside the same DB transaction as the Topic, then a Debezium connector or scheduler ships it to Kafka.
**Unlocks:** Distributed transaction problem, why dual-write is broken, CDC, eventual consistency.
**Interview soundbite:** *"The outbox pattern solves the dual-write problem — atomic write to DB and Kafka. The transaction guarantees consistency; Debezium ships the event eventually."*

#### 8. **WebSocket / Server-Sent Events for live updates** (~1 hour)
**What:** Push topic updates to all connected clients via STOMP-over-WebSocket or SSE.
**Unlocks:** Real-time UX, scaling WebSockets (sticky vs Redis pub-sub), backpressure.

#### 9. **OAuth2 social login (Google)** (~1 hour)
**What:** Add Spring Security OAuth2 client, allow "Sign in with Google" alongside form login.
**Unlocks:** OAuth2 vs OIDC, authorization code flow, PKCE, why social login improves security.

#### 10. **Rate limiting with Bucket4j** (~30 min)
**What:** Limit `/api/auth/login` to 5 requests/minute per IP.
**Unlocks:** Token bucket algorithm, rate limiting strategies, distributed rate limiting via Redis.

### Tier 3 — Architect-level depth

#### 11. **AI/ML integration (RAG over your notes)** (~3 hours, **highly relevant for your career transition**)
**What:** Embed each note via OpenAI (or a local model like nomic-embed-text), store vectors in Postgres `pgvector` or Pinecone, add a `/api/notes/ask` endpoint that does retrieval-augmented generation.
**Unlocks:** Vector embeddings, similarity search, RAG architecture, LLM context windows, prompt engineering, the entire AI/ML architect skillset.
**Interview soundbite:** *"RAG sidesteps fine-tuning. Embed your corpus, store vectors, retrieve top-K by cosine similarity, stuff into the LLM prompt as context. You get up-to-date answers grounded in your private data without retraining a model."*

#### 12. **Saga pattern for multi-step transactions** (~3 hours)
**What:** A "PromoteUser" workflow that: (a) updates roles in Postgres, (b) writes to audit log in Mongo, (c) publishes a `UserPromoted` event to Kafka, (d) on failure, runs compensating actions.
**Unlocks:** Distributed transactions, choreography vs orchestration, eventual consistency.

#### 13. **Multi-tenancy** (~2 hours)
**What:** Add `tenant_id` discriminator to Topic/Note; auto-filter via Hibernate filter or PostgreSQL row-level security.
**Unlocks:** SaaS patterns, tenant isolation strategies, schema-per-tenant vs row-per-tenant.

#### 14. **Audit logging with Hibernate Envers** (~1 hour)
**What:** Auto-track every change to Topic/User in `_aud` tables.
**Unlocks:** Audit trail design, regulatory compliance (SOC2, GDPR, PCI), event sourcing fundamentals.

#### 15. **Containerized deployment to GCP Cloud Run** (~2 hours)
**What:** Multi-stage Dockerfile with native-image GraalVM build, deploy to Cloud Run with Cloud SQL connector, set up CI/CD via GitHub Actions.
**Unlocks:** Container best practices, GraalVM native images, serverless backend, blue/green deployment.

---

## 9. Recommended Priority Order (your roadmap)

Given you have **two interviews tomorrow** (Apache Camel today, EY tomorrow) and ongoing prep:

**Tonight (before tomorrow's interviews):**
- Read this document end-to-end
- Practice the 60-second live demo three times out loud
- Memorize the file-to-question mapping in §6
- Skim `INTERVIEW_GUIDE_SECURITY_AND_OVERALL.md` (already in your repo)

**Next 1-2 weeks (post-EY interview, real career investment):**
1. Tier 1 items 1-5 (the basics every senior dev is expected to know)
2. Tier 2 item 6 (Kafka) — directly aligned with your stated expertise
3. Tier 3 item 11 (RAG) — directly aligned with your AI/ML architect transition

**Next 1-2 months:**
- Tier 2 items 7-10
- Tier 3 items 12-15
- Add a public GitHub README with screenshots so this becomes part of your portfolio

---

## 10. Should You Add More Screenshots / Functionality?

**Short answer: yes, but strategically — not every shiny feature.**

Confidence in interviews comes from **depth, not breadth**. Adding 15 mediocre features won't help; adding 3 features you've truly understood (could whiteboard from scratch, can defend the trade-offs of) will transform you.

**My recommendation, in order:**

1. **Polish what's there first.** Add Flyway (Tier 1 #1), tests (Tier 1 #3), OpenAPI (Tier 1 #4), pagination (Tier 1 #5). These are *expected* of a senior backend dev — not having them is a gap, having them is table stakes.

2. **Pick ONE distinctive feature for your story.** Either Kafka + Outbox (if your next role is microservices-heavy) or RAG over notes (if you're targeting AI/ML architect roles). Don't do both shallowly. Do one deeply.

3. **Add screenshots/GIF to a public README.** A picture of your dashboard, the Zipkin trace, the Prometheus dashboard — these turn the project into a portfolio piece interviewers can browse before the call.

**Specific things you could add tonight that take <15 minutes each:**

- A `README.md` at repo root with the elevator pitch + screenshot
- An ASCII architecture diagram (steal from §2 above)
- A short `CONTRIBUTING.md` showing you think about codebases as collaborative
- `make` targets or `npm run` scripts that one-shot common workflows (`make dev`, `make test`, `make seed`)

These small touches signal *engineering maturity* — they're cheap to add but absent from most candidates' projects.

---

## 11. Closing Thought

You've built something most candidates haven't: a single repository that ties together ~30 senior-developer concepts with working code. The challenge isn't acquiring more concepts — it's being able to walk through this codebase fluently for any interviewer, picking the right file to point at for any question.

**Tonight's job:** read this doc, do the demo three times, sleep.
**Tomorrow's job:** go in confident.
**Next month's job:** Tier 1 enhancements + one Tier 3 distinctive feature.

You're closer to architect-level than you think. The gap from "senior developer" to "architect" isn't more code — it's the ability to articulate *why* every decision is a decision, with trade-offs spelled out. This document is the script.
