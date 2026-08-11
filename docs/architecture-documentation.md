# Architecture Documentation

## Architecture Overview

Two independently deployable Spring Boot services (Recipient, Donor) communicate exclusively through RabbitMQ — never directly. An n8n workflow mediates the Donor's confirmation back to the Recipient, so no callback logic is hardcoded into either service. The Recipient owns all state, the receipt artifact, and the UI-facing API; the Donor's only job is to consume an event and emit a confirmation. Both services follow the same internal layering so scaffolding is consistent across the two.

## Core Layers

Applied identically inside both the Recipient Service and the Donor Service:

- **Domain Layer** — the `PortRequest` entity and its status (`INITIATED` → `COMPLETED`, per REQ-FR-SUB-2/REQ-FR-DASH-1); repository and event-publisher interfaces owned by the domain, with no framework dependencies.
- **Application Layer** — use cases that orchestrate the domain: `SubmitPortRequest` (Recipient), `AutoAcceptPortRequest` (Donor), `CompletePortRequest` (Recipient, invoked by the n8n callback).
- **Infrastructure Layer** — adapters implementing the domain's interfaces: JPA/MySQL repository (Recipient only — Donor is stateless per the concept doc's minimal-Donor-logic goal), RabbitMQ producer/consumer, REST controllers, and the MinIO client adapter (Recipient only, for the receipt).

## Cross-cutting Concerns

- **Error Handling** — each service returns a consistent error response shape from its REST layer; RabbitMQ consumer failures are logged, not retried (production-grade retry/dead-lettering is explicitly out of scope per the requirements doc).
- **Logging** — structured logs in both services, with a correlation ID generated at request submission and propagated through the RabbitMQ message and the n8n callback, so one port request can be traced end-to-end across all three systems.
- **Security** — explicitly none. No authentication, no authorization on any endpoint. This is a deliberate scope boundary, not an oversight (REQ exclusions list).
- **Configuration** — externalized via environment variables (12-factor style), with a `local` profile (Docker Compose hostnames) and a `cloud` profile (Kubernetes service DNS) so the same container images run unmodified in both environments.

## Integration Patterns

- **External Service Integration**: Recipient → RabbitMQ (publish `port.request.initiated`) → Donor (consume, auto-accept, publish `port.request.accepted`) → n8n (RabbitMQ trigger node consumes the confirmation) → n8n calls a REST callback on Recipient to close the loop.
- **Communication Methods**: Angular UI ↔ Recipient over REST/HTTPS; Recipient ↔ Donor only via AMQP (RabbitMQ) — never direct HTTP; n8n ↔ Recipient via a single REST callback; Recipient ↔ MinIO via the S3 API for the receipt object.
- **State Management**: The Recipient is the sole owner of `PortRequest` state. Dashboard updates reflect status without a manual reload (REQ-FR-DASH-2) via lightweight client-side polling of the Recipient's status endpoint — the simplest mechanism that satisfies the requirement without adding WebSocket/SSE infrastructure a POC doesn't need. Real-time push is a reasonable future enhancement, not a scaffolding requirement.

## Component Interactions

1. UI → `POST /api/v1/port-requests` on Recipient → row persisted (`INITIATED`) → event published to RabbitMQ.
2. Donor consumes the event → auto-accepts → publishes a confirmation event to RabbitMQ.
3. n8n's RabbitMQ trigger consumes the confirmation → calls back to Recipient.
4. Recipient's callback handler generates the receipt, uploads it to MinIO, and updates status to `COMPLETED`.
5. UI polls Recipient's status endpoint and reflects `COMPLETED` once step 4 finishes.

## Interface Contracts

- `POST /api/v1/port-requests` (Recipient, called by UI) — creates a request, returns its ID and `INITIATED` status.
- `GET /api/v1/port-requests/{id}` (Recipient, called by UI) — returns current status, polled by the dashboard.
- `POST /api/v1/port-requests/{id}/complete` (Recipient, called by n8n only) — marks `COMPLETED`, triggers receipt generation.
- AMQP `port.request.initiated` — published by Recipient, consumed by Donor.
- AMQP `port.request.accepted` — published by Donor, consumed by n8n.
- S3 `PutObject` — Recipient → MinIO, stores the confirmation receipt.
