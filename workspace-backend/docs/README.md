# Backend (Java + Spring Boot) — Design Doc Library

Generated deep-dive design docs from `@backend` agent.

Covers: Java 21, Spring Boot 3.3, WebFlux, MongoDB, JUnit, Resilience4j, Apache Camel, integration patterns.

## File Naming Convention

```
{YYYY-MM-DD}-{topic-slug}.md
```

Examples:
- `2026-04-26-apache-camel-eip.md`
- `2026-04-27-circuit-breaker-resilience4j.md`
- `2026-04-28-virtual-threads-java21.md`

## How to Generate a New Doc

In Slack DM:
```
@backend deep dive on "{topic}" following all 12 sections of /DESIGN_DOC_TEMPLATE.md.
Workspace is read-only — post the COMPLETE content in this Slack thread as 5 messages
(Sections 1-3, 4-6, 7-9, 10-11, 12). Don't try to write files.
```

Save to:
```bash
nano ~/projects/openclaw-dev-team/workspace-backend/docs/{YYYY-MM-DD}-{topic-slug}.md
```

OR use the helper script if agent saved to /tmp/output/:
```bash
./scripts/pull-agent-output.sh
mv staging/output/*.md workspace-backend/docs/
```

## Current Topics

| Date | Topic | Status |
|---|---|---|
| _none yet_ | | |
