# ⚛️ Frontend — Topics Backlog

Interview study roadmap for React, TypeScript, and modern frontend architecture.

**How to use:**
- Topics ordered by priority (P0 = must-know for full-stack interviews)
- Trigger a deep dive in Slack: `@frontend deep dive on {topic}`
- For your architect track, frontend depth = **"how would you explain this to your backend team?"**

---

## P0 — Must Know (Full-Stack / Architect Interviews)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 1 | **Server State Management** (TanStack Query) | 🔸 Intermediate | ⬜ Not started | Queries, mutations, cache invalidation, optimistic updates. |
| 2 | **Form Patterns** (React Hook Form + Zod) | 🔹 Beginner | ⬜ Not started | Schema-first, type inference, error handling. |
| 3 | **Performance Optimization** | 🔥 Advanced | ⬜ Not started | Code splitting, lazy loading, memoization strategy. |
| 4 | **React Server Components (RSC)** | 🔥 Advanced | ⬜ Not started | Server vs Client boundary, when to use each. |
| 5 | **Real-time UI** (WebSocket, SSE) | 🔸 Intermediate | ⬜ Not started | Reconnection logic, backpressure, memory leaks. |
| 6 | **Accessibility (WCAG 2.2)** | 🔸 Intermediate | ⬜ Not started | Semantic HTML, ARIA, keyboard nav, focus management. |

## P1 — Should Know (Common Questions)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 7 | **Testing Strategy** (Vitest + RTL + Playwright) | 🔸 Intermediate | ⬜ Not started | Unit/component/E2E proportions. |
| 8 | **Event Stream Visualization** (for Kafka dashboards) | 🔥 Advanced | ⬜ Not started | Handling 10K events/sec without lag. D3/Recharts. |
| 9 | **State Machines (XState)** | 🔸 Intermediate | ⬜ Not started | When to pick over useState/useReducer. |
| 10 | **Error Boundaries & Suspense** | 🔸 Intermediate | ⬜ Not started | Loading states, error states, retry. |
| 11 | **Authentication Patterns** (JWT, refresh tokens, CSRF) | 🔸 Intermediate | ⬜ Not started | Storage (httpOnly cookies vs localStorage), refresh flow. |
| 12 | **TypeScript Advanced** (generics, conditional types, type guards) | 🔥 Advanced | ⬜ Not started | Senior interviewers will test `unknown`, discriminated unions. |

## P2 — Nice to Have (Shows Depth)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 13 | **Micro-frontends** | 🔥 Advanced | ⬜ Not started | Module Federation, integration patterns. |
| 14 | **Design Systems** | 🔸 Intermediate | ⬜ Not started | Tokens, composition, theming, documentation. |
| 15 | **Internationalization (i18n)** | 🔹 Beginner | ⬜ Not started | ICU MessageFormat, locale handling, RTL. |
| 16 | **Progressive Web Apps** | 🔸 Intermediate | ⬜ Not started | Service workers, offline-first, push notifications. |

---

## Suggested Learning Path

**Week 1 — Foundations:** #1, #2, #6 (server state, forms, accessibility)
**Week 2 — Performance:** #3, #10 (optimization, error boundaries)
**Week 3 — Full-Stack Context:** #5, #8, #11 (real-time, event viz, auth)
**Week 4 — Advanced:** #4, #9, #12 (RSC, state machines, TS advanced)
**Ongoing:** P2 as needed

---

## Cross-References (Your Backend/Architect Angle)

- **#5 Real-time UI** pairs with `kafka` → event stream consumption via WebSocket bridge
- **#8 Event Stream Visualization** pairs with `kafka` → building a Kafka dashboard
- **#11 Authentication** pairs with `backend` → JWT/OAuth2 flow end-to-end
- **#1 Server State** pairs with `backend` → cache invalidation strategy, stale-while-revalidate

---

## Status Legend

- ⬜ Not started
- 🔨 In progress
- 📝 Studied
- 🎯 Quiz-ready
- ✅ Mastered

---

**Last updated:** 2026-04-18
**Total topics:** 16 (6 P0 + 6 P1 + 4 P2)
**Mastered:** 0 / 16
