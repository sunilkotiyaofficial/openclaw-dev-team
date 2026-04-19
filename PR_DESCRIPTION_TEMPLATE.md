# Pull Request Description Template

**Purpose:** Every PR created by agents uses this format. Consistent PR structure = faster self-review (your interview muscle for code review questions) and cleaner git history when squash-merging.

**When to use:** Agent creates a PR via `gh pr create --title ... --body ...` after pushing a feature branch.

---

## PR Title Format (Conventional Commits)

```
{type}({scope}): {short description in imperative mood}
```

**Types:**
- `feat` — new feature (most agent-generated work)
- `fix` — bug fix
- `docs` — documentation only
- `chore` — tooling, config, dependencies
- `refactor` — code restructure without behavior change
- `test` — adding / improving tests
- `perf` — performance improvement

**Scope examples:** `kafka`, `backend`, `devops`, `qa`, `frontend`, `orchestrator`

**Good examples:**
- `feat(kafka): add saga pattern deep dive — doc + code + tests`
- `feat(backend): implement circuit breaker with Resilience4j`
- `docs(devops): add GKE autoscaling design doc`

**Bad examples:**
- `Update stuff` ❌ (no type, no scope)
- `WIP` ❌ (not a description)
- `Fix bug in the payment service where the thing was broken` ❌ (no type, too long)

---

# TEMPLATE START — everything below goes into the PR body

---

## Summary

{2-3 sentences: what this PR delivers and why it matters. Match the design doc's TL;DR.}

## What's included

- 📄 **Design doc**: [`{YYYY-MM-DD}-{topic-slug}.md`](workspace-{agent}/docs/{YYYY-MM-DD}-{topic-slug}.md)
- 💻 **Working code**: [`workspace-{agent}/code/{YYYY-MM-DD}-{topic-slug}-demo/`](workspace-{agent}/code/{YYYY-MM-DD}-{topic-slug}-demo/)
- 🧪 **Tests**: Testcontainers integration + JUnit unit tests
- 📮 **Postman collection**: runnable via `./newman-run.sh`
- 🎯 **Interview questions**: 10 questions at end of design doc

## Agents that collaborated
- `@kafka` — design doc + architecture
- `@backend` — Spring Boot implementation
- `@qa` — test generation + code review
- `@devops` — Dockerfile + git/PR workflow

## How to test locally

```bash
git checkout {branch-name}
cd workspace-{agent}/code/{YYYY-MM-DD}-{topic-slug}-demo
docker-compose up -d
mvn test                              # unit tests
mvn verify -P integration-test        # Testcontainers tests
./postman/newman-run.sh               # HTTP contract tests
```

Then review the Slack thread link below for context.

## Review checklist (for self-review practice)

### Design
- [ ] Does the design doc explain **why** this approach, not just **what**?
- [ ] Are trade-offs honest (includes 3+ cons)?
- [ ] Are failure modes concrete and detectable?
- [ ] Is Phase 1 truly minimal (no gold-plating)?

### Code
- [ ] Does `docker-compose up` start the full stack cleanly?
- [ ] Are all secrets via env vars (no hardcoded tokens)?
- [ ] Is logging structured and free of PII?
- [ ] Are there at least one happy-path + one failure-path integration test?
- [ ] No `Thread.sleep()` — uses `Awaitility` for async?

### Tests
- [ ] Testcontainers for integration (no H2 / in-memory)?
- [ ] Each test has one clear assertion?
- [ ] AAA pattern (Arrange-Act-Assert)?
- [ ] Tests run in under 30s total for unit, 2min for integration?

### API Contract
- [ ] Postman collection imports cleanly?
- [ ] All endpoints have response-time SLA assertions?
- [ ] Environment file has no real secrets?

### Interview readiness
- [ ] Can you answer all 10 interview questions in the doc out loud?
- [ ] Could you draw the architecture diagram from memory?
- [ ] Do you know the trade-offs vs at least 2 alternatives?

## Links
- **Slack thread**: {link to the DM / thread where this was triggered}
- **Design doc**: {link to the markdown file}
- **Related docs in library**: {links to related topics already in the workspace}
- **External references**: {from section 12 of design doc}

## Agent notes / decisions

{Optional: any non-obvious decisions agents made that reviewer should know. E.g., "Chose choreography over orchestration here because there are only 3 services. Orchestration gets its own topic doc later."}

## Risk assessment

- **Breaking changes**: {none | list them}
- **Migration required**: {no | describe}
- **Production impact**: {none — this is a learning project}

---

# TEMPLATE END

---

## Notes for Agents (devops agent in particular)

1. **Branch naming convention**: `topic/{YYYY-MM-DD}-{topic-slug}` (e.g., `topic/2026-04-18-saga-pattern`)
2. **Never push to `main` directly** — always through a PR
3. **Squash-merge** PRs on `main` (one commit per topic, clean history)
4. **Delete feature branch after merge** — use `gh pr merge --squash --delete-branch`
5. **PR numbers in design doc** — after PR is created, update the design doc's "PR:" field with the URL
6. **Conventional Commits in title** — required, no exceptions (matters for automated changelog later)
