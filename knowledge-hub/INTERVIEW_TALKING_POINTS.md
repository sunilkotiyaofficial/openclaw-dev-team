# Knowledge Hub — Interview Talking Points

Walkthrough script for interviewers asking about Spring Boot / Java 21 / React.

## Opening Pitch (90 sec)

> "I'll show you a Knowledge Hub — a real reference application I built that demonstrates production patterns across Spring Boot 3.3 and Java 21:
>
> - 3 entities — Topic and Resource on PostgreSQL via JPA, Note on MongoDB via reactive driver — to show when each storage type is right
> - Two service flavors side-by-side — TopicService is traditional blocking with Java 21 Virtual Threads, NoteService is fully reactive with Mono/Flux — to show I understand both programming models
> - ResourceService demonstrates external API integration with WebClient and Resilience4j — circuit breaker, retry, time limiter, bulkhead all stacked
> - Global exception handling, Bean Validation, Micrometer metrics, OpenTelemetry tracing
> - React 18 + TypeScript frontend with TanStack Query for the UI layer
>
> Want me to walk through any specific pattern?"

## Quick Reference — File to Open Per Question

| Question | File |
|---|---|
| "Show me JPA entity" | `domain/jpa/Topic.java` |
| "Bidirectional relationship?" | `Topic.java` + `Resource.java` |
| "MongoDB document?" | `domain/mongo/Note.java` |
| "Spring Data JPA repository?" | `repository/jpa/TopicRepository.java` (5 query styles) |
| "Reactive repository?" | `repository/mongo/NoteRepository.java` |
| "Java Streams examples?" | `service/StatsService.java` (12 patterns!) |
| "Java Collections (HashMap vs TreeMap)?" | `service/StatsService.java` |
| "Virtual Threads?" | `KnowledgeHubApplication.java` + `TopicService.bulkMarkMasteredAsync` |
| "Mono/Flux examples?" | `service/NoteService.java` |
| "WebClient?" | `client/ExternalArticleClient.java` |
| "Resilience4j (circuit breaker)?" | `service/ResourceService.java` |
| "Exception handling?" | `exception/GlobalExceptionHandler.java` |
| "Bean Validation?" | `Topic.java` (entity-level) + `TopicController` (param-level) |
| "Transactions?" | `TopicService` (`@Transactional` examples) |
| "@Async + CompletableFuture?" | `TopicService.bulkMarkMasteredAsync` |
| "Micrometer / Prometheus?" | `application.yml` + `@Timed`/`@Counted` annotations |

## 10 Killer One-Liners (Memorize)

1. **"Java 21 Virtual Threads + traditional Spring MVC = reactive-like throughput with imperative code style. The JVM multiplexes thousands of VTs onto a small carrier pool."**

2. **"For new code: WebClient for reactive pipelines, RestClient (Spring 6) for synchronous calls. RestTemplate is in maintenance mode."**

3. **"`@Transactional(readOnly = true)` is a hint — JPA skips dirty checking, significant query speedup."**

4. **"PostgreSQL for structured data + complex joins. MongoDB for flexible schema + embedded sub-documents that travel together."**

5. **"Resilience4j patterns layered: TimeLimiter → Retry → CircuitBreaker → Bulkhead. Always provide a fallback."**

6. **"Bidirectional JPA: cascade-all and orphan-removal are powerful but dangerous defaults — opt in only when the relationship truly owns lifecycle."**

7. **"Constructor injection over field injection: immutable, testable, NPE-free. `final` fields make Spring inject via constructor."**

8. **"Reactive Mono = 0 or 1, Flux = 0 to N. Keep streams streaming — only `collectList()` at the API boundary."**

9. **"`@RestControllerAdvice` + custom exceptions = consistent error responses across the API. RFC 7807 Problem Details is the modern standard."**

10. **"Streams are lazy — intermediate operations don't run until a terminal operation. filter+map+filter is one pass through the data, not three."**

## Java Streams — Quick Reference

Open `StatsService.java` and walk through any of these 12 patterns:

| Pattern | Method | Key Concept |
|---|---|---|
| 1 | `p0TopicNames` | filter + map + sorted + collect |
| 2 | `countByCategory` | groupingBy + counting |
| 3 | `describeNamesByCategory` | groupingBy + summarizingInt |
| 4 | `partitionMastered` | partitioningBy |
| 5 | `firstByCategoryString` | toMap with merge function |
| 6 | `longestNameTopic` | reduce with custom logic |
| 7 | `allResourceUrls` | flatMap (flatten nested) |
| 8 | `topicsSortedByPriorityThenName` | Comparator chaining |
| 9 | `generateFibonacci` | Stream.iterate |
| 10 | `countLongNamesParallel` | parallelStream caveats |
| 11 | `streamLazyDemo` | Stream laziness |
| 12 | `sumOfSquares` | IntStream (avoid boxing) |

## Java Collections — Quick Reference

| Map | When | O() lookup |
|---|---|---|
| HashMap | Default — unordered | O(1) avg |
| TreeMap | Sorted by key, range queries | O(log n) |
| LinkedHashMap | Preserve insertion order | O(1) avg |
| ConcurrentHashMap | Multi-threaded access | O(1) avg, lock striping |

## Reactive Operators — Quick Reference

| Operator | Purpose |
|---|---|
| `map` | Sync transform 1-to-1 |
| `flatMap` | Async transform 1-to-1 (returns Mono/Flux) |
| `filter` | Keep matching elements |
| `switchIfEmpty` | Fallback when source empty |
| `onErrorResume` | Fallback on error |
| `zip` | Combine multiple Monos/Fluxes |
| `defer` | Lazy — useful for retries |
| `doOnNext` | Side effect, doesn't change stream |

## Common Spring Boot Interview Questions + Answers

### Q: "How does Spring DI actually work?"

> "Spring scans for beans (annotations like `@Component`, `@Service`), builds a dependency graph, and creates beans in dependency order. Constructor injection is preferred — Spring picks the constructor with `@Autowired` (or the only public one). Field injection works but breaks immutability and testability."

### Q: "What is `@Transactional` propagation?"

> "REQUIRED (default) — joins existing transaction or creates new. REQUIRES_NEW — always creates new, suspends existing. SUPPORTS — joins if exists, runs without if not. NESTED — savepoint within parent. The most common mistake: REQUIRED on a method called from a non-transactional context — silently runs without a transaction."

### Q: "How do you debug N+1 queries?"

> "Enable `hibernate.generate_statistics=true` and watch the query log. Solutions: `@EntityGraph` for known fetch paths, `JOIN FETCH` in JPQL for ad-hoc, `@BatchSize` for lazy collections. Detect early via test assertions on session statistics."

### Q: "Why Virtual Threads vs Reactive?"

> "Both achieve high concurrency. Virtual Threads = imperative code, easier to debug, blocking syntax but non-pinning during I/O. Reactive = functional pipelines, true backpressure, better for streaming/SSE. My rule: VTs for CRUD APIs, reactive for streaming/SSE/WebSocket. Both can coexist in the same app."

## During the Interview — Pro Tips

### When You Click a File

Walk through it top-to-bottom in 30-60 seconds. Don't read code line-by-line. Talk about:
- What it demonstrates
- Why this pattern over alternatives
- Production trade-offs

### When You Don't Know

Say: *"I haven't used that specific feature in production. Looking at it, I'd approach it like [X pattern I do know]. Let me think about how that would map..."*

Then reason out loud. Senior interviewers respect this far more than confident wrong answers.

### When You Do Know

Be specific. Use numbers from the code:
- "20 connection pool min, 5 idle"
- "Retry 3x with 500ms base + 2x backoff"
- "Circuit opens at 50% failure over last 20 calls"
- "Time limit 2s, bulkhead 10 concurrent calls"

## Pre-Interview Checklist (5 Min Before Call)

```bash
# 1. Verify project compiles
cd ~/projects/openclaw-dev-team/knowledge-hub/backend
./mvnw compile

# 2. Open these tabs in IntelliJ in this order:
#    - INTERVIEW_TALKING_POINTS.md (this file)
#    - StatsService.java (Streams demos)
#    - TopicService.java (Virtual Threads + Streams)
#    - NoteService.java (Reactive)
#    - ResourceService.java (Resilience4j)
#    - GlobalExceptionHandler.java
#    - application.yml (configuration)
```

## File Map for Reference

```
knowledge-hub/
├── README.md                              # Overall project doc
├── INTERVIEW_TALKING_POINTS.md            # This file
├── docker-compose.yml                     # Postgres + Mongo + Zipkin
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/example/knowledgehub/
│       ├── KnowledgeHubApplication.java   # @VirtualThreadConfig
│       ├── domain/
│       │   ├── jpa/
│       │   │   ├── Topic.java             # JPA entity, JOIN, Version
│       │   │   ├── Resource.java          # @ManyToOne owning side
│       │   │   ├── Category.java          # Enum
│       │   │   ├── Priority.java
│       │   │   └── Status.java
│       │   └── mongo/
│       │       └── Note.java              # MongoDB doc + embedded
│       ├── repository/
│       │   ├── jpa/
│       │   │   ├── TopicRepository.java   # 5 query styles
│       │   │   └── ResourceRepository.java
│       │   └── mongo/
│       │       └── NoteRepository.java    # ReactiveMongoRepository
│       ├── service/
│       │   ├── TopicService.java          # Blocking + VT + Streams
│       │   ├── NoteService.java           # Reactive Mono/Flux
│       │   ├── ResourceService.java       # WebClient + Resilience4j
│       │   └── StatsService.java          # 12 Streams patterns
│       ├── client/
│       │   └── ExternalArticleClient.java # WebClient
│       ├── controller/
│       │   ├── TopicController.java       # Traditional REST
│       │   ├── NoteController.java        # Reactive REST + SSE
│       │   └── ResourceController.java
│       └── exception/
│           ├── GlobalExceptionHandler.java
│           └── ResourceNotFoundException.java
└── frontend/                              # React 18 + TS (stub for now)
    └── README.md
```

Good luck. 🦞
