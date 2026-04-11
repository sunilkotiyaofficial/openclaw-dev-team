#!/bin/bash
# ============================================================================
# Sunil's OpenClaw Multi-Agent Dev Team — Setup Script
# ============================================================================
# Run this on your Mac Studio after installing OpenClaw
# Usage: ./setup-dev-team.sh
# ============================================================================

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🦞 OpenClaw Multi-Agent Dev Team Setup${NC}"
echo "=========================================="

# ----------------------------------------------------------------------------
# 1. Check prerequisites
# ----------------------------------------------------------------------------
echo -e "\n${BLUE}[1/7]${NC} Checking prerequisites..."

if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js not found. Install Node 24+ first.${NC}"
    exit 1
fi

NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 22 ]; then
    echo -e "${RED}❌ Node.js 22+ required. You have v$NODE_VERSION${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Node.js $(node -v)${NC}"

if ! command -v openclaw &> /dev/null; then
    echo -e "${YELLOW}⚠ OpenClaw not installed. Installing now...${NC}"
    npm install -g openclaw@latest
fi
echo -e "${GREEN}✓ OpenClaw $(openclaw --version)${NC}"

if ! command -v ollama &> /dev/null; then
    echo -e "${YELLOW}⚠ Ollama not found. The QA agent won't work without it.${NC}"
    echo "   Install from: https://ollama.com/download"
else
    echo -e "${GREEN}✓ Ollama installed${NC}"
fi

if ! command -v claude &> /dev/null; then
    echo -e "${YELLOW}⚠ Claude Code CLI not found. Backend delegation won't work.${NC}"
    echo "   Install: npm install -g @anthropic-ai/claude-code"
else
    echo -e "${GREEN}✓ Claude Code CLI installed${NC}"
fi

# ----------------------------------------------------------------------------
# 2. Check API keys
# ----------------------------------------------------------------------------
echo -e "\n${BLUE}[2/7]${NC} Checking API keys..."

if [ -z "$GEMINI_API_KEY" ]; then
    echo -e "${RED}❌ GEMINI_API_KEY not set${NC}"
    echo "   Get one at: https://aistudio.google.com/apikey"
    echo "   Then: export GEMINI_API_KEY='your-key'"
    exit 1
fi
echo -e "${GREEN}✓ GEMINI_API_KEY set${NC}"

if [ -z "$ANTHROPIC_API_KEY" ]; then
    echo -e "${YELLOW}⚠ ANTHROPIC_API_KEY not set${NC}"
    echo "   Backend and thinking tasks will fail without it."
    echo "   Get one at: https://console.anthropic.com/"
    read -p "   Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then exit 1; fi
else
    echo -e "${GREEN}✓ ANTHROPIC_API_KEY set${NC}"
fi

if [ -z "$TELEGRAM_BOT_TOKEN" ]; then
    echo -e "${YELLOW}⚠ TELEGRAM_BOT_TOKEN not set${NC}"
    echo "   Create a bot via @BotFather on Telegram first."
    read -p "   Continue without Telegram? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then exit 1; fi
else
    echo -e "${GREEN}✓ TELEGRAM_BOT_TOKEN set${NC}"
fi

# ----------------------------------------------------------------------------
# 3. Pull Ollama models for QA agent
# ----------------------------------------------------------------------------
echo -e "\n${BLUE}[3/7]${NC} Setting up local Ollama models..."

if command -v ollama &> /dev/null; then
    echo "Pulling gemma4:26b (this may take a few minutes on first run)..."
    ollama pull gemma4:26b || echo -e "${YELLOW}⚠ Failed to pull model${NC}"
    echo "Pulling llama2:7b as fallback..."
    ollama pull llama2:7b || echo -e "${YELLOW}⚠ Failed to pull fallback model${NC}"
    echo -e "${GREEN}✓ Ollama models ready${NC}"
fi

# ----------------------------------------------------------------------------
# 4. Create workspace directories
# ----------------------------------------------------------------------------
echo -e "\n${BLUE}[4/7]${NC} Creating agent workspaces..."

OPENCLAW_HOME="${HOME}/.openclaw"
mkdir -p "${OPENCLAW_HOME}/workspace-orchestrator"
mkdir -p "${OPENCLAW_HOME}/workspace-backend"
mkdir -p "${OPENCLAW_HOME}/workspace-frontend"
mkdir -p "${OPENCLAW_HOME}/workspace-qa"
mkdir -p "${OPENCLAW_HOME}/workspace-devops"
mkdir -p "${OPENCLAW_HOME}/agents/orchestrator/agent"
mkdir -p "${OPENCLAW_HOME}/agents/backend/agent"
mkdir -p "${OPENCLAW_HOME}/agents/frontend/agent"
mkdir -p "${OPENCLAW_HOME}/agents/qa/agent"
mkdir -p "${OPENCLAW_HOME}/agents/devops/agent"

echo -e "${GREEN}✓ Workspace directories created${NC}"

# ----------------------------------------------------------------------------
# 5. Copy SOUL.md files to each workspace
# ----------------------------------------------------------------------------
echo -e "\n${BLUE}[5/7]${NC} Installing SOUL.md files..."

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PACKAGE_ROOT="$(dirname "$SCRIPT_DIR")"

cp "${PACKAGE_ROOT}/workspace-orchestrator/SOUL.md" "${OPENCLAW_HOME}/workspace-orchestrator/"
cp "${PACKAGE_ROOT}/workspace-backend/SOUL.md" "${OPENCLAW_HOME}/workspace-backend/"
cp "${PACKAGE_ROOT}/workspace-frontend/SOUL.md" "${OPENCLAW_HOME}/workspace-frontend/"
cp "${PACKAGE_ROOT}/workspace-qa/SOUL.md" "${OPENCLAW_HOME}/workspace-qa/"
cp "${PACKAGE_ROOT}/workspace-devops/SOUL.md" "${OPENCLAW_HOME}/workspace-devops/"

echo -e "${GREEN}✓ SOUL.md files installed${NC}"

# ----------------------------------------------------------------------------
# 6. Backup existing config and install new one
# ----------------------------------------------------------------------------
echo -e "\n${BLUE}[6/7]${NC} Installing openclaw.json config..."

CONFIG_FILE="${OPENCLAW_HOME}/openclaw.json"
if [ -f "$CONFIG_FILE" ]; then
    BACKUP_FILE="${CONFIG_FILE}.backup.$(date +%Y%m%d-%H%M%S)"
    cp "$CONFIG_FILE" "$BACKUP_FILE"
    echo -e "${YELLOW}⚠ Existing config backed up to: $BACKUP_FILE${NC}"
fi

cp "${PACKAGE_ROOT}/openclaw.json" "$CONFIG_FILE"
echo -e "${GREEN}✓ Config installed at $CONFIG_FILE${NC}"

# ----------------------------------------------------------------------------
# 7. Restart gateway and verify
# ----------------------------------------------------------------------------
echo -e "\n${BLUE}[7/7]${NC} Restarting OpenClaw gateway..."

openclaw gateway restart || openclaw gateway start

echo -e "\nVerifying agents..."
sleep 2
openclaw agents list --bindings || true

echo ""
echo "=========================================="
echo -e "${GREEN}✅ Setup complete!${NC}"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Send a message to your Telegram bot"
echo "  2. Try: 'Build a Spring Boot service that listens to Kafka events and stores to MongoDB'"
echo "  3. Watch the orchestrator delegate to @backend, @qa, @devops"
echo ""
echo "Useful commands:"
echo "  openclaw agents list --bindings    # List all agents"
echo "  openclaw channels status --probe   # Check Telegram connection"
echo "  openclaw logs                      # Tail gateway logs"
echo "  tail -f ~/.openclaw/logs/gateway.log  # Security monitoring"
echo ""
echo -e "${YELLOW}⚠ Security reminder: Monitor logs for the first week!${NC}"
