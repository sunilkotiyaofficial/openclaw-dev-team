# AI/ML Architecture — Design Doc Library

Generated deep-dive design docs from `@aiml` agent.

## File Naming Convention

```
{YYYY-MM-DD}-{topic-slug}.md
```

Examples:
- `2026-04-26-production-rag-architecture.md`
- `2026-04-27-vector-db-selection.md`
- `2026-04-28-agent-frameworks-comparison.md`

## How to Add a New Doc

### Method 1 — Manual (when sandbox workspace is read-only)

In Slack DM:
```
@aiml deep dive on "{topic}" following all 12 sections of /DESIGN_DOC_TEMPLATE.md.
Workspace is read-only — post the COMPLETE content in this Slack thread as 5 messages
(Sections 1-3, 4-6, 7-9, 10-11, 12). Don't try to write files.
```

Then save to file:
```bash
nano ~/projects/openclaw-dev-team/workspace-aiml/docs/{YYYY-MM-DD}-{topic-slug}.md
# Paste each Slack chunk in order
# Save: Ctrl+O, Enter, Ctrl+X
```

### Method 2 — Semi-automatic (sandbox /tmp + extraction script)

In Slack DM:
```
@aiml deep dive on "{topic}". SAVE the full content to /tmp/output/{YYYY-MM-DD}-{topic-slug}.md
inside your sandbox. ALSO post a 200-word summary in Slack.
```

Then extract:
```bash
cd ~/projects/openclaw-dev-team
./scripts/pull-agent-output.sh
mv staging/output/*.md workspace-aiml/docs/
```

### After Either Method — Commit

```bash
cd ~/projects/openclaw-dev-team
git add workspace-aiml/docs/
git commit -m "docs(aiml): {topic} deep dive"
git push origin main
```

## Current Topics

(Will be populated as you generate docs)

| Date | Topic | Status |
|---|---|---|
| _none yet_ | | |
