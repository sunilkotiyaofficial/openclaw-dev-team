# 🚀 COMPLETE SETUP GUIDE — From Zero to Running Agents

**Current Status:** Code committed to GitHub, OrbStack + Ollama downloaded, nothing running yet  
**Your Hardware:** Mac Studio 36GB  
**Goal:** Get all 5 agents running and writing code

---

## 📊 TIME ESTIMATE

| Phase | Task | Time | Total |
|-------|------|------|-------|
| **Phase 1** | Prerequisites Check | 5 min | 5 min |
| **Phase 2** | OrbStack Start | 2 min | 7 min |
| **Phase 3** | Ollama Setup | 20 min* | 27 min |
| **Phase 4** | OpenClaw Setup | 10 min | 37 min |
| **Phase 5** | Configuration Deploy | 5 min | 42 min |
| **Phase 6** | First Test Run | 5 min | 47 min |
| | **TOTAL** | | **~1 hour** |

*Ollama model download time depends on internet speed. 20 min is estimate for Gemma4 26B (26GB model).

---

## 🔧 PHASE 1: Prerequisites Verification (5 min)

### Step 1.1: Check What You Have

```bash
# Check Node.js
node -v          # Should be 22+
npm -v            # Should be 10+

# Check OpenClaw CLI
openclaw --version    # Should show a version

# Check if Ollama app is downloaded
ls -la /Applications/Ollama.app    # Should exist

# Check if OrbStack is downloaded
ls -la /Applications/OrbStack.app  # Should exist
```

### Step 1.2: Verify API Keys

```bash
# Check environment variables are set
echo $ANTHROPIC_API_KEY          # Should show your key
echo $GEMINI_API_KEY             # Should show your key
echo $TELEGRAM_BOT_TOKEN         # Should show your token

# If empty, set them in ~/.zshrc:
# export ANTHROPIC_API_KEY="your-key"
# export GEMINI_API_KEY="your-key"
# export TELEGRAM_BOT_TOKEN="your-token"

# Then reload:
source ~/.zshrc
```

### Step 1.3: Clone & Navigate to Project

```bash
# Assuming you have the repo cloned
cd /path/to/your/github/repo

# Verify files
ls -la openclaw-mac-studio-36gb-orbstack.json
ls -la scripts/setup-dev-team.sh
```

**Status after Phase 1:** ✅ Ready for next steps

---

## 🌐 PHASE 2: Start OrbStack (2 min)

### Step 2.1: Launch OrbStack

```bash
# Option A: Open via Finder
open /Applications/OrbStack.app

# Option B: Start via brew services
brew services start orbstack

# Wait 30-40 seconds for it to start
```

### Step 2.2: Verify OrbStack is Running

```bash
# Check status
orbctl status

# Expected output: "OrbStack is running"

# Verify Docker works through OrbStack
docker ps

# Should show empty list (or existing containers)
```

**Troubleshooting:**
```bash
# If not working, restart
orbctl stop
sleep 5
orbctl start
sleep 10
docker ps
```

**Status after Phase 2:** ✅ OrbStack running, Docker ready

---

## 🦙 PHASE 3: Setup Ollama (20 min)

### Step 3.1: Start Ollama

```bash
# Option A: Open via Finder
open /Applications/Ollama.app

# Option B: Start via brew services
brew services start ollama

# Wait 30 seconds for startup
```

### Step 3.2: Verify Ollama is Responding

```bash
# Check if Ollama server is running
curl http://localhost:11434/api/tags

# Expected output: JSON with list of models (initially empty)
```

### Step 3.3: Pull Gemma4 Model (THIS TAKES TIME!)

```bash
# Pull Gemma 4 26B model
# ⚠️ This is ~26GB, will take 15-30 min depending on internet speed
ollama pull gemma4:26b

# Watch progress:
# Downloading: ████████░░ 45%
# Downloading: ██████████ 100%
# Done!

# Also pull Llama 2 fallback (faster, ~4GB, takes 5-10 min)
ollama pull llama2:7b
```

### Step 3.4: Verify Models Loaded

```bash
# List available models
ollama list

# Should show:
# NAME           ID          SIZE    MODIFIED
# gemma4:26b     a80...      26GB    5 minutes ago
# llama2:7b      78...       4GB     2 minutes ago
```

### Step 3.5: Test Model is Working

```bash
# Quick test to ensure Gemma4 loads into memory
ollama run gemma4:26b "What is 2+2?"

# Expected: Model loads, generates "2+2 = 4"
# Time: ~5 seconds after first load
```

**Time Breakdown for Phase 3:**
- Ollama startup: 30s
- Model verification: 30s
- Gemma4 pull: **15-30 min** (internet dependent)
- Llama2 pull: **5-10 min**
- Testing: 1 min
- **Total: 20-40 min** (usually ~20-25 min on fast internet)

**Status after Phase 3:** ✅ Ollama models ready

---

## 🔐 PHASE 4: OpenClaw Installation (10 min)

### Step 4.1: Verify OpenClaw CLI

```bash
# Check if installed
openclaw --version

# If not installed:
npm install -g openclaw@latest
```

### Step 4.2: Create OpenClaw Directories

```bash
# These should exist, but verify
mkdir -p ~/.openclaw/logs
mkdir -p ~/.openclaw/workspace-orchestrator
mkdir -p ~/.openclaw/workspace-backend
mkdir -p ~/.openclaw/workspace-frontend
mkdir -p ~/.openclaw/workspace-qa
mkdir -p ~/.openclaw/workspace-devops
```

### Step 4.3: Run Health Check

```bash
# Navigate to project
cd /path/to/your/github/repo

# Run the health check script
./scripts/health-check.sh

# Expected output: All ✓ checkmarks
```

**Status after Phase 4:** ✅ OpenClaw ready

---

## ⚙️ PHASE 5: Deploy Configuration (5 min)

### Step 5.1: Copy Configuration

```bash
# Use the OrbStack optimized config
cp openclaw-mac-studio-36gb-orbstack.json ~/.openclaw/openclaw.json

# Verify it's there
ls -la ~/.openclaw/openclaw.json
```

### Step 5.2: Restart OpenClaw Gateway

```bash
# Restart the gateway
openclaw gateway restart

# Wait 5 seconds for restart
sleep 5

# Verify it's running
curl http://127.0.0.1:18789/health

# Expected: {"status": "ok"}
```

### Step 5.3: Verify Agents

```bash
# List all agents
openclaw agents list --bindings

# Expected output: All 5 agents listed as active
# 🧠 Orchestrator: active
# ☕ Backend: active
# ⚛️  Frontend: active
# 🧪 QA: active
# ☁️ DevOps: active
```

**Status after Phase 5:** ✅ OpenClaw configured and running

---

## 🧪 PHASE 6: First Test Run (5 min)

### Step 6.1: Warm Ollama Cache

```bash
# Pre-load models into RAM (eliminates cold start)
./scripts/warm-cache.sh

# This runs a test prompt to load Gemma4 into memory
# Takes 2-3 minutes
```

### Step 6.2: Monitor System

```bash
# Open a NEW terminal and run monitoring
./scripts/monitor-agents.sh

# Keep this running to see real-time metrics
```

### Step 6.3: Test Send First Task

```bash
# Send a simple test via Telegram bot
# Message: "Write a simple Spring Boot REST API endpoint that returns Hello World"

# Watch the agents work:
# - Orchestrator creates a plan
# - Backend writes the code
# - QA generates tests
# - DevOps creates deployment files

# Monitor script will show:
# - Memory usage
# - Agent status
# - API latency
# - Processing progress
```

### Step 6.4: Verify Output

```bash
# Check agent workspaces for generated files
ls -la ~/.openclaw/workspace-backend/
ls -la ~/.openclaw/workspace-qa/
ls -la ~/.openclaw/workspace-devops/

# You should see generated code files
```

**Status after Phase 6:** ✅ Agents working, ready for production use

---

## 📋 COMPLETE SETUP CHECKLIST

### Pre-Installation
- [ ] Node.js 22+ installed
- [ ] OpenClaw CLI installed
- [ ] OrbStack app downloaded
- [ ] Ollama app downloaded
- [ ] API keys set in ~/.zshrc
- [ ] GitHub repo cloned locally

### Installation Phase
- [ ] Phase 1: Prerequisites verified (5 min)
- [ ] Phase 2: OrbStack started (2 min)
- [ ] Phase 3: Ollama models downloaded (20 min)
- [ ] Phase 4: OpenClaw setup (10 min)
- [ ] Phase 5: Configuration deployed (5 min)
- [ ] Phase 6: First test successful (5 min)

### Total Time: ~1 hour (mostly Ollama download time)

---

## ⚠️ POTENTIAL CHALLENGES & SOLUTIONS

### Challenge 1: Ollama Model Download Too Slow

**Problem:** Internet is slow, Gemma4 26B taking > 30 min

**Solutions:**
```bash
# Option A: Check download speed
curl -w '%{speed_download}\n' -o /dev/null -s https://github.com

# Option B: Run overnight
# Start the pull, let it run overnight

# Option C: Use smaller model first
ollama pull mistral:latest  # Much smaller (~4GB)
# Later: ollama pull gemma4:26b

# Option D: Check disk space
df -h  # Make sure you have 50GB free for downloads + extraction
```

### Challenge 2: OrbStack Won't Start

**Problem:** OrbStack app doesn't start

**Solutions:**
```bash
# Check if it's already running
pgrep -f orbstack

# Restart it
orbctl stop
sleep 5
orbctl start
sleep 10

# Or restart via app
killall -9 OrbStack
open /Applications/OrbStack.app
```

### Challenge 3: Ollama Models Keep Unloading

**Problem:** Ollama says "model not found" after first use

**Solutions:**
```bash
# Ollama unloads models after 5 min of inactivity
# Solution: Run warm-cache before starting

./scripts/warm-cache.sh

# Or adjust Ollama settings
export OLLAMA_KEEP_ALIVE=1h  # Keep models loaded for 1 hour
```

### Challenge 4: Not Enough Disk Space

**Problem:** Can't download Gemma4

**Solutions:**
```bash
# Check available space
df -h

# Clean up Docker images
docker system prune -a --volumes

# Delete old Ollama models if any
ollama rm mistral:latest  # Remove if not using

# Free up space on Mac
# Empty Trash
# Remove old files
```

### Challenge 5: API Keys Not Recognized

**Problem:** "ANTHROPIC_API_KEY not set"

**Solutions:**
```bash
# Edit ~/.zshrc
nano ~/.zshrc

# Add these lines:
export ANTHROPIC_API_KEY="sk-..."
export GEMINI_API_KEY="AIza..."
export TELEGRAM_BOT_TOKEN="123..."

# Save (Ctrl+X, then Y, then Enter)

# Reload
source ~/.zshrc

# Verify
echo $ANTHROPIC_API_KEY  # Should show your key
```

### Challenge 6: Telegram Bot Not Receiving Messages

**Problem:** Messages sent but not received by orchestrator

**Solutions:**
```bash
# Verify bot token is correct
openclaw channels status --probe

# Recreate bot:
# 1. Go to @BotFather on Telegram
# 2. /mybots → select your bot
# 3. /newtoken
# 4. Update .zshrc with new token
# 5. source ~/.zshrc
# 6. openclaw gateway restart
```

### Challenge 7: Memory Running Out (System Slow)

**Problem:** "Memory pressure: Yellow" in Activity Monitor

**Solutions:**
```bash
# Check what's using memory
top -o %MEM

# Free up space:
# 1. Close web browser
# 2. Close other IDEs
# 3. Reduce agent parallelism (run fewer agents)

# Or reduce agent memory limits:
# Edit: ~/.openclaw/openclaw.json
# Lower the "resourceLimits.memory" values
# Restart: openclaw gateway restart
```

### Challenge 8: Agents Running Very Slowly

**Problem:** Agents take > 1 min to respond

**Solutions:**
```bash
# Check what's slow
./scripts/monitor-agents.sh

# Check if Ollama is responsive
curl http://localhost:11434/api/tags

# Check if models are loaded
ollama list

# If Gemma4 not loaded:
./scripts/warm-cache.sh

# Monitor latency:
time curl http://localhost:11434/api/generate -X POST \
  -H "Content-Type: application/json" \
  -d '{"model":"gemma4:26b","prompt":"test"}'
```

---

## 🎯 SUCCESS INDICATORS

### Phase 1-2 Success
```bash
docker ps              # ✓ Should work
docker info | grep "Operating System"  # ✓ Should show "orbstack"
```

### Phase 3 Success
```bash
ollama list            # ✓ Shows gemma4:26b and llama2:7b
curl http://localhost:11434/api/tags  # ✓ Returns JSON
```

### Phase 4 Success
```bash
./scripts/health-check.sh  # ✓ All checks pass
```

### Phase 5 Success
```bash
curl http://127.0.0.1:18789/health  # ✓ Returns {"status":"ok"}
openclaw agents list    # ✓ Shows all 5 agents active
```

### Phase 6 Success
```bash
# You send: "Write a Hello World Spring Boot endpoint"
# Agents generate code
# Monitor shows activity
# 🧠 Orchestrator processes request
# ☕ Backend generates code
# 🧪 QA writes tests
# ☁️ DevOps creates manifests
```

---

## 📊 EXPECTED PERFORMANCE

### First Run (Cold Start)
- Orchestrator: ~3-5s to respond
- Backend: ~5-10s to generate code
- QA: ~10-15s for tests (first load of Ollama)
- Total first task: ~30-45s

### Subsequent Runs (Warm Cache)
- Orchestrator: ~1-2s
- Backend: ~2-5s
- QA: ~3-8s (Ollama already loaded)
- Total: ~15-25s per task

### System Resources
- Memory used: 28-30GB (when all agents running)
- Available for browser: 6-8GB
- CPU usage: 20-40% (all 8 cores used by agents)

---

## 🚀 FULL SETUP COMMAND SEQUENCE (Copy & Paste)

```bash
# 1. Navigate to project
cd /path/to/your/repo

# 2. Start OrbStack
open /Applications/OrbStack.app

# 3. Wait 40s, then start Ollama
sleep 40
open /Applications/Ollama.app

# 4. Wait 30s, then download models
sleep 30
ollama pull gemma4:26b

# 5. While that's downloading, open new terminal for this:
# Run health check
./scripts/health-check.sh

# 6. After Ollama models ready, deploy config
cp openclaw-mac-studio-36gb-orbstack.json ~/.openclaw/openclaw.json

# 7. Restart OpenClaw
openclaw gateway restart

# 8. Warm cache
./scripts/warm-cache.sh

# 9. Start monitoring in new terminal
./scripts/monitor-agents.sh

# 10. Send first task via Telegram
# Message: "Write a Spring Boot service with GET /health endpoint"

# 11. Watch agents work in monitor terminal!
```

---

## ✅ INTEGRATION ISSUES TO EXPECT

### Likely Issues (Common)
1. **Ollama model download slow** - Expected, just wait
2. **First run slower** - Agents need to load models, normal
3. **Memory usage high** - Expected with all agents + Ollama
4. **Telegram messages delayed** - May take 5-10s, normal

### Unlikely but Possible Issues
1. **Port conflicts** (18789 already in use)
2. **API key problems** (typo in .zshrc)
3. **Disk space** (< 50GB free)
4. **Network issues** (can't reach APIs)

### How to Debug Any Issue
```bash
# 1. Check logs
tail -f ~/.openclaw/logs/gateway.log

# 2. Run health check
./scripts/health-check.sh

# 3. Monitor agents
./scripts/monitor-agents.sh

# 4. Check each component
docker ps           # OrbStack/Docker
ollama list         # Ollama
curl http://127.0.0.1:18789/health  # OpenClaw
echo $ANTHROPIC_API_KEY              # API keys
```

---

## 🎓 WHAT HAPPENS AFTER SETUP

### Immediately After (Minute 1)
- All 5 agents active and ready
- Ollama models in memory
- Ready to send tasks via Telegram

### First Hour
- Send several test tasks
- Observe agent interactions
- Verify code generation quality
- Monitor memory and performance

### First Day
- Run real work tasks
- Fine-tune agent behavior via SOUL.md files
- Test different workload combinations
- Verify all integrations working

### First Week
- Build confidence in agent reliability
- Optimize for your workflows
- Create standard task patterns
- Document common issues

---

## 🎉 FINAL TIMELINE

| Time | Activity | Status |
|------|----------|--------|
| **T+0min** | Start setup | Start here |
| **T+5min** | Prerequisites checked | ✓ |
| **T+7min** | OrbStack running | ✓ |
| **T+27min** | Ollama models ready | ✓ |
| **T+37min** | OpenClaw configured | ✓ |
| **T+42min** | Configuration deployed | ✓ |
| **T+47min** | First test successful | ✅ Ready! |
| **T+1hour** | Agents actively working | 🚀 Go! |

---

## 📞 WHEN TO CHECK DOCUMENTATION

- **Setup stuck?** → Read this file + GETTING_STARTED.md
- **Memory issues?** → Check MEMORY_ANALYSIS_FINAL.md
- **Performance slow?** → Check ORBSTACK_VS_DOCKER.md
- **Agent behavior?** → Check workspace-*/SOUL.md files
- **API costs?** → Check COST_TRACKING in GETTING_STARTED.md

---

**Status:** ✅ Ready to execute  
**Time estimate:** ~1 hour total  
**Difficulty:** Easy (mostly waiting for downloads)  
**Success rate:** 95%+ if following this guide  

🚀 **You're ready to launch!**


