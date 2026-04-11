#!/bin/bash
# ============================================================================
# OpenClaw System Health Check — Pre-flight Verification
# ============================================================================
# Validates all prerequisites before running the dev team
# Usage: ./scripts/health-check.sh
# ============================================================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

CHECKS_PASSED=0
CHECKS_FAILED=0

check() {
    local name="$1"
    local command="$2"

    echo -n "  $name... "

    if eval "$command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC}"
        ((CHECKS_PASSED++))
    else
        echo -e "${RED}✗${NC}"
        ((CHECKS_FAILED++))
    fi
}

echo -e "${BLUE}🦞 OpenClaw Health Check${NC}"
echo "=========================================="

echo ""
echo -e "${BLUE}[CLI TOOLS]${NC}"
check "Node.js 22+" "node -v | grep -E 'v(2[2-9]|[3-9][0-9])'"
check "npm" "npm --version"
check "OpenClaw CLI" "openclaw --version"
check "Ollama" "ollama --version"
check "Docker" "docker --version"
check "Git" "git --version"
check "curl" "curl --version"
check "jq" "jq --version"

echo ""
echo -e "${BLUE}[ENVIRONMENT VARIABLES]${NC}"
check "ANTHROPIC_API_KEY set" "[ -n \"\$ANTHROPIC_API_KEY\" ]"
check "GEMINI_API_KEY set" "[ -n \"\$GEMINI_API_KEY\" ]"
check "TELEGRAM_BOT_TOKEN set" "[ -n \"\$TELEGRAM_BOT_TOKEN\" ]"

echo ""
echo -e "${BLUE}[NETWORK & CONNECTIVITY]${NC}"
check "Ollama listening" "curl -s http://localhost:11434/api/tags > /dev/null"
check "OpenClaw gateway" "curl -s http://127.0.0.1:18789/health > /dev/null"
check "Internet access" "curl -s https://api.github.com > /dev/null"

echo ""
echo -e "${BLUE}[DIRECTORIES]${NC}"
check "~/.openclaw exists" "[ -d ~/.openclaw ]"
check "Ollama models" "ollama list | grep -q gemma4"

echo ""
echo -e "${BLUE}[RESOURCE LIMITS]${NC}"
TOTAL_MEM=$(sysctl -n hw.memsize | awk '{print $1/1024/1024/1024 " GB"}')
CPU_COUNT=$(sysctl -n hw.ncpu)
echo "  Total Memory: $TOTAL_MEM"
echo "  CPU Cores: $CPU_COUNT"

if [ "$CPU_COUNT" -lt 6 ]; then
    echo -e "  ${YELLOW}⚠️  Less than 6 CPU cores. Performance may be limited.${NC}"
fi

echo ""
echo "=========================================="
echo -e "Results: ${GREEN}$CHECKS_PASSED passed${NC}, ${RED}$CHECKS_FAILED failed${NC}"

if [ $CHECKS_FAILED -gt 0 ]; then
    echo -e "${RED}❌ Health check failed. Fix issues above and retry.${NC}"
    exit 1
else
    echo -e "${GREEN}✅ All systems go! Ready to launch OpenClaw.${NC}"
fi

