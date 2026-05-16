# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Layout

This is a polyglot monorepo. Treat each top-level directory as an independent project:

- **`knowledge-hub/`** — Active reference app. Spring Boot 3.3 + Java 21 backend + React 18 + Vite frontend. Most day-to-day code work happens here.
- **`order-processing-poc/`** — Saga-pattern microservices POC (4 Spring Boot services + React dashboard) coordinated over Kafka 3.7 KRaft. Self-contained with its own `docker-compose.yml` and per-service Maven modules.
- **`workspace-{orchestrator,backend,frontend,qa,devops,kafka,aiml}/`** — Per-agent definition files (`SOUL.md`, `IDENTITY.md`, `BOOTSTRAP.md`, etc.) for the OpenClaw multi-agent system. Not application code; these are runtime config for the orchestrator/specialist agents. Do not refactor as if it were code.
- **`scripts/`** — Bash scripts for the OpenClaw gateway lifecycle (`setup-dev-team.sh`, `health-check.sh`, `monitor-agents.sh`).
- Top-level `*.md` files are **shared templates** (`DESIGN_DOC_TEMPLATE.md`, `CODE_PROJECT_TEMPLATE.md`, `PR_DESCRIPTION_TEMPLATE.md`, `POSTMAN_COLLECTION_TEMPLATE.md`) that the agents reference when generating artifacts. When editing template files, remember they are read by other agents — keep the section structure stable.

The OpenClaw orchestration tooling (`openclaw` CLI, gateway, Slack workspace) is invoked at the user's machine level — it is **not** something Claude Code should try to start, restart, or shell into. **Note:** OpenClaw was migrated from Telegram to Slack in April 2026. The agents communicate over Slack channels (`#orchestrator`, `#backend`, `#kafka`, `#devops`, `#qa`, `#frontend`, `#aiml`). Older docs that reference Telegram are stale.

## knowledge-hub — Common Commands

There is no Maven wrapper checked in (despite what `knowledge-hub/README.md` says). Use the system `mvn`:

```bash
# Infra (Postgres, MongoDB, Zipkin, Prometheus, Grafana)
cd knowledge-hub && docker-compose up -d

# Backend
cd knowledge-hub/backend
mvn spring-boot:run                              # run app on :8080
mvn test                                         # all tests
mvn -Dtest=TopicServiceTest test                 # single test class
mvn -Dtest=TopicServiceTest#findById_returnsTopic test   # single method
mvn package -DskipTests                          # build jar without tests

# Frontend
cd knowledge-hub/frontend
npm install
npm run dev      # Vite on :3000, proxies /api -> :8080
npm run build    # tsc -b && vite build (typecheck is part of build)
npm run lint     # eslint .ts/.tsx
```

Useful endpoints when the backend is running: `http://localhost:8080/actuator/health`, `/actuator/prometheus`, Zipkin at `http://localhost:9411`, Grafana at `http://localhost:3001` (admin/admin).

JWT secret in `application.yml` is a known dev placeholder — override with the `JWT_SECRET` env var for any non-local run.

### Quick reference — facts that have bitten past sessions

These are the gotchas that cost real debugging time. Memorize them.

- **Postgres database name is `knowledge_hub` (underscore), NOT `knowledgehub`.** Confirm with `\l` inside psql. The wrong name returns `database "knowledgehub" does not exist`.
- **Topic entity field is `name`, NOT `title`.** JSON POST bodies must use `"name"`. Jackson silently drops unknown `"title"` keys, and Bean Validation then fails because `name` is `@NotBlank` → returns 500 (caught by generic handler).
- **`Category` enum values** (the only valid strings for `topic.category`): `JAVA`, `SPRING`, `DATABASE`, `KAFKA`, `CLOUD`, `ALGORITHMS`, `SYSTEM_DESIGN`, `AI_ML`, `INTEGRATION`, `OBSERVABILITY`, `TESTING`, `BEHAVIORAL`. Anything else returns 500 via `InvalidFormatException`.
- **Grafana port is `3001` on the host** (mapped from container `3000`) to avoid the Vite dev server port collision at `3000`.
- **`curl` exits 0 even on HTTP 4xx/5xx.** Seed/test scripts MUST capture status via `-w "%{http_code}"` and branch on it — past sessions printed false-positive ✓ checkmarks while every request was actually returning 500.
- **`docker exec` is a shell command, NOT a SQL command.** Don't try to run it inside a `psql` prompt — exit psql first with `\q`.
- **`docker-compose down -v` deletes named volumes** — wipes Postgres + Mongo data including seeded users and topics. Routine `down` is safe; only use `-v` deliberately.

### Testing — current state

There is currently **no meaningful test suite** in `knowledge-hub/backend`. `mvn test` will pass instantly because there are no tests. The single highest-leverage Tier-1 enhancement is adding Testcontainers-based integration tests (`@SpringBootTest` with real Postgres + Mongo containers). See `knowledge-hub/PROJECT_OVERVIEW_AND_INTERVIEW_GUIDE.md` §8 (the Tier-1 roadmap) for the full plan.

### Reference / interview-prep documents (in `knowledge-hub/`)

These are study artifacts owned by the user, NOT templates or live code. **Do not refactor them as code** when making changes elsewhere:

- `PROJECT_OVERVIEW_AND_INTERVIEW_GUIDE.md` — full architecture diagram, tech stack tables, file-to-question mapping, 60-second demo script, 15-item ranked enhancement roadmap
- `STORY_TOPIC_VS_NOTE_FLOW.md` — narrative walkthrough of the sync (JPA + Postgres) vs async (Reactive + Mongo) request flows plus Docker volume persistence model
- `BFF_PATTERN_DEEP_DIVE.md` — Backend-for-Frontend pattern with three aggregation strategies (sequential / CompletableFuture / Mono.zip), production concerns, full code
- `INTERVIEW_GUIDE_SECURITY_AND_OVERALL.md` — earlier interview prep guide covering Spring Security in depth
- `INTERVIEW_TALKING_POINTS.md` — short-form interview soundbites

## knowledge-hub — Architecture Notes

The backend is intentionally a **showcase of mixed paradigms** (this is a portfolio/interview project, not a single-style codebase). When making changes, preserve the paradigm of the slice you are touching:

- **Servlet + Virtual Threads slice** — `TopicService` / `TopicController` / `Topic` JPA entity → PostgreSQL. Blocking style is intentional; virtual threads are enabled globally via `spring.threads.virtual.enabled: true` plus the `applicationTaskExecutor` bean in `KnowledgeHubApplication.java`. Do NOT introduce `synchronized` blocks here — they pin virtual threads. Use `ReentrantLock` if locking is needed.
- **Reactive WebFlux slice** — `NoteService` / `NoteController` / `Note` Mongo document → reactive Mongo. Returns `Mono`/`Flux` end to end, including SSE endpoints. Do not call blocking JPA repos from this slice; do not `.block()` a reactor pipeline.
- **Resilience4j slice** — `ResourceService` and `client/ExternalArticleClient.java` use the `articleService` instance configured in `application.yml` (circuit breaker + retry + time-limiter + bulkhead). New external HTTP calls should follow the same pattern and add their own named instance instead of overloading `articleService`.

Both web stacks (`spring-boot-starter-web` and `spring-boot-starter-webflux`) are on the classpath at the same time — this is intentional for the demo and works because Spring Boot picks Servlet as primary when both are present. Don't "fix" this.

### Security wiring (recently refactored — preserve the split)

Auth beans were deliberately split to break a circular dependency. Keep them in their assigned files:

- `security/SecurityConfig.java` — only the `SecurityFilterChain` and CORS bean. Constructor-injects `AuthenticationProvider` and `JwtAuthenticationFilter`; never declare `UserDetailsService`/`PasswordEncoder`/`AuthenticationProvider`/`AuthenticationManager` here.
- `security/ApplicationConfig.java` — `UserDetailsService`, `PasswordEncoder` (BCrypt), `DaoAuthenticationProvider`, and `AuthenticationManager` beans live here.
- `security/JwtService.java` — uses jjwt **0.12.x fluent API** (`Jwts.builder().subject(...).signWith(key, Jwts.SIG.HS256)`). Do not regress to the deprecated 0.11.x API.
- `security/JwtAuthenticationFilter.java` — runs before `UsernamePasswordAuthenticationFilter`.
- Authorization model: roles are `USER`, `EDITOR`, `ADMIN`. URL rules in `SecurityConfig` are GET → any authenticated, POST/PUT → EDITOR+, DELETE → ADMIN. Method-level `@PreAuthorize` is enabled via `@EnableMethodSecurity`.
- Auth exceptions are mapped to proper status codes (401 vs 403) by handlers in `exception/GlobalExceptionHandler.java` — don't let them fall through to the generic 500 path.

### Frontend conventions

- Vite alias `@` → `frontend/src/`. Use `@/auth/...`, `@/lib/...`, etc., not relative `../../`.
- API client is the singleton `api` exported from `src/lib/api.ts`. It owns the access token (sessionStorage), auto-attaches `Authorization: Bearer ...`, and on 401 clears the token and redirects to `/login`. Don't bypass it with raw `fetch` for backend calls.
- Server state lives in TanStack Query (`src/hooks/use*.ts`); UI state in component hooks. Don't put fetched data in React state.
- Routing uses React Router v6 (`react-router-dom@^6.27`). Vite is **pinned to 5.x** intentionally — do not bump to 6/7 without checking `tsconfig.node.json` and the React 18 toolchain.
- Tailwind is locked to a light theme; the dashboard/sidebar were redesigned recently — match the existing visual language rather than introducing a dark/competing palette.

## order-processing-poc

Independent multi-module Maven project. Quick start: `cd order-processing-poc && cp .env.example .env && docker-compose up --build`. Services: order (8081), payment (8082), inventory (8083), shipping (8084); React dashboard on 3000; Kafdrop on 9000. Saga is choreography-based over Kafka — `order-service` owns the state machine. See `order-processing-poc/README.md` for failure-path test inputs.

## Working with the agent workspaces

If asked to update an agent's behavior, edit `workspace-{agent}/SOUL.md` (the operational prompt) or the smaller files alongside it. The orchestrator's `SOUL.md` defines the three modes (LEARNING / BUILD / INTERVIEW), the delegation protocol (`agentToAgent` → `sessions_spawn` → direct fallback), and the expected structured response format from specialists. When changing one agent's contract, check the orchestrator's `SOUL.md` for matching expectations before/after.
