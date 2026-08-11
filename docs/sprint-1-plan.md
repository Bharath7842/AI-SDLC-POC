# Sprint 1 Plan: Port-In / Port-Out Portability POC

This plan sequences all 24 scaffolding stories (13 Dev, 7 DevOps, 4 QA) into dependency-ordered waves. A wave is a set of stories that can start together because their dependencies are already satisfied — it's not a fixed calendar day, since actual duration depends on team size and story complexity. Stories within a wave can run in parallel across roles; stories across waves generally cannot.

## Wave 1 — Kickoff (no code dependencies)

| Story | Owner | Title |
|---|---|---|
| DEV-01 | Dev | Initialize monorepo structure |
| QA-01 | QA | Test plan mapped to the six success criteria |

**Why these two first:** DEV-01 unblocks every other Dev and DevOps story. QA-01 needs no code at all — it only needs the planning documents, so there's no reason for QA to sit idle while Dev scaffolds. Get QA-01 reviewed and agreed by the end of this wave; it's the yardstick the whole sprint gets measured against.

## Wave 2 — Project Scaffolds

| Story | Owner | Title | Depends on |
|---|---|---|---|
| DEV-02 | Dev | Scaffold Angular UI project | DEV-01 |
| DEV-03 | Dev | Scaffold Recipient Service project | DEV-01 |
| DEV-04 | Dev | Scaffold Donor Service project | DEV-01 |
| OPS-01 | DevOps | Docker Compose local environment (infra services) | DEV-01 |
| QA-02 | QA | Author REST API test cases (against architecture doc's contracts) | none (authoring only) |

**Why these together:** All four Dev/DevOps stories only need DEV-01 and are independent of each other, so they parallelize cleanly across however many people are available. QA-02 can be *authored* here even though there's no API to run it against yet — the interface contracts in the architecture doc are the source of truth, not the running code.

## Wave 3 — Dockerization, CI, and Domain Layers

| Story | Owner | Title | Depends on |
|---|---|---|---|
| DEV-05 | Dev | `PortRequest` domain entity + repository interface — Recipient | DEV-03 |
| DEV-06 | Dev | `PortRequest` domain representation — Donor | DEV-04 |
| OPS-02 | DevOps | Dockerfiles for Recipient, Donor, and UI | DEV-02, DEV-03, DEV-04 |
| OPS-03 | DevOps | Environment-variable configuration profiles | DEV-03, DEV-04, OPS-01 |
| OPS-05 | DevOps | CI pipeline (build + test, all three components) | DEV-02, DEV-03, DEV-04 |

**Why these together:** Everything here depends only on Wave 2's scaffolds. This is the busiest DevOps wave — Dockerfiles, config profiles, and CI can all be worked in parallel by different people since they touch different files, though OPS-03's environment variable names should be agreed with whoever's writing DEV-05/06 before those land.

## Wave 4 — Integration Adapters and Environment Verification

| Story | Owner | Title | Depends on |
|---|---|---|---|
| DEV-07 | Dev | RabbitMQ producer adapter — Recipient | OPS-03, DEV-05 |
| DEV-09 | Dev | MySQL persistence adapter — Recipient | OPS-03, DEV-05 |
| DEV-10 | Dev | MinIO client adapter — Recipient | OPS-01, OPS-03 |
| OPS-04 | DevOps | End-to-end local connectivity verification | OPS-01, OPS-03 |

**Why these together:** These are the three Recipient-side adapters (messaging, persistence, storage), plus the DevOps connectivity check that should really happen alongside them — there's little point building adapters against an environment nobody's verified is reachable. All three adapters are independent of each other, so they parallelize well.

## Wave 5 — Donor Completion and Test Harness

| Story | Owner | Title | Depends on |
|---|---|---|---|
| DEV-08 | Dev | RabbitMQ consumer + producer — Donor | OPS-03, DEV-06, DEV-07 |
| OPS-06 | DevOps | Container build & registry push scripts | OPS-02, OPS-05 |
| QA-03 | QA | RabbitMQ test harness (auto-accept + n8n callback validation) | OPS-01, OPS-04, DEV-08 |

**Why these together:** DEV-08 needs DEV-07's producer to exist first (Donor consumes what Recipient produces), which is why it lands a wave later than the other adapters. OPS-06 can run in parallel — it only needs the Dockerfiles and CI pipeline, not the messaging code. QA-03 is listed here for completeness but its actual execution trails DEV-08 within the wave.

## Wave 6 — App Structure and Cloud Manifests

| Story | Owner | Title | Depends on |
|---|---|---|---|
| DEV-11 | Dev | Recipient REST API scaffold | DEV-07, DEV-09, DEV-10 |
| DEV-12 | Dev | Angular submission form & dashboard shell | DEV-02, DEV-11 |
| DEV-13 | Dev | Base n8n workflow scaffold | OPS-01, DEV-08, DEV-11 |
| OPS-07 | DevOps | Kubernetes/Knative manifests | OPS-03, OPS-06, DEV-11 |

**Why these together:** DEV-11 is the linchpin — it needs all three Recipient adapters finished, and everything else in this wave (the UI shell, the n8n workflow, the K8s manifests) needs DEV-11 in turn. This is the wave where the individual pieces stop being isolated adapters and start being a connected system.

## Wave 7 — Sprint Acceptance

| Story | Owner | Title | Depends on |
|---|---|---|---|
| QA-02 (execute) | QA | Run authored REST API test cases against DEV-11 | DEV-11 |
| QA-04 | QA | End-to-end smoke test (UI → COMPLETED → receipt) | DEV-12, DEV-13, OPS-04 |

**Why these last:** Nothing here is new work — QA-02's test cases were written back in Wave 2 and are now executed against the real API. QA-04 is the sprint's acceptance gate: it walks the full flow described in the concept doc's "What Done Looks Like" section and checks it off against QA-01's test plan. Sprint 1 isn't done until this wave passes.

## Cross-Role Summary

- **Dev** carries the critical path (DEV-01 → DEV-02/03/04 → DEV-05/06 → DEV-07/08/09/10 → DEV-11 → DEV-12/13) — this is the sequence that determines the sprint's minimum length.
- **DevOps** front-loads environment and CI work (Waves 2–3) so Dev never blocks on missing infrastructure, then closes with containerization and cloud manifests (Waves 5–6) once there's something real to deploy.
- **QA** starts on day one with pure planning (Wave 1), authors executable test cases early against contracts rather than running code (Wave 2), builds an isolated messaging test harness alongside the Donor work (Wave 5), and closes the sprint with the acceptance gate (Wave 7) — QA is never idle waiting for "a build to test," because there's always a next artifact to plan or author against.
