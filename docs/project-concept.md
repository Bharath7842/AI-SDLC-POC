# Project Concept: Port-In / Port-Out Portability POC

## 1. Elevator Pitch

A minimal, end-to-end proof-of-concept that demonstrates how a customer or entity can be
**"ported"** from one organization (the **Donor**) to another (the **Recipient**) through a
fully automated, event-driven workflow — with no manual approval steps and no
authentication getting in the way of demonstrating the toolchain itself.

Think mobile number portability, insurance policy switching, or subscription/account
transfer — the *pattern* is identical across all of them: a Recipient asks for something,
a Donor releases it, and a record moves from one system of ownership to another. This POC
builds that pattern once, generically, and proves out the technical plumbing needed to
support it in production later.

## 2. The Problem

Portability workflows (numbers, policies, accounts, subscriptions) are common but usually
get bogged down in domain-specific complexity — regulatory rules, manual review queues,
SLA clocks, billing reconciliation — before anyone has proven the underlying **integration
architecture** actually works. Teams often can't get a clean answer to basic questions like:

- Can two independent services communicate reliably through async messaging alone?
- Can a no-code orchestration layer (n8n) mediate between them without hardcoding logic
  into either service?
- Does the whole thing containerize and run identically across clouds?

This POC strips away the domain complexity so those architectural questions can be
answered quickly and cheaply.

## 3. The Concept

A **Recipient** submits a port request through a simple Angular UI. That request is
persisted and published as an event. A **Donor** service picks it up and — since this is a
POC with no manual gate — **auto-accepts instantly**. The confirmation flows back through
an **n8n** orchestration layer, which calls back into the Recipient service to close the
loop, generate a receipt, and update the dashboard. No login, no human-in-the-loop, no
domain-specific business rules — just the raw mechanics of a two-party async handoff.

The two attached diagrams capture this precisely:

- `architecture-diagram.mermaid` — the static system view (UI → Recipient Service →
  MySQL/RabbitMQ → Donor Service → n8n → back to Recipient → MinIO).
- `port-flow-sequence.mermaid` — the same flow as a time-ordered sequence, showing exactly
  which service calls which, and in what order, from submission to `COMPLETED` status.

## 4. Core Actors

| Actor | Role |
|---|---|
| **Recipient** | Gaining party — initiates the port request via the UI |
| **Donor** | Losing party — auto-accepts the request, no manual review |
| **n8n** | No-code orchestrator — bridges Donor confirmation back to Recipient |
| **Customer / Entity** | The thing being ported (number, policy, account) — a passive subject of the request, not a system actor |

## 5. Why This Architecture

| Choice | Reasoning |
|---|---|
| RabbitMQ between Recipient and Donor | Proves the two parties can stay fully decoupled — neither calls the other directly |
| n8n as the callback layer | Demonstrates a no-code integration pattern instead of hardcoding the callback into the Donor service, keeping Donor logic minimal |
| MySQL + MinIO | Separates structured status/audit data from the generated receipt artifact |
| Docker Compose locally, Kubernetes/Knative in the cloud | Same containers run unmodified from a laptop to any CNCF-conformant cluster (EKS/AKS/GKE), with Knative proving scale-to-zero economics for bursty request-driven services |
| No authentication | Deliberately removes a whole axis of complexity so the POC stays focused on the messaging/orchestration pattern, not access control |

## 6. What "Done" Looks Like (Success Criteria)

The POC is successful if a single demo run can show, without any manual intervention:

1. A port request submitted in the Angular UI.
2. The request persisted with `status = INITIATED`.
3. The Donor service auto-accepting the request purely by consuming a RabbitMQ event.
4. n8n routing the Donor's confirmation back to the Recipient service with zero custom code.
5. The dashboard reflecting `status = COMPLETED` without a page reload triggering the update manually.
6. A confirmation receipt retrievable from MinIO.

## 7. Scope Boundaries

**In scope:** request submission, event-driven auto-accept, status dashboard, confirmation
receipt, containerized local + cloud-agnostic deployment.

**Deliberately out of scope (for now):** authentication, manual donor review/rejection,
multi-tenant support, SLA timers/escalation, notifications, billing, production-grade
retry/dead-lettering. These are documented as explicit **future enhancements** rather than
gaps — the POC is scoped tightly on purpose.

## 8. Technology Stack at a Glance

Angular (frontend) · Spring Boot/Quarkus REST services (Recipient + Donor) · RabbitMQ
(topic exchange) · n8n (orchestration) · MySQL 8 (system of record) · MinIO (receipt
storage) · Docker Compose (local) · Kubernetes + Knative (cloud, cloud-agnostic across
AWS/Azure/GCP).

## 9. Path Beyond the POC

Once the architecture is validated, the natural next steps (already captured in the
technical spec as future enhancements) are: adding real authentication, replacing
auto-accept with genuine manual donor review, adding retries/dead-lettering, notifications,
multi-donor directory support, and observability — turning the proven pattern into a
production-ready system.

## 10. Related Documents

- `port-in-port-out-spec.md` — full technical specification: data model, API contracts,
  messaging topology, n8n workflow detail, GUI wireframes, Docker Compose, and
  Kubernetes/Knative deployment manifests.
- `architecture-diagram.mermaid` — static architecture flowchart.
- `port-flow-sequence.mermaid` — end-to-end sequence diagram.
