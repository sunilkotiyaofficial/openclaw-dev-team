# Knowledge Hub — Complete Interview Guide

**Use this as your interview script.** Memorize the opening pitch + the 10 killer one-liners. Have key files open in IntelliJ tabs ready to walk through.

---

## Part 1: The Opening Pitch (90 Seconds)

When asked "tell me about a project you've built":

> "I built **Knowledge Hub** — a Spring Boot 3.3 + Java 21 application that demonstrates production patterns I use daily. It's a learning tracker with three entities split across two databases:
>
> - **Topic** and **Resource** in PostgreSQL via JPA — for structured, relational data
> - **Note** in MongoDB via reactive driver — for flexible content with embedded version history
>
> On the backend, I have **two service flavors side-by-side**: TopicService is traditional blocking with Java 21 Virtual Threads — gives you reactive-like throughput with imperative code. NoteService is fully reactive Mono/Flux. Both achieve high concurrency, just different programming models.
>
> External calls go through **WebClient with Resilience4j** — circuit breaker, retry, time limiter, bulkhead all stacked. Graceful degradation via fallback methods.
>
> **Authentication** is JWT-based with role-based authorization — USER, EDITOR, ADMIN — using Spring Security 6. Passwords BCrypt-hashed. Stateless sessions for horizontal scaling.
>
> Everything is observable — Micrometer metrics to Prometheus, distributed tracing via OpenTelemetry to Zipkin, structured JSON logging.
>
> React 18 + TypeScript frontend with TanStack Query for state management.
>
> Want me to walk through any part?"

---

## Part 2: The Story — Why I Built It This Way

### The Problem

> "I was overwhelmed managing my own interview prep across many topics — Kafka, Spring, AI/ML, Apache Camel. I had docs scattered across 5 places. So I built a tool that:
> - Tracks topics with priority and status
> - Stores rich Markdown notes with version history
> - Links external resources (articles, videos)
> - Multi-user with role-based access (so I could share with study buddies later)"

### The Architecture Decisions

> "Three big calls early:
>
> **1. Why split data across PostgreSQL + MongoDB?**
> Topics have structured queries — find by category, status, priority. Pure relational fit. Notes are unstructured — Markdown body, embedded version history, attachments. The version history is naturally a sub-document — it always travels with the note. PostgreSQL JSONB could work, but MongoDB's reactive driver gives true backpressure-aware streaming.
>
> **2. Why both Virtual Threads AND Reactive in the same app?**
> Topics are CRUD-heavy with simple flows — Virtual Threads give me imperative simplicity with no thread-pool exhaustion. Notes have streaming use cases (live update feed via SSE) where reactive's true backpressure matters. Use the right tool for each.
>
> **3. Why JWT over server-sessions?**
> Stateless = horizontal scalability. No session store, any node validates any token. Trade-off: can't revoke before expiry without a denylist. Mitigated by short access token TTL — 15 minutes — and longer refresh tokens (7 days)."

---

## Part 3: Spring Security Deep Dive

### "Walk me through your security implementation."

**Open `SecurityConfig.java`. Talk through these 5 elements:**

> "Spring Security 6 with JWT-based stateless authentication.
>
> 1. **CSRF disabled** — only matters for cookie-based sessions. JWTs in headers don't need CSRF protection.
>
> 2. **CORS enabled** — explicit allow-list for the React frontend on port 3000 and 5173.
>
> 3. **Authorization rules** — three tiers:
>    - GET endpoints → any authenticated role (USER, EDITOR, ADMIN)
>    - POST/PUT → EDITOR or ADMIN
>    - DELETE → ADMIN only
>
> 4. **Stateless session** — `SessionCreationPolicy.STATELESS`. Spring won't create an HTTP session.
>
> 5. **Custom JWT filter** — runs BEFORE Spring's standard `UsernamePasswordAuthenticationFilter`. Extracts the Bearer token, validates the signature and expiry, sets the SecurityContext."

### "How do you handle login?"

**Open `AuthController.java` → `login` method:**

> "The flow:
> 1. Client POSTs username + password
> 2. `authenticationManager.authenticate(...)` triggers Spring's `DaoAuthenticationProvider`
> 3. That provider calls my custom `UserDetailsService` which queries Postgres via `UserRepository.findByUsername`
> 4. Spring compares the submitted password against the BCrypt hash using `BCryptPasswordEncoder.matches()`
> 5. On success, I issue access + refresh JWTs
> 6. On failure, Spring throws `BadCredentialsException` → my GlobalExceptionHandler returns 401"

### "How does JWT validation work on each request?"

**Open `JwtAuthenticationFilter.java`:**

> "Every request hits this filter once.
>
> 1. Extract Authorization header — bail early if missing or doesn't start with 'Bearer '
> 2. Strip the 'Bearer ' prefix
> 3. Parse token to extract username (this verifies the signature implicitly)
> 4. Load UserDetails from DB
> 5. Validate token: signature OK + not expired + username matches
> 6. Build `UsernamePasswordAuthenticationToken` with the user's authorities (roles)
> 7. Set `SecurityContextHolder` — downstream filters and `@PreAuthorize` see the authenticated user
>
> Notice — the filter NEVER fails the request itself. If validation fails, we just pass through without setting auth. Later filters return 401 if the endpoint requires auth."

### "How do you protect against common attacks?"

| Attack | Defense |
|---|---|
| **SQL injection** | JPA parameterized queries (Spring Data) — never string concatenation |
| **Brute force on login** | BCrypt strength=12 (~250ms per hash). Add Bucket4j rate limiting in production. |
| **JWT tampering** | HMAC-SHA256 signature. Tampered token fails parse. |
| **Token replay (stolen JWT)** | Short access token TTL (15 min) + HTTPS only |
| **CSRF** | Not applicable — JWT in header, not cookie |
| **XSS** | Spring auto-escapes JSON. Frontend uses React's auto-escaping. |
| **Mass assignment** | DTOs (records) instead of binding requests directly to entities |
| **Privilege escalation on signup** | Self-signup ignores requested EDITOR/ADMIN — only USER role assigned |

### "BCrypt vs Argon2 vs SHA-256?"

> "Never SHA-256 alone — it's fast, designed for fingerprinting, not password hashing. An attacker brute-forces SHA-256 at billions per second on a GPU.
>
> BCrypt and Argon2 are both adaptive — they're deliberately slow and memory-hard. BCrypt is mature, widely supported, with strength factor tunable (I use 12 — about 250ms per hash). Argon2 is the modern winner of the Password Hashing Competition, even more resistant to GPU attacks.
>
> For new projects, Argon2id is the right call. BCrypt is the safe pragmatic choice — especially with Spring Security's first-class support."

### "Why JWT and not server-sessions?"

> "Three reasons:
> 1. **Stateless scalability** — no session store, any node validates any token
> 2. **Mobile/SPA friendliness** — no cookies needed, headers travel everywhere
> 3. **Microservice federation** — same JWT verified across N services without shared session DB
>
> Trade-off: revocation is hard. JWTs are valid until expiry. Mitigations:
> - Short access token TTL (15 min)
> - Refresh token rotation
> - Optional Redis denylist for hard revocation"

### "Walk me through your role hierarchy."

```
Role.USER     → read-only access
Role.EDITOR   → USER + can create/update topics, notes
Role.ADMIN    → EDITOR + can delete + manage user roles
```

> "Three patterns enforce this:
>
> **Pattern 1: URL-based at the filter chain** (in SecurityConfig):
> ```java
> .requestMatchers(POST, '/api/topics/**').hasAnyRole('EDITOR', 'ADMIN')
> .requestMatchers(DELETE, '/api/**').hasRole('ADMIN')
> ```
>
> **Pattern 2: Method-level via `@PreAuthorize`** (in AuthController):
> ```java
> @PreAuthorize(\"hasRole('ADMIN')\")
> public UserInfo assignRole(...)
> ```
>
> **Pattern 3: Programmatic** in service code if logic depends on data:
> ```java
> if (!user.getRoles().contains(Role.ADMIN)
>     && !resource.getOwnerId().equals(user.getId())) { throw new AccessDeniedException(); }
> ```
>
> Layered defense — even if URL rule is bypassed, method-level catches it."

### "OAuth2 vs JWT vs SAML?"

> "These solve different problems:
>
> **JWT** = a token format. Self-contained, signed, stateless.
>
> **OAuth2** = a delegation framework. 'Let App X access User Y's resource at Service Z.' OAuth2 commonly USES JWTs as access tokens but doesn't have to.
>
> **SAML** = older XML-based federation, common in enterprise SSO (corporate IDP → multiple apps).
>
> My Knowledge Hub uses JWT for its own auth. If I wanted users to sign in with Google/Okta, I'd add Spring Security OAuth2 Client. The pattern: Google issues a JWT (id_token), my backend verifies the signature against Google's public JWKS endpoint, creates/maps a local User record, then issues my own JWT for subsequent calls."

---

## Part 4: User Lifecycle Story

### "Walk me through what happens when a new user signs up."

**Open `AuthController.register` → `UserService.register`:**

> "1. POST /api/auth/register — Bean Validation kicks in via `@Valid`. The RegisterRequest record has @NotBlank, @Email, @Size, and a regex Pattern enforcing strong passwords (upper + lower + digit, min 8 chars).
>
> 2. UserService.register checks uniqueness — username and email both must be unique. Throws IllegalArgumentException → 400 if conflict.
>
> 3. Password is hashed with BCryptPasswordEncoder.encode() — strength 12. Plain password never persists.
>
> 4. Default role is USER. Even if the request asks for ADMIN, we ignore it on signup — privilege escalation is impossible via the public endpoint. ADMIN must explicitly grant roles via `/users/{id}/roles/{role}`.
>
> 5. JPA @PrePersist sets createdAt timestamp.
>
> 6. After persist, immediate auto-login: load UserDetails, generate access + refresh JWTs, return AuthResponse.
>
> Net result: client receives tokens and user info in one round-trip. Authenticated for next request."

### "What happens when an existing user logs in?"

> "Same flow but with credentials check:
>
> 1. POST /api/auth/login with username + password
> 2. authenticationManager.authenticate triggers DaoAuthenticationProvider
> 3. Provider calls my UserDetailsService.loadUserByUsername — DB lookup
> 4. BCryptPasswordEncoder.matches the submitted password against the stored hash
> 5. On success: build Authentication object with user's authorities
> 6. UserService.recordLogin updates lastLoginAt
> 7. Generate JWTs, return AuthResponse
> 8. Client stores tokens (memory + httpOnly cookie for refresh — never localStorage for tokens in production)"

### "Why don't you store JWT in localStorage?"

> "XSS attack vector. If anything injects JS into your page — a vulnerable npm dep, a stored XSS bug — the attacker can read localStorage and exfiltrate the token.
>
> Better: keep access token in memory (variable), store refresh token in an httpOnly cookie with Secure + SameSite=Strict flags. Cookie isn't accessible to JS, mitigating XSS. The refresh-on-page-load pattern means you don't lose access on refresh.
>
> The Knowledge Hub frontend stub uses memory storage for the access token + httpOnly cookie for the refresh token."

---

## Part 5: Follow-up Questions Drill

### "How would you implement single sign-on?"

> "Add Spring Security OAuth2 Client. Configure Google or Okta as a provider. Flow:
> 1. User clicks 'Sign in with Google'
> 2. Redirect to Google with our client_id, scopes (openid email profile)
> 3. User authenticates with Google
> 4. Google redirects back with auth code
> 5. Spring exchanges code for id_token + access_token at Google's token endpoint
> 6. Spring verifies id_token signature against Google's JWKS public keys
> 7. My OAuth2UserService either finds an existing User by email or creates one
> 8. Issue MY own JWT — application code below this layer doesn't change"

### "How would you implement password reset?"

> "Add a PasswordResetToken entity (separate from User) — has user_id, token, expires_at, used flag.
>
> Flow:
> 1. User requests reset → generate cryptographically random token (UUID + secure random), store hashed in DB
> 2. Email user a link with the raw token: `https://app/reset?token=...`
> 3. User clicks → POST /api/auth/reset with token + new password
> 4. Backend hashes the submitted token, looks up by hash, checks expires_at, checks used=false
> 5. Hashes new password, updates user, marks token used=true
>
> Critical: never email the password itself. Tokens single-use only. Constant-time comparison to prevent timing attacks."

### "How would you implement multi-factor auth?"

> "TOTP-based 2FA (Google Authenticator).
>
> 1. Add `User.totpSecret` (encrypted at rest)
> 2. POST /api/auth/setup-2fa generates a secret, returns it as QR code (otpauth:// URI)
> 3. User scans, enters first 6-digit code → backend verifies via TOTP library (Google Authenticator algorithm = RFC 6238)
> 4. On successful verification, mark `User.mfaEnabled = true`
> 5. Login flow now requires step-up: after username/password, prompt for TOTP code BEFORE issuing JWT
> 6. Recovery codes — generate 10 single-use backup codes at setup, hash and store"

### "How would you handle JWT secret rotation?"

> "Rolling rotation:
> 1. Store multiple keys (current + previous) in Vault/Secret Manager
> 2. JWT header includes 'kid' (key id)
> 3. Sign new tokens with current key
> 4. Validate accepts tokens signed with current OR previous key
> 5. After max-token-lifetime passes, retire previous key from validation
>
> This way you can rotate without invalidating live sessions. Production secret managers like AWS Secret Manager + RDS rotation handle this lifecycle."

### "How does role-based authorization work in @PreAuthorize?"

> "@PreAuthorize is method-level RBAC. Spring evaluates the SpEL expression BEFORE the method body runs. Examples:
>
> - `hasRole('ADMIN')` — current user has ROLE_ADMIN authority
> - `hasAnyRole('EDITOR', 'ADMIN')` — at least one
> - `hasAuthority('topics:write')` — fine-grained authority (without ROLE_ prefix)
> - `#userId == authentication.principal.id` — runtime ownership check
> - `@securityService.canEdit(#topicId, authentication)` — custom Spring bean call
>
> The runtime check is the killer feature: 'You can edit YOUR own topic but not someone else's,' enforced declaratively."

---

## Part 6: 10 Killer One-Liners (Memorize)

1. **"Stateless JWT auth = horizontal scalability without sticky sessions; trade-off is harder revocation, mitigated with short access tokens."**

2. **"BCrypt strength 12 is ~250ms per hash — infeasible to brute-force, fast enough for login UX."**

3. **"JWT validation: signature, expiry, issuer, subject. The signature is what makes it tamper-proof."**

4. **"Spring Security filter chain: my JWT filter runs BEFORE UsernamePasswordAuthenticationFilter — we authenticate from the token, skipping form-based auth."**

5. **"Defense in depth: URL-based rules at the filter, method-level @PreAuthorize, programmatic checks in services for ownership."**

6. **"Refresh tokens are how you get long sessions without long-lived attack windows — short-lived access tokens (15 min) + opaque refresh tokens (7 days)."**

7. **"Never store JWTs in localStorage — XSS attack vector. Memory + httpOnly cookie for refresh."**

8. **"Privilege escalation defense: signup endpoint ignores requested ADMIN/EDITOR roles — only an existing ADMIN can grant them."**

9. **"OAuth2 is a delegation framework that often USES JWTs; SAML is older XML-based; JWT is just a token format. Don't conflate them."**

10. **"Spring's @PreAuthorize evaluates SpEL expressions against the current user — supports role checks, ownership checks, custom bean calls."**

---

## Part 7: Files to Open in IntelliJ Tabs

In this exact order before the interview:

1. `INTERVIEW_GUIDE_SECURITY_AND_OVERALL.md` (this file)
2. `INTERVIEW_TALKING_POINTS.md` (the original Java/Spring guide)
3. `SecurityConfig.java` ⭐ — most likely to walk through
4. `JwtAuthenticationFilter.java`
5. `JwtService.java`
6. `User.java` (UserDetails implementation)
7. `AuthController.java`
8. `StatsService.java` (Streams patterns)
9. `TopicService.java` (Virtual Threads)
10. `NoteService.java` (Reactive)
11. `ResourceService.java` (Resilience4j)
12. `Topic.java` (JPA entity)

---

## Part 8: Build & Run Commands

```bash
# Start infra
cd ~/projects/openclaw-dev-team/knowledge-hub
docker-compose up -d

# Run backend
cd backend
./mvnw spring-boot:run

# Test signup
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username":"alice",
    "email":"alice@example.com",
    "password":"AliceTest1"
  }'

# Test login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"AliceTest1"}'

# Use the access token
TOKEN="<paste-access-token-here>"
curl http://localhost:8080/api/topics \
  -H "Authorization: Bearer $TOKEN"

# Try DELETE (will 403 — alice is just USER)
curl -X DELETE http://localhost:8080/api/topics/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

## Part 9: The "Why" Story (Defends Architecture Choices)

When the interviewer asks "Why this approach?" — here's your answer for each major decision:

| Decision | Why |
|---|---|
| **Spring Security 6 over JEE / custom auth** | Industry standard, well-documented, integrates with all Spring features; custom auth is reinventing wheels with bugs |
| **JWT over server-sessions** | Horizontal scalability, microservice-friendly, mobile-ready |
| **HS256 over RS256** | Single-app symmetric secret is simpler; would switch to RS256 if multiple services need to verify the same JWT (issuer signs with private, others verify with public) |
| **15-min access token TTL** | Balances UX vs blast radius — long enough to avoid constant refreshes, short enough to limit damage from leaked token |
| **BCrypt over PBKDF2 / SHA-256** | BCrypt is purpose-built for passwords, GPU-resistant, strength-tunable. Never use plain SHA. |
| **DTO records over entities at API boundary** | Prevents over-posting / mass assignment attacks, decouples API from DB schema, immutable |
| **Method-level @PreAuthorize over only URL rules** | Defense in depth; method-level catches refactoring mistakes that URL rules might miss |
| **Stateful refresh tokens (when added)** | Allows revocation; access tokens stay stateless |
| **CORS explicit allow-list** | Prevents CSRF / unauthorized cross-origin reads; never use `*` in production |

---

## Part 10: Things You Learned (For "Lessons Learned" Questions)

> "Three things stand out:
>
> **1. Defense in depth matters.** Multiple layers — URL rules, method-level annotations, programmatic checks in services — caught a bug during a refactor that URL rules alone wouldn't have caught.
>
> **2. Bean Validation > manual validation.** Putting `@NotBlank @Email @Size` on the DTO record removes 30 lines of validation code per endpoint and gives consistent error responses via my GlobalExceptionHandler.
>
> **3. Records are perfect for DTOs.** Immutable, concise, and the equals/hashCode/toString come free. The only friction is making sure JSON deserialization works — which Jackson handles natively for records since Spring Boot 3."

---

## TL;DR — Memorize These Three Things

1. **The 90-second opening pitch** (Part 1)
2. **The 5 elements of `SecurityConfig` walkthrough** (Part 3)
3. **The 10 killer one-liners** (Part 6)

That's all you need to confidently demo this code in any senior backend interview.

Good luck. 🦞
