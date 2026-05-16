# Knowledge Hub — The Story of Topi & Noti

> A narrative walkthrough of how Topics and Notes flow through the system, why they use different paradigms, and how Docker volumes keep everything safe.

---

## The Setup — Imagine Knowledge Hub as a Tiny Digital Library

Knowledge Hub has hired **two librarians** to manage the user's interview prep materials:

**Topi** is the strict, methodical librarian.
- She handles **structured catalog cards** — every card has the same fields: name, category, priority, status. She files them in the **filing cabinet downstairs** (PostgreSQL).
- She works one customer at a time. Patient, polite, but blocking — if you ask her for 100 cards, she writes them out one by one, makes you wait at the counter.

**Noti** is the fast, flexible librarian.
- He handles **free-form scribbled notes** — every note is shaped differently: long ones, short ones, with attachments, with revision history, with whatever embedded data the writer wanted. He files them in **the loose-leaf box** (MongoDB).
- He works on many tasks at once. While the printer is warming up for one note, he starts the next one. Non-blocking, reactive — handles 100 customers in the time it takes Topi to handle 10.

Both librarians share **one basement vault** (Docker volumes) where their data sleeps at night, safe from container restarts.

---

## Part 1 — Topi's Story: The Sync Journey of a Topic

You're sitting at the dashboard. You click "**+ New Topic**" and type *"Spring Security & JWT"*, category *SPRING*, priority *P0*, status *MASTERED*.

Watch what happens, step by step. I'll point at every file.

### Stage 1 — The browser pops the request into a paper plane

`frontend/src/pages/TopicsPage.tsx` catches your click, calls a function in `frontend/src/hooks/useTopics.ts`. Specifically the `createTopic` mutation (a TanStack Query mutation). It bundles your form data into a JSON object and tells `frontend/src/lib/api.ts` to send it.

The api client opens an envelope: it stamps `Authorization: Bearer eyJhbGc…` (your JWT from `AuthContext`) on the front, writes `POST /api/topics` on the address line, attaches the JSON body, and throws it into the Vite dev server's mailbox.

### Stage 2 — Vite plays postman

Vite (your dev server at `localhost:3000`) sees the address starts with `/api`. The proxy config in `frontend/vite.config.ts` says *"any /api letter goes to localhost:8080"*. Vite forwards it. In production, this would be nginx or Cloud Run doing the same thing.

### Stage 3 — Spring Security checks your ID at the gate

Your paper plane arrives at Spring Boot. Before any controller sees it, Spring Security's filter chain inspects it. The order is in `backend/.../security/SecurityConfig.java`.

First filter: **CORS**. *"Are you from localhost:3000? OK, allowed."*

Second filter: **`JwtAuthenticationFilter`** (the file lives at `backend/.../security/JwtAuthenticationFilter.java`). It plucks the Bearer token from the header, calls `JwtService.extractUsername(token)` → `JwtService.isTokenValid(...)`. If the JWT is expired or signature is broken, the filter throws and the global handler returns 401. If valid, it loads your `User` from the DB via `UserDetailsService` and shoves your authentication into Spring's `SecurityContext` — meaning *"this request is now authenticated as sunil2 with role ADMIN."*

Third gate: **Authorization rules** in `SecurityConfig`. The line `requestMatchers(POST, "/api/topics/**").hasAnyRole("EDITOR", "ADMIN")` checks whether your roles include either. You're ADMIN. Pass.

### Stage 4 — Topi the librarian opens the envelope

The request lands at `backend/.../controller/TopicController.java` → method `createTopic(@Valid @RequestBody Topic topic)`.

The `@Valid` annotation triggers Bean Validation. Hibernate Validator looks at the `Topic` entity, sees `@NotBlank` on `name`, and checks: *"Did the customer fill in the name field? Yes."* Same for `@NotNull` on `category`. If anything was missing, it throws `MethodArgumentNotValidException`, your `GlobalExceptionHandler` catches it, you get a clean 400.

### Stage 5 — Topi calls the service

The controller hands the topic to `backend/.../service/TopicService.java` → `createTopic(topic)`.

The service is **annotated `@Transactional`** (or runs inside one). Spring opens a database transaction — think of it as Topi opening a fresh ledger page. Whatever happens next is one atomic unit: either everything succeeds or nothing changes.

The service does two things:
1. Calls `topicRepository.findByName("Spring Security & JWT")` — checks for duplicates. If found, throws `IllegalArgumentException("Topic with name '...' already exists")` → which becomes a 400 via the global handler. (You saw this earlier when Vector Embeddings already existed!)
2. Calls `topicRepository.save(topic)`.

### Stage 6 — JPA, Hibernate, and the actual SQL

`topicRepository.save(topic)` looks innocent but it's doing a lot.

`TopicRepository extends JpaRepository<Topic, Long>` (in `backend/.../repository/jpa/`). Spring Data generated the implementation at startup. The `save` method:

1. Checks if the entity has an ID. New topic, no ID → calls `EntityManager.persist(topic)`.
2. Hibernate translates the entity into SQL. Looks at `@Table(name="topics")`, looks at every field with `@Column`. Sees `@PrePersist` → calls Topic's `onCreate()` lifecycle method which sets `createdAt = Instant.now()`.
3. Builds: `INSERT INTO topics (name, category, priority, status, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?, ?, 0) RETURNING id;`
4. The JDBC driver (HikariCP connection pool) sends it to PostgreSQL.
5. PostgreSQL writes the row to disk. Topi marks the ledger.
6. The auto-generated `id` (let's say `13`) flows back. Hibernate sets `topic.setId(13)`.

When the service method returns, Spring **commits the transaction** — Topi closes the ledger page permanently. The transaction is now durable.

### Stage 7 — The reply travels back

The controller wraps the saved Topic in a `200 OK` (or `201 Created`) HTTP response. Spring's `MappingJackson2HttpMessageConverter` serializes it back to JSON. The response flies back through the filter chain, out the network interface, through Vite, into the browser.

### Stage 8 — TanStack Query updates the UI

`useTopics.ts` mutation has `onSuccess: () => queryClient.invalidateQueries(['topics'])`. This tells TanStack Query *"the cached topic list is stale, refetch it."* The `useQuery` watching `['topics']` automatically re-fires the GET, the table re-renders with the new row at the top.

You see your new topic appear. **Total elapsed time: ~50-200ms.**

### What was sync about this?

Every step from Stage 5 to Stage 6 was **blocking**. The thread that handled your HTTP request was *parked* waiting for Postgres to finish. If Postgres took 100ms, that thread couldn't do anything else for 100ms. Tomcat's default thread pool is 200 threads — so under heavy load, you can handle 200 concurrent topic-creates, no more. Thread #201 has to wait.

This is fine for typical CRUD apps — the simplicity is worth it. JPA/Hibernate is the most well-understood ORM in the Java world.

---

## Part 2 — Noti's Story: The Async Journey of a Note

Now you click into a topic and add a note: *"BCrypt strength 12 takes ~250ms — that's the whole point, slow on purpose to defeat brute force."*

Same browser, same JWT, similar URL pattern (`POST /api/notes`) — but the underlying machinery is **completely different**.

### Stages 1-3 — Identical to Topi

Browser, Vite proxy, Spring Security filter chain. Nothing changes. JWT validated, request authorized, lands at `backend/.../controller/NoteController.java`.

### Stage 4 — Reactive controller signature

Look at the method signature:

```java
public Mono<Note> createNote(@RequestBody Note note) { ... }
```

Notice the return type: **`Mono<Note>`**, not `Note`. A `Mono<T>` is *"a promise of zero or one T, that may not exist yet."* Like a JavaScript Promise but with backpressure support.

The controller doesn't do the work itself. It builds a **pipeline**:

```java
return noteService.create(note)
        .doOnNext(saved -> log.info("Saved note {}", saved.getId()))
        .map(saved -> /* maybe transform */ saved);
```

Importantly: **none of this code has executed yet.** A Mono is a *recipe*, not a *meal*. The recipe gets handed back up to Spring WebFlux, which subscribes to it. Subscription is what triggers actual execution.

### Stage 5 — The reactive service

`backend/.../service/NoteService.java` → `create(note)`:

```java
public Mono<Note> create(Note note) {
    note.setCreatedAt(Instant.now());
    return noteRepository.save(note)            // returns Mono<Note>
            .doOnSuccess(saved -> ... )         // side effect, doesn't block
            .onErrorMap(DuplicateKeyException.class,
                ex -> new IllegalArgumentException("Note title taken"));
}
```

`noteRepository` is a **`ReactiveMongoRepository<Note, String>`** (in `backend/.../repository/mongo/`). Its `save()` returns a `Mono<Note>`.

### Stage 6 — Mongo's reactive driver

The reactive Mongo driver opens a **non-blocking** connection. It sends the BSON-serialized note over the wire, then **immediately releases the thread**. The thread that was holding your HTTP request? It's freed up to handle other requests. It's like leaving a number with the doctor's office — you don't sit in the waiting room, you go shopping. When MongoDB finishes the insert, it pings back via callback, and a thread picks up the response.

This is **the magic of WebFlux + reactive drivers**: a single thread can juggle thousands of in-flight requests because no thread is ever blocked on I/O.

### Stage 7 — The response flies back

Once Mongo pings back with the saved doc (with auto-generated `_id`), the Mono completes, the pipeline runs `.doOnSuccess` (logs), and Spring WebFlux serializes the result to JSON and writes it to the response.

### Stage 8 — Frontend handles it identically

The frontend doesn't know or care that the backend is reactive. It got a JSON response. TanStack Query updates the cache. UI rerenders. Same as Topic.

### What was async about this?

The thread handling your request was **never blocked**. It dropped your work into the reactive Mongo driver, went off to handle other requests, and came back when Mongo signaled completion. With 200 threads, you can have **thousands** of concurrent in-flight notes — limited only by Mongo's capacity, not by Java thread count.

---

## Part 3 — Why Two Paradigms? The Real Reason.

Imagine you're scaling Knowledge Hub to 1 million users. Two scenarios:

**Scenario A — Heavy CRUD on Topics.** Users mostly create/edit topics. Topics are well-structured and small. JPA + sync is fine; the simplicity is worth more than the throughput. You'd scale by adding more app server instances behind a load balancer.

**Scenario B — Heavy Note-streaming.** Users open a topic and want to see ALL notes streamed live as they're added by other users. WebFlux + reactive Mongo lets you handle 10x more concurrent SSE/WebSocket connections per JVM, because reactive doesn't tie up a thread per connection.

The real-world rule: **use reactive when you have lots of slow I/O concurrency** (long-polling, streaming, fan-out to many slow services). **Use blocking JPA when you have CPU-bound or simple CRUD** (the cognitive overhead of Mono/Flux isn't worth it for a basic save).

Knowledge Hub demonstrates BOTH **on purpose** so you can talk about the trade-off with confidence.

### Interview-ready trade-off table

| Aspect              | Topic (JPA, sync)              | Note (Reactive, async)              |
|---------------------|--------------------------------|--------------------------------------|
| Return type         | `Topic`                        | `Mono<Note>` / `Flux<Note>`         |
| Thread model        | Thread-per-request (blocking)  | Event loop (non-blocking)           |
| Repository base     | `JpaRepository`                | `ReactiveMongoRepository`           |
| DB driver           | JDBC (blocking)                | Reactive Streams (non-blocking)     |
| Error handling      | try/catch, exceptions          | `.onErrorMap`, `.onErrorResume`     |
| Transactions        | `@Transactional` (simple)      | `TransactionalOperator` (more verbose) |
| Best for            | CRUD, simple APIs              | Streaming, fan-out, lots of I/O     |
| Debugging           | Easy — flat stack trace        | Harder — operators don't show in stack |
| Library coverage    | All Java libs work             | Must use reactive-friendly libs    |

### The 30-second interview answer

> *"Knowledge Hub uses both blocking JPA and reactive WebFlux deliberately. JPA on Postgres for Topic CRUD because relational data with simple operations doesn't benefit enough from reactive to justify the complexity — debugging blocking code is straightforward and JPA's ecosystem is mature. Reactive WebFlux on Mongo for Notes because notes are conceptually a stream — many users editing many docs concurrently — and WebFlux scales connection-count without scaling thread-count. The mental model: blocking is the default, reactive is a tool for specific high-concurrency I/O patterns."*

---

## Part 4 — The Basement Vault: Docker Volumes

The story so far assumed Postgres and MongoDB just *exist*. Let's pull back the curtain.

When you ran `docker-compose up -d postgres mongodb`, Docker started two containers from images. **Containers are ephemeral** — kill the container, the data inside the container's filesystem vanishes. That's a feature for stateless apps, but a disaster for databases.

That's where **volumes** come in. Look at `knowledge-hub/docker-compose.yml`:

```yaml
postgres:
  image: postgres:16-alpine
  volumes:
    - postgres-data:/var/lib/postgresql/data    # ← named volume
  ...

mongodb:
  image: mongo:7.0
  volumes:
    - mongo-data:/data/db                       # ← named volume

volumes:
  postgres-data:                                # ← declared at top level
  mongo-data:
```

### What's actually happening

A **Docker volume** is a directory on your Mac (managed by Docker Desktop or OrbStack) that's *mounted* into the container at the specified path.

For Postgres: the directory `/var/lib/postgresql/data` *inside the container* is actually a slice of your Mac's disk. Postgres writes WAL logs, table data, indexes — all of it lands on your Mac, not inside the container.

```
Your Mac:
/var/lib/docker/volumes/knowledge-hub_postgres-data/_data
                    │
                    └─── (this is the postgres data directory)
                         on container startup, mounted as /var/lib/postgresql/data
```

When you stop the container (`docker-compose stop postgres`), the container is destroyed, but the **volume persists**. Start the container again — it sees the existing data directory, runs WAL replay, and you're back where you left off. **Your seeded users and 12 topics survive container restarts.**

### Three kinds of volume mounts

For interview-recall, know the difference:

1. **Named volume** (what we use):
   ```yaml
   volumes:
     - postgres-data:/var/lib/postgresql/data
   ```
   Docker manages the location. Best for production — portable, named, lifecycle-managed.

2. **Bind mount** (used for `prometheus.yml`):
   ```yaml
   prometheus:
     volumes:
       - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
   ```
   Maps a specific file/folder from your project into the container. Good for **config files** you edit and want the container to pick up. The `:ro` makes it read-only.

3. **tmpfs** (in-memory):
   ```yaml
   volumes:
     - type: tmpfs
       target: /tmp
   ```
   Pure RAM, vanishes on stop. Useful for tests, scratch space.

### What survives, what doesn't (memorize)

| What you care about              | Survives `docker-compose stop` ? | Survives `docker-compose down` ? | Survives `docker-compose down -v` ? |
|----------------------------------|----------------------------------|-----------------------------------|--------------------------------------|
| Container processes              | No (containers stop)             | No (containers removed)           | No                                   |
| Container filesystem (no volume) | Yes (paused state)               | No (deleted)                      | No                                   |
| **Named volumes (your DB data)** | **Yes**                          | **Yes**                           | **NO — DELETED**                     |
| Bind mounts (your project files) | Yes                              | Yes                               | Yes (lives on your Mac)              |

**The dangerous command:** `docker-compose down -v` — the `-v` flag wipes named volumes. **This will delete your seeded topics and users**. Memorize this — it's a classic interview gotcha.

### Inspect your volumes right now

```bash
# List Docker volumes
docker volume ls | grep knowledge

# See size + mount point
docker system df -v | grep knowledge

# Even peek inside (Linux/Mac)
docker run --rm -v knowledge-hub_postgres-data:/data alpine du -sh /data
```

### Why this matters for the interview

> *"Stateful containers — anything with a database — must have a named volume. Forget the volume and a routine `docker-compose down` wipes prod data. In production we don't even use compose; we use managed services like RDS, Cloud SQL, MongoDB Atlas — Docker volumes are a local-dev concept. But knowing the pattern means you can spin up identical infra anywhere with one command."*

---

## Part 5 — Putting Both Stories Together

Here's the **complete request lifecycle** for both Topic and Note, side by side:

```
                            BROWSER (React)
                                    │
                          POST /api/topics    POST /api/notes
                                    │                │
                                    ▼                ▼
                              VITE PROXY (3000 → 8080)
                                    │
                                    ▼
                       SPRING SECURITY FILTER CHAIN
                          (CORS → JWT → AuthZ rules)
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
            TopicController                   NoteController
            (returns Topic)                   (returns Mono<Note>)
                    │                               │
                    ▼                               ▼
            TopicService                      NoteService
            (@Transactional, blocking)        (Mono pipeline, non-blocking)
                    │                               │
                    ▼                               ▼
            TopicRepository                   NoteRepository
            (JpaRepository)                   (ReactiveMongoRepository)
                    │                               │
                    ▼                               ▼
              JDBC + HikariCP              MongoDB Reactive Driver
              (blocking)                   (non-blocking, callback-based)
                    │                               │
                    ▼                               ▼
              ┌──────────────┐              ┌──────────────┐
              │ PostgreSQL 16 │              │  MongoDB 7   │
              │              │              │              │
              │  topics      │              │  notes       │
              │  users       │              │  collection  │
              │  user_roles  │              │              │
              └──────┬───────┘              └──────┬───────┘
                     │                              │
                     ▼                              ▼
              ┌──────────────────────────────────────────┐
              │       BASEMENT VAULT — DOCKER VOLUMES    │
              │                                          │
              │  postgres-data  ◄─── persists Topi's     │
              │                      filing cabinet      │
              │                                          │
              │  mongo-data     ◄─── persists Noti's     │
              │                      loose-leaf box      │
              └──────────────────────────────────────────┘
```

---

## Part 6 — Quick Recall Cheat Sheet (memorize for tomorrow)

**Topic = JPA + Postgres + sync** because:
- Structured, relational, simple CRUD
- Use when you want simplicity over raw throughput
- Thread-per-request, `@Transactional`, return type is the entity

**Note = Reactive WebFlux + Mongo + async** because:
- Document-shaped, varying structure, embedded versions and attachments
- Use when concurrent I/O density matters
- Event loop, return type is `Mono<T>` or `Flux<T>`

**Volumes = Docker's basement vault** because:
- Containers are ephemeral; data must outlive them
- Named volumes persist across stop/down
- `docker-compose down -v` is the dangerous one — wipes data

**The story you tell tomorrow:**
*"My Knowledge Hub uses two storage paradigms deliberately — JPA on Postgres for Topics demonstrates the standard CRUD pattern that 80% of business apps need. Reactive WebFlux on Mongo for Notes demonstrates the high-concurrency I/O pattern for streaming/fan-out scenarios. Docker volumes ensure the data survives container restarts in dev — in production this would be Cloud SQL and MongoDB Atlas. The same JWT-secured filter chain protects both."*

That single paragraph is your interview answer to "**Walk me through your project's architecture.**"
