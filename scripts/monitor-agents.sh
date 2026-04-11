#!/bin/bash
# ============================================================================
# OpenClaw Agent Health Monitor — Mac Studio 36GB Optimized
# ============================================================================
# Real-time monitoring of agent memory, latency, and API usage
# Usage: ./scripts/monitor-agents.sh
# ============================================================================

set -e

GATEWAY_HOST="${GATEWAY_HOST:-127.0.0.1}"
GATEWAY_PORT="${GATEWAY_PORT:-18789}"
REFRESH_INTERVAL="${REFRESH_INTERVAL:-5}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

clear_screen() {
    clear
    echo -e "${BLUE}🦞 OpenClaw Agent Monitor${NC}"
    echo "======================================================================"
    echo -e "Gateway: ${GREEN}http://$GATEWAY_HOST:$GATEWAY_PORT${NC} | Refresh: ${YELLOW}${REFRESH_INTERVAL}s${NC}"
    echo "======================================================================"
}

check_gateway() {
    if ! curl -s "http://$GATEWAY_HOST:$GATEWAY_PORT/health" > /dev/null 2>&1; then
        echo -e "${RED}❌ Gateway unreachable at http://$GATEWAY_HOST:$GATEWAY_PORT${NC}"
        exit 1
    fi
}

monitor_agents() {
    echo ""
    echo -e "${BLUE}[AGENTS]${NC}"
    curl -s "http://$GATEWAY_HOST:$GATEWAY_PORT/agents" 2>/dev/null | jq -r '.[] |
        "\(.id | gsub("[☕⚛️🧪☁️🧠]"; "")) | \(.status) | \(.model) | Mem: \(.memory_mb)MB | Latency: \(.latency_ms)ms"' || echo "  (unable to fetch)"

    echo ""
    echo -e "${BLUE}[OLLAMA STATUS]${NC}"
    if command -v ollama &> /dev/null; then
        ollama list 2>/dev/null | tail -n +2 | awk '{printf "  %-15s %s\n", $1, $3}' || echo "  (Ollama not responding)"
    else
        echo "  ⚠️  Ollama not found in PATH"
    fi
}

monitor_docker() {
    echo ""
    echo -e "${BLUE}[DOCKER CONTAINERS]${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.CPUPerc}}\t{{.MemUsage}}" 2>/dev/null | grep -i openclaw || echo "  (no containers running)"
}

monitor_system() {
    echo ""
    echo -e "${BLUE}[SYSTEM RESOURCES]${NC}"

    # Memory usage
    if [ "$(uname)" = "Darwin" ]; then
        TOTAL_MEM=$(sysctl -n hw.memsize | awk '{print $1/1024/1024/1024}')
        USED_MEM=$(vm_stat | grep "Pages in use" | awk '{print $3}' | tr -d '.' | awk -v pagesize=4096 '{printf "%.1f", $1*pagesize/1024/1024/1024}')
        CPU_COUNT=$(sysctl -n hw.ncpu)
        echo -e "  Memory: ${USED_MEM}GB / ${TOTAL_MEM}GB | CPUs: $CPU_COUNT"
    fi
}

monitor_api_usage() {
    echo ""
    echo -e "${BLUE}[API TOKEN USAGE]${NC}"

    if [ -f ~/.openclaw/logs/gateway.log ]; then
        echo "  (Last 100 API calls)"
        tail -100 ~/.openclaw/logs/gateway.log 2>/dev/null | jq -s 'group_by(.provider) | map({provider: .[0].provider, calls: length, tokens: map(.tokens // 0) | add})' 2>/dev/null || echo "  (unable to parse logs)"
    else
        echo "  (No logs available)"
    fi
}

main() {
    check_gateway

    while true; do
        clear_screen
        monitor_agents
        monitor_docker
        monitor_system
        monitor_api_usage

        echo ""
        echo -e "${YELLOW}Press Ctrl+C to exit${NC}"
        sleep "$REFRESH_INTERVAL"
    done
}

main "$@"

