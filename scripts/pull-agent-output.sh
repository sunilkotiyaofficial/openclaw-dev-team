#!/bin/bash
# ═════════════════════════════════════════════════════════════
# pull-agent-output.sh — Extract agent-generated files from sandbox
# ═════════════════════════════════════════════════════════════
# Run this AFTER an agent has generated content in its sandbox
# at /tmp/output/. Extracts everything to your local workspace.
#
# Usage:
#   ./scripts/pull-agent-output.sh                    # uses default staging dir
#   ./scripts/pull-agent-output.sh /custom/path       # custom dest
# ═════════════════════════════════════════════════════════════

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_STAGING="$PROJECT_ROOT/staging"
DEST="${1:-$DEFAULT_STAGING}"

echo "═══════════════════════════════════════════"
echo "  OpenClaw Agent Output Extractor"
echo "═══════════════════════════════════════════"

# Find orchestrator sandbox container
CID=$(docker ps --filter "name=openclaw-sbx-agent-orchestrator" --format "{{.ID}}" 2>/dev/null | head -1)

if [ -z "$CID" ]; then
  echo "❌ ERROR: No running orchestrator container found."
  echo ""
  echo "Possible reasons:"
  echo "  1. Gateway not running   → openclaw gateway restart"
  echo "  2. No recent agent run   → trigger a deep-dive in Slack first"
  echo "  3. Container stopped     → check 'docker ps -a'"
  exit 1
fi

echo "✓ Container found: $CID"
echo ""

# Check what's in /tmp/output
echo "→ Inspecting /tmp/output in container..."
FILES=$(docker exec "$CID" sh -c 'cd /tmp && [ -d output ] && find output -type f 2>/dev/null || echo ""' 2>/dev/null)

if [ -z "$FILES" ]; then
  echo "❌ /tmp/output is empty or doesn't exist."
  echo ""
  echo "When you prompt agents, include this line:"
  echo "  'Save the full content to /tmp/output/{date}-{slug}.md'"
  exit 1
fi

echo "  Found files:"
echo "$FILES" | sed 's/^/    /'
echo ""

# Extract via tar streaming (bypasses docker cp permission glitches)
mkdir -p "$DEST"
echo "→ Extracting to: $DEST"
docker exec "$CID" sh -c 'cd /tmp && tar cf - output' 2>/dev/null | tar xf - -C "$DEST" 2>/dev/null

if [ ! -d "$DEST/output" ]; then
  echo "❌ Extraction failed. Check container access."
  exit 1
fi

echo "✓ Extracted successfully"
echo ""
echo "Files now at: $DEST/output/"
ls -la "$DEST/output/" | tail -n +2
echo ""

echo "═══════════════════════════════════════════"
echo "  Next steps:"
echo "═══════════════════════════════════════════"
echo "  1. Move files to the right workspace folder, e.g.:"
echo "     mv $DEST/output/*.md $PROJECT_ROOT/workspace-aiml/docs/"
echo ""
echo "  2. Commit:"
echo "     cd $PROJECT_ROOT"
echo "     git add workspace-*/docs/"
echo "     git commit -m 'docs: add deep dive on \$TOPIC'"
echo "     git push origin main"
echo "═══════════════════════════════════════════"
