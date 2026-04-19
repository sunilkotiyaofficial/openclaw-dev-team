# ☁️ DevOps — Topics Backlog

Interview study roadmap for cloud architecture, deployment, and SRE topics.

**How to use:**
- Topics ordered by priority (P0 = must-know for architect interviews)
- Trigger a deep dive in Slack: `@devops deep dive on {topic}`
- Check off when you can confidently explain + code + design under pressure

---

## P0 — Must Know (Architect Interviews Will Test These)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 1 | **GKE Autoscaling** (HPA + VPA + Cluster Autoscaler) | 🔸 Intermediate | ⬜ Not started | Layer differences, custom metrics, scaling latency. |
| 2 | **Blue-Green Deployment** | 🔹 Beginner | ⬜ Not started | Zero-downtime deploy, rollback strategy. |
| 3 | **Canary Release** | 🔸 Intermediate | ⬜ Not started | Traffic shaping (Istio, Cloud Run), Flagger. |
| 4 | **Workload Identity** (no static keys) | 🔸 Intermediate | ⬜ Not started | IAM binding to K8s SA, audit trail. |
| 5 | **CI/CD Pipeline Design** | 🔸 Intermediate | ⬜ Not started | Test gates, approval flows, artifact promotion. |
| 6 | **Disaster Recovery (RPO/RTO)** | 🔥 Advanced | ⬜ Not started | Backup strategy, region failover, DR drills. |
| 7 | **Cost Optimization Strategies** | 🔸 Intermediate | ⬜ Not started | Spot instances, committed use, right-sizing, scheduled scale-down. |

## P1 — Should Know (Common Questions)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 8 | **Container Security** (Distroless, SBOM, Binary Authorization) | 🔸 Intermediate | ⬜ Not started | CVE scanning, signing, policy enforcement. |
| 9 | **Multi-Region Deployment** | 🔥 Advanced | ⬜ Not started | Active-active vs active-passive, data replication lag. |
| 10 | **Observability** (Metrics + Logs + Traces — the 3 pillars) | 🔸 Intermediate | ⬜ Not started | Cardinality, sampling, correlation IDs. |
| 11 | **Secret Management** (Secret Manager, Vault, CSI driver) | 🔸 Intermediate | ⬜ Not started | Rotation, access patterns, audit. |
| 12 | **Service Mesh** (Istio vs Linkerd) | 🔥 Advanced | ⬜ Not started | mTLS, observability, traffic management. |
| 13 | **Network Policies & Zero Trust** | 🔸 Intermediate | ⬜ Not started | Deny-all default, allow-list, VPC-SC. |
| 14 | **Infrastructure as Code** (Terraform modules) | 🔸 Intermediate | ⬜ Not started | State management, workspaces, modules, remote backend. |

## P2 — Nice to Have (Shows Depth)

| # | Topic | Difficulty | Status | Notes |
|---|---|---|---|---|
| 15 | **GitOps** (Argo CD, Flux) | 🔸 Intermediate | ⬜ Not started | Pull-based CD, drift detection. |
| 16 | **Chaos Engineering** | 🔥 Advanced | ⬜ Not started | Gremlin, Litmus, game days. |
| 17 | **Cloud Run vs GKE** decision framework | 🔹 Beginner | ⬜ Not started | When each shines, migration path. |
| 18 | **FinOps** principles | 🔹 Beginner | ⬜ Not started | Cost allocation, chargeback, unit economics. |
| 19 | **SRE SLO/SLI/Error Budget** | 🔸 Intermediate | ⬜ Not started | Google SRE book concepts applied. |
| 20 | **Multi-Cloud Strategy** (GCP + AWS) | 🔥 Advanced | ⬜ Not started | Abstraction layers, vendor lock-in trade-offs. |

---

## Suggested Learning Path

**Week 1 — Deploy Fundamentals:** #2, #3, #5 (blue-green, canary, CI/CD)
**Week 2 — Scale:** #1, #7 (autoscaling, cost optimization)
**Week 3 — Security:** #4, #8, #11, #13 (identity, container security, secrets, network)
**Week 4 — Reliability:** #6, #10 (DR, observability)
**Week 5 — Advanced:** #9, #12, #14 (multi-region, service mesh, IaC)
**Ongoing:** P2 as interest dictates

---

## Cross-References

- **#1 GKE Autoscaling** pairs with `backend` → load testing, HPA metrics
- **#4 Workload Identity** pairs with `backend` → JWT/OAuth2 (auth patterns)
- **#10 Observability** pairs with `backend` → OpenTelemetry instrumentation
- **#12 Service Mesh** pairs with `backend` → circuit breaker (can move to mesh layer)

---

## Status Legend

- ⬜ Not started
- 🔨 In progress
- 📝 Studied
- 🎯 Quiz-ready
- ✅ Mastered

---

**Last updated:** 2026-04-18
**Total topics:** 20 (7 P0 + 7 P1 + 6 P2)
**Mastered:** 0 / 20
