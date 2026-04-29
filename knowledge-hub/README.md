# Knowledge Hub — Interview Prep Tracker

**Reference project demonstrating modern Spring Boot 3.3 + Java 21 + React 18 patterns** in a real, useful application: track your interview prep topics, notes, and resources.

## What It Demonstrates

| Concept | Where to See It | File |
|---|---|---|
| **JPA / Relational (PostgreSQL)** | `Topic` entity with relations | `domain/jpa/Topic.java` |
| **MongoDB / Document** | `Note` with embedded versions, tags | `domain/mongo/Note.java` |
| **JPA Relationships** | `Topic` ↔ `Resource` one-to-many | `domain/jpa/Resource.java` |
| **Spring Data JPA** | Derived queries, `@Query`, projections | `repository/jpa/TopicRepository.java` |
| **Spring Data MongoDB** | Reactive queries, aggregation | `repository/mongo/NoteRepository.java` |
| **Reactive WebFlux (Mono/Flux)** | Note service end-to-end async | `service/NoteService.java` |
| **Traditional Servlet + Virtual Threads** | Topic service uses VT executor | `service/TopicService.java` |
| **WebClient (modern HTTP client)** | External article metadata | `client/ExternalArticleClient.java` |
| **RestClient (Java 17+ alternative)** | Synchronous calls | `client/RestClientExample.java` |
| **Resilience4j** | Circuit breaker, retry, bulkhead | `service/ResourceService.java` |
| **Micrometer + Prometheus** | Custom metrics + tracing | All services |
| **Bean Validation** | DTOs with `@NotBlank`, `@Size` | `dto/TopicDto.java` |
| **Global Exception Handling** | `@RestControllerAdvice` | `exception/GlobalExceptionHandler.java` |
| **Java Streams + Collectors** | Filtering, grouping, partitioning | `service/StatsService.java` |
| **Java Collections deep examples** | HashMap, TreeMap, ConcurrentHashMap | `service/StatsService.java` |
| **DTO Pattern + MapStruct alternative** | Records + manual mapping | `dto/` |
| **Spring profiles** | dev/test/prod configs | `application.yml` |
| **Docker Compose multi-service** | App + Postgres + MongoDB | `docker-compose.yml` |
| **React 18 + TypeScript + TanStack Query** | Modern frontend | `frontend/` |

## Architecture

```
┌──────────────────────────────────────────────┐
│  React 18 Frontend (Vite + Tailwind)         │
│  ┌────────┐ ┌────────┐ ┌──────────────┐      │
│  │ Topics │ │  Notes │ │  Resources   │      │
│  └────┬───┘ └────┬───┘ └──────┬───────┘      │
└───────┼──────────┼─────────────┼─────────────┘
        ↓          ↓             ↓
   REST/JSON  REST/SSE       REST + Resilience4j
        ↓          ↓             ↓
┌──────────────────────────────────────────────┐
│  Spring Boot 3.3 Backend (Java 21)           │
│  ┌──────────────┐ ┌──────────────┐ ┌─────────┐
│  │ TopicService │ │ NoteService  │ │Resource │
│  │ (blocking +  │ │ (Reactive    │ │ Service │
│  │  Virtual Thr)│ │  Mono/Flux)  │ │+ WebCli)│
│  └──────┬───────┘ └──────┬───────┘ └────┬────┘
│         ↓                ↓              ↓
│  ┌──────────────┐ ┌──────────────┐ ┌─────────┐
│  │  PostgreSQL  │ │   MongoDB    │ │External │
│  │  (JPA)       │ │  (Reactive)  │ │  API    │
│  └──────────────┘ └──────────────┘ └─────────┘
└──────────────────────────────────────────────┘
```

## Quick Start

```bash
# Start all dependencies
docker-compose up -d

# Run backend
cd backend
./mvnw spring-boot:run

# In another terminal, run frontend
cd frontend
npm install
npm run dev

# Backend: http://localhost:8080
# Frontend: http://localhost:3000
# Postgres: localhost:5432 (knowledge_hub / postgres)
# MongoDB: localhost:27017 (knowledge_notes)
# Prometheus metrics: http://localhost:8080/actuator/prometheus
```

## File Map (Backend)

```
backend/src/main/java/com/example/knowledgehub/
├── KnowledgeHubApplication.java         # Main + Virtual Threads config
├── config/
│   ├── WebClientConfig.java             # WebClient bean with timeout/connection pool
│   ├── Resilience4jConfig.java          # Circuit breaker + retry config
│   └── VirtualThreadConfig.java         # @Async + Virtual Thread executor
├── domain/
│   ├── jpa/
│   │   ├── Topic.java                   # JPA @Entity (PostgreSQL)
│   │   ├── Resource.java                # JPA @Entity with @ManyToOne
│   │   └── Category.java                # Enum
│   └── mongo/
│       └── Note.java                    # MongoDB @Document
├── repository/
│   ├── jpa/
│   │   ├── TopicRepository.java         # Derived queries + @Query
│   │   └── ResourceRepository.java
│   └── mongo/
│       └── NoteRepository.java          # ReactiveMongoRepository
├── service/
│   ├── TopicService.java                # Blocking + Virtual Threads
│   ├── NoteService.java                 # Reactive Mono/Flux
│   ├── ResourceService.java             # WebClient + Resilience4j
│   └── StatsService.java                # Streams + Collections demos
├── controller/
│   ├── TopicController.java             # Traditional REST
│   ├── NoteController.java              # Reactive REST + SSE
│   └── ResourceController.java          # REST with circuit breaker
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── ValidationException.java
├── dto/
│   ├── TopicDto.java                    # Request/response records
│   ├── NoteDto.java
│   └── ResourceDto.java
└── client/
    └── ExternalArticleClient.java       # WebClient + Resilience4j
```

## Interview Talking Points by Concept

See `INTERVIEW_TALKING_POINTS.md` for detailed Q&A prep on each concept.

## How to Grow This Later

The structure naturally extends to:
- **AI features:** Add `@aiml` integration → embeddings on notes → semantic search
- **Authentication:** Spring Security 6 + JWT
- **Real-time:** WebSocket for collaborative note editing
- **Multi-user:** Add User entity, ownership, sharing
- **Mobile:** React Native client reusing the same API
- **MCP integration:** Expose this as an MCP server for Claude Desktop

This is your **personal portfolio piece + interview reference + actual learning tracker** in one project.

## License

Personal use. Generated as interview prep reference.
