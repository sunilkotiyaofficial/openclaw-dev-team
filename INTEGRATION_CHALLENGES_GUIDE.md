# 🔗 INTEGRATION GUIDE & CHALLENGES

**Overview:** Complete guide to integrations, common challenges, and solutions  
**Audience:** First-time setup, troubleshooting  
**Difficulty Level:** Beginner-friendly explanations

---

## 📊 INTEGRATION ARCHITECTURE

```
┌─────────────────────────────────────────────────────────┐
│                     YOUR MAC STUDIO                      │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────────────────────────────────────────┐   │
│  │             OrbStack Container Runtime           │   │
│  │  ┌─────────────────────────────────────────────┐ │   │
│  │  │         OpenClaw Gateway (Port 18789)      │ │   │
│  │  │  ┌──────────────────────────────────────┐  │ │   │
│  │  │  │  5 Agent Containers                 │  │ │   │
│  │  │  │  • Orchestrator (Gemini Flash)      │  │ │   │
│  │  │  │  • Backend (Claude Sonnet)          │  │ │   │
│  │  │  │  • Frontend (Gemini Flash)          │  │ │   │
│  │  │  │  • QA (Ollama Gemma4)               │  │ │   │
│  │  │  │  • DevOps (Gemini Flash)            │  │ │   │
│  │  │  └──────────────────────────────────────┘  │ │   │
│  │  └─────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────┘   │
│                                                           │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Ollama (Gemma 4 26B + Llama 2 7B)               │   │
│  │  Running as separate host process                │   │
│  └──────────────────────────────────────────────────┘   │
│                                                           │
│  ┌──────────────────────────────────────────────────┐   │
│  │  APIs Accessed:                                  │   │
│  │  • Anthropic API (Claude Sonnet for backend)    │   │
│  │  • Google Gemini API (Gemini Flash for others)  │   │
│  │  • Telegram API (for messages/tasking)          │   │
│  └──────────────────────────────────────────────────┘   │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

---

## 🔌 INTEGRATION POINTS (7 Critical)

### 1. OrbStack ↔ Docker

**What it is:** OrbStack runs Docker containers  
**How it works:** OrbStack manages the container runtime, replacing Docker Desktop  
**Potential issues:**
- OrbStack not started
- Docker CLI not in PATH
- Port conflicts

**Solution:**
```bash
# Verify integration
orbctl status                    # Should show "running"
docker ps                        # Should work
docker info | grep "Operating System"  # Should show "orbstack"
```

---

### 2. Docker ↔ OpenClaw Gateway

**What it is:** OpenClaw runs as a container orchestrator  
**How it works:** OpenClaw daemon talks to Docker to manage agent containers  
**Potential issues:**
- Gateway won't start
- Containers won't launch
- Port 18789 in use

**Solution:**
```bash
# Check if port is free
lsof -i :18789

# Restart gateway
openclaw gateway restart
sleep 5
curl http://127.0.0.1:18789/health
```

---

### 3. OpenClaw ↔ Ollama

**What it is:** QA agent uses Ollama for local inference  
**How it works:** OpenClaw connects to Ollama via HTTP (localhost:11434)  
**Potential issues:**
- Ollama not running
- Model not found
- Timeout connecting

**Solution:**
```bash
# Check Ollama
curl http://localhost:11434/api/tags  # Should return JSON
ollama list                            # Should show gemma4:26b

# Restart if needed
brew services restart ollama
sleep 30
```

---

### 4. Agents ↔ Anthropic API

**What it is:** Backend agent uses Claude Sonnet  
**How it works:** Agent sends requests to Anthropic API via HTTPS  
**Potential issues:**
- API key invalid
- Rate limit hit
- Network timeout
- No internet connection

**Solution:**
```bash
# Verify key
echo $ANTHROPIC_API_KEY  # Should show key (not empty)

# Test connectivity
curl https://api.anthropic.com/v1/messages \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01"

# Check quota
# Go to: https://console.anthropic.com/account/limits
```

---

### 5. Agents ↔ Google Gemini API

**What it is:** Orchestrator, Frontend, DevOps use Gemini Flash  
**How it works:** Agents send requests to Google API  
**Potential issues:**
- API key invalid
- Free tier rate limit
- No quota left

**Solution:**
```bash
# Verify key
echo $GEMINI_API_KEY  # Should show key

# Test connectivity
curl https://generativelanguage.googleapis.com/v1beta/models/models \
  -H "x-goog-api-key: $GEMINI_API_KEY"

# Check quota
# Go to: https://console.cloud.google.com/apis/quotas
```

---

### 6. OpenClaw ↔ Telegram API

**What it is:** Agents communicate via Telegram  
**How it works:** Gateway sends/receives messages from Telegram bot  
**Potential issues:**
- Bot token invalid
- Not connected to @BotFather
- Messages not routing to correct chat
- Bot not responding

**Solution:**
```bash
# Verify token
echo $TELEGRAM_BOT_TOKEN  # Should show token

# Test bot
# Send message to your Telegram bot
# Should receive acknowledgment

# If not working:
# 1. Go to @BotFather
# 2. /mybots → select your bot
# 3. /newtoken
# 4. Update .zshrc with new token
# 5. source ~/.zshrc
# 6. openclaw gateway restart
```

---

### 7. Git Repository ↔ Agent Workspaces

**What it is:** Your GitHub repo connected to agent workspaces  
**How it works:** Agents read/write from ~/.openclaw/workspace-*  
**Potential issues:**
- Workspaces not created
- Permissions denied
- Disk full

**Solution:**
```bash
# Verify workspaces exist
ls -la ~/.openclaw/workspace-*/

# Check permissions
chmod 755 ~/.openclaw/workspace-*

# Check disk space
df -h ~/.openclaw
```

---

## ⚠️ COMMON CHALLENGES & SOLUTIONS

### Challenge 1: "OrbStack not responding" or "Docker won't start"

**Symptoms:**
- `docker ps` returns error
- `orbctl status` shows "not running"
- Terminal says "Docker daemon not responding"

**Root causes:**
1. OrbStack app not started
2. OrbStack crashed
3. Port conflicts

**Solutions:**
```bash
# Option 1: Restart OrbStack
brew services stop orbstack
sleep 5
brew services start orbstack
sleep 30
docker ps

# Option 2: Hard restart
killall -9 OrbStack
sleep 5
open /Applications/OrbStack.app
sleep 40
docker ps

# Option 3: Check if something using docker port
lsof -i :2375  # OrbStack's docker port
lsof -i :2376
```

**Prevention:**
- Don't close OrbStack while working
- Keep it running in background
- Set to start on boot: `brew services start orbstack`

---

### Challenge 2: "Ollama model not found" or "Model keeps unloading"

**Symptoms:**
- `ollama run gemma4:26b` says model not found
- QA agent responds with "model not found"
- Model works once then disappears

**Root causes:**
1. Model not fully downloaded
2. Ollama unloading after inactivity (5 min default)
3. Model corrupted during download
4. Disk space issues

**Solutions:**
```bash
# Option 1: Re-download
ollama rm gemma4:26b
ollama pull gemma4:26b

# Option 2: Keep models loaded longer
export OLLAMA_KEEP_ALIVE=1h  # Keep for 1 hour
# Add to ~/.zshrc permanently

# Option 3: Pre-load before work
./scripts/warm-cache.sh

# Option 4: Check disk space
df -h
# Should have > 50GB free
```

**Prevention:**
- Run `./scripts/warm-cache.sh` before starting work
- Don't interrupt model downloads
- Keep `OLLAMA_KEEP_ALIVE` set to 1h

---

### Challenge 3: "API Key not recognized" or "Invalid API key"

**Symptoms:**
- Agents respond with "API key invalid"
- `echo $ANTHROPIC_API_KEY` shows empty
- Different terminal windows see different values

**Root causes:**
1. Environment variables not exported
2. .zshrc not sourced in current terminal
3. Typo in key value
4. Key from wrong service (e.g., Google key in Anthropic)

**Solutions:**
```bash
# Option 1: Set in current terminal only
export ANTHROPIC_API_KEY="sk-..."
export GEMINI_API_KEY="AIza..."
export TELEGRAM_BOT_TOKEN="123..."

# Option 2: Make permanent (.zshrc method)
# Edit ~/.zshrc:
nano ~/.zshrc

# Add these lines at the end:
export ANTHROPIC_API_KEY="sk-..."
export GEMINI_API_KEY="AIza..."
export TELEGRAM_BOT_TOKEN="123..."

# Save: Ctrl+X, Y, Enter

# Option 3: Reload current terminal
source ~/.zshrc

# Option 4: Verify
echo "Anthropic: $ANTHROPIC_API_KEY" | head -c 20
echo "Gemini: $GEMINI_API_KEY" | head -c 20
```

**Prevention:**
- Add to ~/.zshrc, not just terminal
- Always `source ~/.zshrc` after editing
- Check keys don't have spaces at end
- Verify key is from correct service

---

### Challenge 4: "System running slow" or "Memory pressure: Yellow"

**Symptoms:**
- Activity Monitor shows "Memory pressure: Yellow" or "Red"
- System very slow/laggy
- Beach ball spinning
- Agents responding slowly

**Root causes:**
1. All 5 agents + Ollama + browser = too much RAM
2. Swap being used (disk swapping)
3. Docker using too much memory
4. Other apps consuming memory

**Solutions:**
```bash
# Option 1: See what's using memory
top -o %MEM   # Sort by memory usage
ps aux | sort -rn -k 4 | head -10

# Option 2: Free up RAM immediately
# Close Chrome/Safari/other apps
# Restart Docker: docker system restart
# Restart Ollama: ollama restart

# Option 3: Reduce agent load
# Don't run all 5 agents simultaneously
# Send simpler tasks that use fewer agents

# Option 4: Increase OrbStack memory
# Edit ~/.openclaw/openclaw.json
# Change "vmMemory": "14GB" to "16GB"
# Restart: openclaw gateway restart

# Option 5: Monitor memory usage
./scripts/monitor-agents.sh
# Watch memory in real-time
```

**Prevention:**
- Close browser before running agents
- Don't run IDE while agents working
- Monitor memory proactively
- Don't exceed agent caps

---

### Challenge 5: "Telegram messages not being received" or "Bot not responding"

**Symptoms:**
- Send message to bot, get no response
- OpenClaw says "No messages"
- Other Telegram bots work fine

**Root causes:**
1. Bot token wrong or expired
2. Wrong chat ID
3. Telegram API down
4. Bot not actually created
5. Gateway not connected to Telegram

**Solutions:**
```bash
# Option 1: Verify token
echo $TELEGRAM_BOT_TOKEN

# Option 2: Create new token
# Go to @BotFather on Telegram
# /mybots
# Select your bot
# /newtoken
# Use new token

# Option 3: Update token
nano ~/.zshrc
# Edit TELEGRAM_BOT_TOKEN line
source ~/.zshrc
openclaw gateway restart

# Option 4: Verify message format
# Send: "@orchestrator: Hello"
# Must mention @orchestrator for delegation

# Option 5: Check gateway
curl http://127.0.0.1:18789/channels/telegram/status
```

**Prevention:**
- Store token safely in .zshrc
- Test bot sends message
- Check connection before starting work

---

### Challenge 6: "Agents won't start" or "All agents showing offline"

**Symptoms:**
- `openclaw agents list` shows all "offline"
- Gateway not responding
- Can't send tasks
- Port 18789 not responding

**Root causes:**
1. Gateway crashed
2. Configuration invalid JSON
3. Docker issue
4. Port in use

**Solutions:**
```bash
# Option 1: Restart gateway
openclaw gateway restart
sleep 5
curl http://127.0.0.1:18789/health

# Option 2: Check logs
tail -f ~/.openclaw/logs/gateway.log

# Option 3: Validate config
cat ~/.openclaw/openclaw.json | jq .
# Should show valid JSON, no errors

# Option 4: Check port
lsof -i :18789
# If something else using port, kill it

# Option 5: Nuclear option (if all else fails)
killall -9 openclaw
sleep 5
rm ~/.openclaw/logs/*.log
openclaw gateway start
sleep 10
```

**Prevention:**
- Don't edit openclaw.json manually
- Always validate JSON after edits
- Don't close gateway abruptly
- Monitor agent status regularly

---

### Challenge 7: "Build failing" or "Agent code won't run"

**Symptoms:**
- Backend generates code but build fails
- Maven/npm errors
- Docker build fails
- Tests fail

**Root causes:**
1. Missing dependencies
2. Java/Node version mismatch
3. Build tool not installed
4. Invalid code generation

**Solutions:**
```bash
# Option 1: Check what's available
java -version
node -v
mvn -v
docker --version

# Option 2: Install missing tools
brew install maven
brew install openjdk

# Option 3: Tell agents about constraints
# In your Telegram message, specify:
# "Use Java 21, Spring Boot 3.3, Maven"

# Option 4: Check agent output
ls -la ~/.openclaw/workspace-backend/
# Review generated code for errors

# Option 5: Run command locally
cd ~/.openclaw/workspace-backend/
mvn clean install  # Try to build locally
```

**Prevention:**
- Ensure all build tools installed
- Specify versions in task description
- Let agents use defaults (they match your system)

---

## 🔍 DIAGNOSTICS SCRIPT

Create `scripts/diagnose.sh`:

```bash
#!/bin/bash
echo "=== OpenClaw Diagnostics ==="
echo ""
echo "1. OrbStack"
orbctl status
echo ""
echo "2. Docker"
docker ps
echo ""
echo "3. Ollama"
ollama list
echo ""
echo "4. OpenClaw Gateway"
curl http://127.0.0.1:18789/health
echo ""
echo "5. API Keys"
echo "Anthropic: $(echo $ANTHROPIC_API_KEY | head -c 10)..."
echo "Gemini: $(echo $GEMINI_API_KEY | head -c 10)..."
echo ""
echo "6. System Resources"
echo "Memory:"
top -l 1 | head -3
echo "Disk:"
df -h ~/.openclaw
echo ""
echo "=== Diagnostics Complete ==="
```

Run anytime something doesn't work:
```bash
chmod +x scripts/diagnose.sh
./scripts/diagnose.sh
```

---

## ✅ INTEGRATION CHECKLIST

Before declaring setup complete, verify:

- [ ] OrbStack running and responsive
- [ ] Docker working (`docker ps`)
- [ ] Ollama responsive (`ollama list`)
- [ ] Models downloaded (Gemma4 + Llama2)
- [ ] API keys set and accessible
- [ ] OpenClaw gateway running
- [ ] All 5 agents showing active
- [ ] Telegram bot connected and sending messages
- [ ] Memory usage reasonable (~28-30GB)
- [ ] First task completed successfully

---

## 📞 SUPPORT MATRIX

| Component | Issue | Check File |
|-----------|-------|-----------|
| OrbStack | Won't start | COMPLETE_SETUP_GUIDE.md#Phase2 |
| Ollama | Models slow | COMPLETE_SETUP_GUIDE.md#Challenge1 |
| API Keys | Not found | COMPLETE_SETUP_GUIDE.md#Challenge4 |
| Memory | High usage | MEMORY_ANALYSIS_FINAL.md |
| Telegram | Bot not responding | COMPLETE_SETUP_GUIDE.md#Challenge6 |
| Performance | Agents slow | ORBSTACK_VS_DOCKER.md |
| Configuration | JSON errors | FINAL_CONFIGURATION_REVIEW.md |

---

**Status:** ✅ All integrations documented  
**Challenges documented:** 7 major challenges + solutions  
**Support matrix:** Complete  

🚀 **You have everything needed to troubleshoot any issue!**


