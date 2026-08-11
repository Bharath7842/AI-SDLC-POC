# Core Requirements for Port-In / Port-Out Portability POC

## Functional Requirements

### Port Request Submission

- REQ-FR-SUB-1: The system shall allow a Recipient to submit a port request through an Angular UI.
- REQ-FR-SUB-2: The system shall persist submitted port requests with an initial status of `INITIATED`.

### Event-Driven Donor Processing

- REQ-FR-DON-1: The system shall publish an event when a port request is submitted.
- REQ-FR-DON-2: The Donor service shall consume the published event and automatically accept the port request, with no manual review step.

### Orchestration & Callback

- REQ-FR-ORC-1: The system shall route the Donor's confirmation back to the Recipient service via n8n, without hardcoding callback logic into either the Donor or Recipient service.

### Status Dashboard

- REQ-FR-DASH-1: The system shall update the request status to `COMPLETED` once the port flow finishes.
- REQ-FR-DASH-2: The dashboard shall reflect status changes without requiring a manual page reload.

### Confirmation Receipt

- REQ-FR-REC-1: The system shall generate a confirmation receipt upon completion of a port request.
- REQ-FR-REC-2: The generated receipt shall be retrievable from object storage (MinIO).

### Deployment & Portability

- REQ-FR-DEP-1: The system shall run via Docker Compose in a local environment.
- REQ-FR-DEP-2: The system shall be deployable, unmodified, to a CNCF-conformant Kubernetes/Knative cluster across AWS, Azure, and GCP.

## Explicitly Out of Scope

(Documented per the concept doc's scope boundaries — not requirements, but noted so they aren't accidentally assumed later.)

- Authentication / access control
- Manual donor review or rejection
- Multi-tenant support
- SLA timers / escalation
- Notifications
- Billing
- Production-grade retry / dead-lettering
