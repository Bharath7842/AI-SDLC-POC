# Sprint 1: Epic-Story Structure — Usage Guide

This guide explains how to use the Sprint 1 Epic-and-Story structure for Jira backlog creation and sprint planning.

---

## Documents Provided

| Document | Purpose |
|----------|---------|
| `sprint-1-epics-stories.md` | **Detailed Epic & Story specifications** — includes acceptance criteria, dependencies, story points, wave assignments, and notes. **Use this as the source of truth.** |
| `sprint-1-jira-import.csv` | **Jira bulk import template** — CSV format ready for Jira import. Contains summaries and brief descriptions. |
| `SPRINT-1-USAGE-GUIDE.md` | This file — instructions for importing and planning. |

---

## Structure Overview

### Epics (6 Total)

**Development (3 Epics):**
- **DEV-E1: Project Infrastructure & Scaffolding** — Monorepo setup and project scaffolds
- **DEV-E2: Domain Layer & Integration Adapters** — Entity models, messaging, persistence, storage adapters
- **DEV-E3: REST API & Application Layer** — API endpoints, UI, orchestration workflow

**Quality Assurance (1 Epic):**
- **QA-E1: Test Planning & Automation Framework** — Test planning, Robot Framework automation, CI/CD integration

**DevOps (2 Epics):**
- **OPS-E1: Cloud-Agnostic Infrastructure & Containerization** — Docker, Kubernetes, cloud-agnostic deployment
- **OPS-E2: Observability & Operational Readiness** — Logging, health checks, operational runbook

### Stories (27 Total)

| Role | Epic | Stories | Points |
|------|------|---------|--------|
| **Dev** | DEV-E1 | DEV-S1 to DEV-S4 | 18 |
| **Dev** | DEV-E2 | DEV-S5 to DEV-S10 | 28 |
| **Dev** | DEV-E3 | DEV-S11 to DEV-S13 | 17 |
| **QA** | QA-E1 | QA-S1 to QA-S5 | 27 |
| **OPS** | OPS-E1 | OPS-S1 to OPS-S7 | 39 |
| **OPS** | OPS-E2 | OPS-S8 to OPS-S9 | 9 |
| **TOTAL** | 6 | 27 | **139 points** |

---

## Importing into Jira

### Option 1: CSV Import (Recommended for First-Time Setup)

**Steps:**

1. **Prepare Jira Project:**
   - Navigate to your Jira project (KAN - AI SDLC)
   - Go to **Project Settings → Issue Types**
   - Ensure "Story" and "Epic" issue types exist

2. **Create Epics First (Manual or via API):**
   - Create 6 Epics manually or via Jira API:
     - `DEV-E1: Project Infrastructure & Scaffolding`
     - `DEV-E2: Domain Layer & Integration Adapters`
     - `DEV-E3: REST API & Application Layer`
     - `QA-E1: Test Planning & Automation Framework`
     - `OPS-E1: Cloud-Agnostic Infrastructure & Containerization`
     - `OPS-E2: Observability & Operational Readiness`
   - **Note Epic URLs** (you'll need these for CSV import)

3. **Prepare CSV File:**
   - Open `sprint-1-jira-import.csv`
   - If your Jira custom fields use different names (e.g., "Estimation" instead of "Story Points"), update column headers
   - Replace `Epic Link` values with actual Epic URLs or Keys (e.g., `KAN-100` for DEV-E1)

4. **Import via Jira:**
   - Go to **Project Settings → Import/Export** (or use Jira's API)
   - Select CSV import
   - Map columns: Summary → Summary, Story Type → Issue Type, Story Points → Story Points, etc.
   - Preview and confirm
   - Click **Import**

5. **Post-Import Validation:**
   - All 27 stories appear in backlog
   - Epics linked correctly
   - Labels applied (Wave-X, Role, Sprint-1)
   - Story Points assigned

### Option 2: Manual Creation (If CSV Import Not Available)

1. Create Epics first (see "Create Epics First" step above)
2. Open `sprint-1-epics-stories.md`
3. For each story:
   - Click **Create Issue** in Jira
   - Copy Summary from the markdown (e.g., "DEV-S1: Initialize Monorepo Structure")
   - Copy Description section (Acceptance Criteria, Dependencies, Notes)
   - Select Issue Type: Story
   - Assign Story Points from the markdown
   - Select Epic Link
   - Add Labels: Wave-X, Role, Sprint-1
   - Click **Create**

### Option 3: API Bulk Import (For Automation)

Use Jira REST API to create stories in bulk:

```bash
#!/bin/bash
# Example: Create a single story via API

curl -X POST \
  -H "Authorization: Bearer <JIRA_TOKEN>" \
  -H "Content-Type: application/json" \
  https://bhrthmahesh09.atlassian.net/rest/api/3/issues \
  -d '{
    "fields": {
      "project": {"key": "KAN"},
      "summary": "DEV-S1: Initialize Monorepo Structure",
      "description": "As a developer, I want to initialize the monorepo...",
      "issuetype": {"name": "Story"},
      "customfield_10001": 3,
      "customfield_10002": "DEV-E1",
      "labels": ["Wave-1", "DEV", "Sprint-1"]
    }
  }'
```

---

## Wave-Based Sprint Planning

### Understanding Waves

Stories are organized into **7 waves**, each representing a dependency-ordered set of work:

- **Wave 1:** Kickoff — No dependencies, can start immediately
- **Wave 2:** Scaffolds — Depends on Wave 1 completion
- **Wave 3–6:** Incremental feature development — Each wave depends on previous waves
- **Wave 7:** Acceptance & deployment — Final validation gate

### Sprint Planning by Wave

**Option A: Sequential (Recommended for small teams)**
- Sprint Week 1: Waves 1–2 (6 stories, ~15 points)
- Sprint Week 2: Waves 3–4 (9 stories, ~25 points)
- Sprint Week 3–4: Waves 5–6 (10 stories, ~35 points)
- Sprint Week 5–6: Wave 7 (2 stories, ~9 points)
- **Total Duration:** 5–6 weeks

**Option B: Parallel (Recommended for larger teams)**
- Assign Dev team to the critical path (DEV stories)
- Assign QA team to test planning (QA-S1 parallel with Wave 2)
- Assign DevOps team to infrastructure (OPS-S1 parallel with Wave 2)
- Wave dependencies prevent blocking — teams work in parallel within each wave

### Using Wave Labels in Jira

1. **Filter by Wave:**
   - Go to **Backlog**
   - Add filter: `labels = "Wave-1"` to see all Wave 1 stories
   - Or: `labels in ("Wave-1", "Wave-2")` for multiple waves

2. **Color-Code Swimlanes:**
   - Create 7 swimlanes labeled Wave-1 through Wave-7
   - Drag stories into corresponding swimlane
   - This visualizes dependencies and progress

3. **Velocity Tracking:**
   - Record actual points completed per wave
   - Compare to planned points to identify bottlenecks

---

## Role-Based Task Assignment

### Developer (13 Stories, 63 Points)

**Suggested Assignment Strategy:**
- Assign 1 Dev per Epic (3 Devs) or rotate if smaller team
- Critical Path: DEV-S1 → DEV-S3/S4 → DEV-S5 → DEV-S7/S9/S10 → DEV-S11 → DEV-S12/S13
- Frontend Dev: DEV-S2, DEV-S12
- Backend Dev: DEV-S3, DEV-S5, DEV-S7, DEV-S9, DEV-S11
- Backend Dev: DEV-S4, DEV-S6, DEV-S8, DEV-S10, DEV-S13

**Velocity Guidance:**
- Experienced dev: 15–20 points/week → completes ~2–3 stories/week
- Junior dev: 8–12 points/week → completes ~1–2 stories/week

### QA Engineer (5 Stories, 27 Points)

**Assignment:**
- All 5 stories typically assigned to single QA engineer or pair
- QA-S1 (test planning) starts in Wave 1 — no blockers
- QA-S2 (API tests) authored in Wave 2, executed in Wave 6
- QA-S3 (messaging tests) depends on infrastructure (Wave 5)
- QA-S4 (E2E tests) and QA-S5 (CI/CD) are final gate (Wave 7)

**Parallel Work:**
- Author tests (QA-S2) while developers build features
- This prevents QA from blocking at the end of sprint

### DevOps Engineer (9 Stories, 49 Points)

**Assignment:**
- Assign to 1–2 DevOps engineers
- Critical Path: OPS-S1 → OPS-S3 → OPS-S2 → OPS-S5/S6 → OPS-S7
- Infrastructure-heavy: OPS-S1, OPS-S2, OPS-S3, OPS-S5, OPS-S6, OPS-S7 (40 points)
- Observability: OPS-S8, OPS-S9 (9 points)

**Parallel Work:**
- OPS can work independently of Dev for infrastructure setup
- Coordination points: OPS-S3 (config profiles), OPS-S7 (Kubernetes deployment)

---

## Sprint Execution Checklist

### Pre-Sprint (Day -3)

- [ ] All stories reviewed and acceptance criteria agreed
- [ ] Epics created in Jira and linked to stories
- [ ] Story points estimated and team agreed
- [ ] Stories assigned to team members
- [ ] Dependencies visualized (use "Blocks" link type)
- [ ] Wave labels applied to all stories

### Sprint Start (Day 1)

- [ ] Team sync: Review Wave 1 stories and blockers
- [ ] Dev environment setup: Run OPS-S1 (Docker Compose)
- [ ] Create shared Slack channel for sprint updates
- [ ] Daily standups: What's done, what's in progress, blockers

### Mid-Sprint (Day 3–4)

- [ ] Wave 1 & 2 should be in progress or done
- [ ] No blockers on Wave 3 stories?
- [ ] QA-S1 (test plan) reviewed and approved
- [ ] Review actual velocity vs. planned (adjust if needed)

### Sprint End (Day 21–28)

- [ ] Wave 7 stories (acceptance tests) executed
- [ ] All stories in Done status or explicitly deferred
- [ ] Retrospective: What went well, what to improve
- [ ] Release notes: List of completed features for demo

---

## Acceptance Criteria Verification

Each story in the markdown has a **Checkbox Acceptance Criteria** list. Use this for acceptance:

**During Development:**
- Developer checks off criteria as they're completed
- Criteria are verifiable (testable, measurable)

**Before Story Closure:**
- QA verifies acceptance criteria are met
- All checkboxes should be checked
- If any unchecked, story is not done (move back to In Progress)

**Example from DEV-S11:**
```
- [ ] POST /api/v1/port-requests — Submits a new port request
  - [ ] Request body: { "customerId": "string" }
  - [ ] Response (201): { "id": "uuid", "status": "INITIATED", "createdAt": "ISO8601" }
  - [ ] Calls DEV-S7 producer to publish event
  - [ ] Persists via DEV-S9 adapter
```

---

## Common Issues & Troubleshooting

### Issue: Story Blocked by Dependency Not Done

**Solution:**
- Check Wave assignment — dependencies should be same or earlier wave
- If dependency delayed, defer story to next sprint or expedite dependency
- Update Jira "Blocks" link to show blocking story
- Flag in standups until resolved

### Issue: Story Points Underestimated

**Solution:**
- Stories can be split:
  - Example: DEV-S11 (6 points) might split into:
    - DEV-S11a: POST endpoint (3 points)
    - DEV-S11b: GET endpoint (2 points)
    - DEV-S11c: Callback endpoint (1 point)
- Move lower-priority sub-story to next sprint
- Adjust team velocity for future sprints

### Issue: QA Can't Execute Tests (Code Not Ready)

**Solution:**
- QA authors tests against interface contracts in parallel
- Don't block QA waiting for code — tests are ready when code lands
- Use "test contract" (from architecture doc) as truth, not running code

### Issue: DevOps Infrastructure Delayed

**Solution:**
- Infrastructure (Docker Compose, Kubernetes) blocks almost everything
- Prioritize OPS-S1 → OPS-S3 → OPS-S5 heavily
- Consider having a dedicated DevOps engineer for first 2 weeks
- Fallback: use public cloud sandbox environment if on-prem infra unavailable

---

## Performance Metrics

### Velocity Tracking

Record actual story points completed per wave:

```
Wave 1: 8 points (planned 8) — 100% on track ✓
Wave 2: 22 points (planned 23) — 96% on track
Wave 3: 25 points (planned 27) — 93% on track
Wave 4: 20 points (planned 24) — 83% on track ← Bottleneck
Wave 5: 28 points (planned 26) — 108% on track ✓
Wave 6: 35 points (planned 35) — 100% on track ✓
Wave 7: 9 points (planned 9) — 100% on track ✓
```

**Actions:**
- If velocity < 90%, investigate Wave 4 bottleneck
- If velocity > 110%, re-estimate points for next sprint
- Trend velocity over 2–3 sprints for accurate forecasting

### Cycle Time (Story → Done)

Track how long stories stay in progress:

- Target: < 5 days per story
- If stories > 1 week in progress → consider splitting or removing blockers

### Burndown

Create burndown chart showing planned vs. actual points completed per day:

- Ideal line: linear decline from 139 to 0 over sprint duration
- Actual line: may be lumpy (depends on wave structure)
- If lagging ideal by 20%+ midway → discuss acceleration plan

---

## Post-Sprint Demo & Handoff

### Demo Checklist

- [ ] POC endpoint accessible (Recipient Service API)
- [ ] Angular UI loads and can submit port request
- [ ] Submit request and observe dashboard updating to COMPLETED
- [ ] Confirmation receipt retrievable from MinIO
- [ ] Logs show correlation ID propagated end-to-end
- [ ] Test reports show all API/messaging/E2E tests passing
- [ ] Kubernetes manifests deploy to test cluster
- [ ] Documentation (runbook, architecture) up-to-date

### Documentation Handoff

- [ ] README updated with latest deployment instructions
- [ ] Runbook added to `/infra/RUNBOOK.md` for on-call team
- [ ] API docs generated (Swagger/OpenAPI from Spring Boot)
- [ ] Test results and coverage reports available
- [ ] Deployment artifacts (Docker images, Kubernetes manifests) versioned and tagged

---

## Next Sprint Planning (Sprint 2+)

**Post-POC Enhancements (Document for Product Backlog):**

1. **Authentication & Authorization** — Implement OAuth2 or JWT
2. **Manual Donor Review** — Add approval workflow before auto-accept
3. **Advanced Retry & Dead-Lettering** — Improve message reliability
4. **Notifications** — Email/Slack alerts on status changes
5. **Multi-Tenant Support** — Isolate data per tenant
6. **Metrics & Dashboards** — Prometheus/Grafana integration
7. **Helm Charts** — Package Kubernetes deployment
8. **Performance Optimization** — Caching, batch processing
9. **Security Hardening** — TLS, encryption at rest, secrets management
10. **Cost Optimization** — Auto-scaling, spot instances, resource right-sizing

---

## References

- **Architecture Doc:** `architecture-documentation.md`
- **Tech Stack:** `technology-stack.md`
- **Requirements:** `core-requirements.md`
- **Vision:** `vision-statement.md`
- **Sprint Plan:** `sprint-1-plan.md`

---
