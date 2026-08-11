# Technology Stack Documentation

## Core Technology

- **Angular** 22.1.0 — Recipient-facing UI
- **Spring Boot** 4.1.0 — Recipient Service & Donor Service (two independently deployed instances of the same stack)
- **Docker Compose** v5.3.1 — local orchestration
- **Kubernetes** v1.36.3 + **Knative** 1.23 — cloud orchestration, cloud-agnostic (AWS/Azure/GCP)

## Required Dependencies

### Frontend Runtime

- **Node.js** ^22.12.0 (22.x LTS)
  - Purpose: build/runtime environment for the Angular UI
  - Chosen because: Angular 22 requires Node.js 22+ (Node 20 support was dropped at the v22 release)
- **TypeScript** ^6.0.0
  - Purpose: Angular's implementation language
  - Chosen because: Angular 22 requires TypeScript 6.0+; 5.9 and below are no longer supported

### Backend Runtime

- **Java** 25 LTS (OpenJDK/Temurin 25.0.3)
  - Purpose: runtime for both Spring Boot services
  - Chosen because: it's the current LTS (2-year cadence: 21 → 25), and Spring Boot 4.1.0 supports it (min Java 17, max Java 26). Running an LTS keeps Recipient and Donor on a build with the longest security-patch runway.

### Messaging

- **RabbitMQ** 4.3.4 (topic exchange)
  - Purpose: fully decouples Recipient and Donor — neither service calls the other directly
  - Chosen because: current stable release; actively maintained by Broadcom/VMware Tanzu
  - Requires: Erlang/OTP 27.0 or later (RabbitMQ 4.3.x will not start on older Erlang releases)

### Orchestration

- **n8n** 2.33.7
  - Purpose: routes the Donor's confirmation back to the Recipient with no custom code in either service
  - Chosen because: current stable release; n8n ships weekly patches, so re-verify before a production pull

### Persistence

- **MySQL** 8.4 LTS (8.4.10)
  - Purpose: system of record for port-request status/audit data
  - Chosen because: MySQL 8.0 reached end-of-life at 8.0.46 in April 2026 with no further security patches. 8.4 is the actively maintained LTS line.

### Object Storage

- **MinIO** `RELEASE.2025-10-15T17-29-55Z` (last community/open-source release)
  - Purpose: stores the generated confirmation receipt
  - ⚠️ **Accepted risk, documented per your decision:** MinIO's open-source line was marked unmaintained by MinIO Inc. between February and April 2026, with the company steering users toward its paid AIStor product. This pinned release is the final community build — it works, but it will **not** receive further security patches. This is worth revisiting as a "future enhancement" (see Requirements doc) — actively maintained S3-compatible alternatives like SeaweedFS or Garage remain available if this POC's runway extends.

## Compatibility Matrix

| Component | Depends on | Constraint | Status |
|---|---|---|---|
| Angular 22.1.0 | Node.js | ^22.12.0+ | ✅ compatible |
| Angular 22.1.0 | TypeScript | ^6.0.0+ | ✅ compatible |
| Spring Boot 4.1.0 | Java | 17 min, 26 max | ✅ Java 25 LTS compatible |
| Spring Boot 4.1.0 | Spring Framework | 7.0.8+ (bundled transitively) | ✅ compatible |
| RabbitMQ 4.3.4 | Erlang/OTP | 27.0 minimum | ✅ compatible (pin Erlang 27+ in container image) |
| Knative 1.23 | Kubernetes | 1.34 minimum | ✅ K8s 1.36.3 exceeds minimum |
| MinIO (pinned) | — | unmaintained upstream | ⚠️ accepted risk, no forward compatibility guarantee |

## Notes on Version Currency

These versions were verified current as of **August 8, 2026**. Because RabbitMQ, n8n, Kubernetes, and Knative all ship frequent patch releases, re-verify patch-level versions immediately before the actual scaffolding/build phase — this document should not sit unused for more than a few weeks before implementation starts.
