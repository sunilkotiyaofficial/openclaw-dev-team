# ⏱️ QUICK EXECUTION TIMELINE

**Current Status:** OrbStack & Ollama downloaded, code committed to GitHub, nothing running  
**Objective:** Get agents running and writing code  
**Time to First Agent Task:** ~1 hour

---

## 🚀 QUICK START (Just the Commands)

### Terminal Window 1: Setup

```bash
# T+0: Check prerequisites (5 min)
node -v                    # Check Node.js
npm -v                     # Check npm
openclaw --version         # Check OpenClaw
echo $ANTHROPIC_API_KEY    # Check API key
echo $GEMINI_API_KEY       # Check API key

# T+5: Start OrbStack (2 min)
open /Applications/OrbStack.app
sleep 40

# T+7: Start Ollama (2 min)
open /Applications/Ollama.app
sleep 30

# T+9: Download Ollama models (20+ min)
# THIS IS THE LONGEST STEP - Just wait
ollama pull gemma4:26b

# ⏳ While models downloading, open ANOTHER terminal and continue below...

# T+27: When Gemma4 done, verify
ollama list              # Should show gemma4:26b
ollama pull llama2:7b    # Optional: pull fallback

# T+37: Setup OpenClaw
cd /path/to/your/repo
./scripts/health-check.sh    # Verify all OK

# T+42: Deploy config
cp openclaw-mac-studio-36gb-orbstack.json ~/.openclaw/openclaw.json
openclaw gateway restart
sleep 5

# T+47: Ready for testing!
echo "✅ Setup complete!"
```

### Terminal Window 2: Monitoring (Start after T+30)

```bash
# T+30: Open new terminal, navigate to repo
cd /path/to/your/repo

# T+35: Run monitoring
./scripts/monitor-agents.sh

# Keep this running to watch agents work
```

### Terminal Window 3: Testing (Start after T+47)

```bash
# T+47+: Send first task via Telegram
# Message: "Write a simple Spring Boot REST API endpoint that returns 'Hello World'"

# Watch in monitoring terminal:
# - Memory usage update
# - Agent status changes
# - Responses come back
# - Code gets generated
```

---

## 📊 TIME BREAKDOWN

| Phase | Task | Duration | Cumulative |
|-------|------|----------|-----------|
| 1 | Prerequisites | 5 min | 5 min |
| 2 | OrbStack startup | 2 min | 7 min |
| 3 | Ollama startup | 2 min | 9 min |
| 3 | **Model download** | **20 min** | **29 min** |
| 3 | Model verification | 1 min | 30 min |
| 4 | OpenClaw setup | 10 min | 40 min |
| 5 | Config deploy | 5 min | 45 min |
| 6 | First test | 5 min | **50 min** |
| | **TOTAL** | | **~1 hour** |

*Model download time varies: 15-40 min depending on internet speed*

---

## ✅ SUCCESS CHECKLIST

After each phase, verify:

**After Phase 1 (T+5):**
```bash
node -v               # ✓ 22+
openclaw --version    # ✓ Shows version
echo $ANTHROPIC_API_KEY  # ✓ Shows key (not empty)
```

**After Phase 2 (T+7):**
```bash
docker ps             # ✓ Works
orbctl status         # ✓ "OrbStack is running"
```

**After Phase 3a (T+27):**
```bash
ollama list           # ✓ Shows gemma4:26b
curl http://localhost:11434/api/tags  # ✓ Returns JSON
```

**After Phase 4 (T+40):**
```bash
./scripts/health-check.sh  # ✓ All green checkmarks
```

**After Phase 5 (T+45):**
```bash
curl http://127.0.0.1:18789/health  # ✓ {"status":"ok"}
openclaw agents list        # ✓ All 5 agents active
```

**After Phase 6 (T+50):**
```bash
# Telegram message sent
# Monitor shows activity
# Agents processing
# Code generated ✅
```

---

## 🎯 ONE-LINER COMMANDS

### Quick Status Check (anytime)

```bash
# Is everything running?
docker ps && ollama list && curl http://127.0.0.1:18789/health && echo "✅ All good!"

# Monitor agents
./scripts/monitor-agents.sh

# Run health check
./scripts/health-check.sh
```

---

## ⚡ POTENTIAL BLOCKERS & FIXES

| Blocker | Fix | Time |
|---------|-----|------|
| Ollama download slow | Just wait, or use smaller model first | +10-20 min |
| OrbStack won't start | `brew services start orbstack` | +2 min |
| API key error | `source ~/.zshrc` in terminal | +1 min |
| Agents won't respond | Restart: `openclaw gateway restart` | +2 min |
| Out of disk space | Run: `docker system prune -a` | +5 min |

---

## 🚨 CRITICAL STEPS (Don't Skip!)

1. ✅ **Wait for Ollama models to fully download** - Don't rush Phase 3
2. ✅ **Source .zshrc after setting env vars** - Otherwise keys won't be found
3. ✅ **Restart OpenClaw after config change** - New config won't load otherwise
4. ✅ **Warm Ollama cache before first big task** - Makes first response 10x faster
5. ✅ **Use correct config file** - Use `openclaw-mac-studio-36gb-orbstack.json`

---

## 🎓 WHAT TO EXPECT DURING SETUP

**T+0-5:** Quick checks, should all pass ✓

**T+5-7:** OrbStack appears in dock, then starts ✓

**T+7-9:** Ollama appears in dock, then starts ✓

**T+9-29:** Long wait while Gemma4 26B (26GB) downloads
- Will see: `Downloading: ████░░░░░ 40%`
- This is normal, just wait
- Don't interrupt

**T+29-37:** Quick Ollama model verification and backend setup
- May see some technical output, all normal

**T+37-42:** OpenClaw restarts
- May see: `gateway restarting...`
- Wait for it to come back up

**T+42-47:** Agents should appear active
- See all 5 agents in monitoring if running
- No errors expected

**T+47+:** Send Telegram message and watch agents work! 🎉

---

## 📱 YOUR FIRST TASK (After T+50)

Send this via Telegram to your bot:

```
Build a Spring Boot REST API with:
- GET /health endpoint returns {"status": "ok"}
- GET /api/greeting endpoint returns {"message": "Hello World"}
- Include unit tests
- Include Docker deployment manifest
```

**What will happen:**
1. 🧠 Orchestrator analyzes and creates a plan (3-5s)
2. ☕ Backend generates Spring Boot code (5-10s)
3. 🧪 QA generates test cases (10-15s)
4. ☁️ DevOps generates Docker files (5-10s)
5. 🧠 Orchestrator summarizes and sends results (3-5s)

**Total time:** ~30-50s for full task

**Files generated:**
- Spring Boot service code
- JUnit tests
- Dockerfile
- Kubernetes manifests
- README with instructions

---

## 🔍 HOW TO DEBUG IF STUCK

```bash
# Check logs
tail -f ~/.openclaw/logs/gateway.log | head -50

# Check if all services running
docker ps                           # Docker containers
ollama list                         # Ollama models
curl http://127.0.0.1:18789/health  # OpenClaw

# Check agent status
openclaw agents list --bindings

# Check memory
top -l 1 | head -20

# Check disk
df -h
```

---

## 💾 SAVE THIS FOR REFERENCE

Bookmark or print this file. You'll need it for:
- Troubleshooting
- Restarting services
- Understanding timelines
- Quick command reference

---

## 🚀 READY? LET'S GO!

**Start with Phase 1 above and follow the timeline.**

**Estimated time to working agents: ~1 hour**

**Most of that time is just waiting for Ollama to download the 26GB model.**

**After that, you'll have a fully functional multi-agent development team!**

---

**Next: Follow COMPLETE_SETUP_GUIDE.md for detailed steps**


