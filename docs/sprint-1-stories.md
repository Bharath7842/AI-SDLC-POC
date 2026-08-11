# Sprint 1: Scaffolding Stories (Dev / QA / DevOps)

Story ID format: `[ROLE]-[NN]` — `DEV` for developer stories, `OPS` for DevOps/infrastructure stories, `QA` for quality-assurance stories. Renumber freely if your tracker uses a different scheme.

## Project Creation & Configuration (Dev)

**DEV-01**

As a developer, I want to initialize the monorepo directory structure so that all four components (UI, Recipient, Donor, infra) have a consistent, predictable home.

Acceptance Criteria:
- Repository contains `/ui`, `/recipient-service`, `/donor-service`, `/infra` at the root
- Root `README.md` documents the layout and links to the four planning documents (requirements, tech stack, architecture)
- `.gitignore` covers Node, Maven, and Docker build artifacts

Dependencies: none

Notes:
- The only story with no dependency — QA-01 can start in parallel, everything else waits on this

---

**DEV-02**

As a developer, I want to scaffold the Angular UI project so that frontend work has a running baseline.

Acceptance Criteria:
- `ng new` run inside `/ui` using Angular 22.1.0 (per `package.json` in the tech stack doc)
- Project builds and serves with `ng serve` showing the default Angular welcome page
- Node.js ^22.12.0 and TypeScript ^6.0.0 confirmed via `node -v` / `tsc -v`

Dependencies: DEV-01

Notes:
- Use standalone components (Angular 22 default) — no NgModules

---

**DEV-03**

As a developer, I want to scaffold the Recipient Service Spring Boot project so that backend work has a running baseline.

Acceptance Criteria:
- Maven project created in `/recipient-service` using the locked `pom.xml` (Spring Boot 4.1.0, Java 25)
- Project builds with `mvn clean install` and starts with `mvn spring-boot:run`
- Health check endpoint (`/actuator/health`) returns `UP`

Dependencies: DEV-01

Notes:
- Add `spring-boot-starter-actuator` for the health endpoint — needed for OPS-04's connectivity verification too

---

**DEV-04**

As a developer, I want to scaffold the Donor Service Spring Boot project so that it can independently consume events from the Recipient.

Acceptance Criteria:
- Maven project created in `/donor-service`, copied from the Recipient's `pom.xml` template with `artifactId` changed to `donor-service`
- `spring-boot-starter-data-jpa` and `mysql-connector-j` removed — Donor is stateless per the architecture doc
- Project builds and starts independently of the Recipient service

Dependencies: DEV-01

Notes:
- Keep this service deliberately minimal — the architecture doc calls out "no manual review, minimal Donor logic" as a design goal

---

## Development Environment & Infrastructure Setup (DevOps)

**OPS-01**

As a DevOps engineer, I want the Docker Compose environment running locally so that RabbitMQ, MySQL, MinIO, and n8n are available for integration work.

Acceptance Criteria:
- `docker-compose.yml` (from the tech stack doc) placed in `/infra`
- `docker compose up` starts all four services without errors
- RabbitMQ management UI reachable at `localhost:15672`, MySQL on `3306`, MinIO on `9000`, n8n on `5678`

Dependencies: DEV-01

Notes:
- MinIO image is the pinned `RELEASE.2025-10-15T17-29-55Z` — the last maintained open-source build; see the tech stack doc's accepted-risk note

---

**OPS-02**

As a DevOps engineer, I want Dockerfiles for the Recipient Service, Donor Service, and Angular UI so that all three application components can be containerized alongside the infrastructure services.

Acceptance Criteria:
- Each service has a multi-stage Dockerfile producing a minimal runtime image (Java 25 JRE for the two Spring Boot services, static Nginx serve for the Angular build)
- `docker build` succeeds for all three without errors
- Resulting images run standalone with `docker run` and respond on their expected ports

Dependencies: DEV-02, DEV-03, DEV-04

Notes:
- Required before OPS-06 (registry push) and OPS-07 (Kubernetes manifests) — nothing can deploy without an image to deploy

---

**OPS-03**

As a DevOps engineer, I want environment-variable-based configuration profiles so that the same containers run unmodified locally and in the cloud.

Acceptance Criteria:
- Both Spring Boot services support a `local` profile (Docker Compose hostnames: `rabbitmq`, `mysql`, `minio`) and a `cloud` profile (Kubernetes service DNS)
- `.env.local` and `.env.cloud` templates checked into `/infra`
- Switching profiles requires only an environment variable change, no rebuild

Dependencies: DEV-03, DEV-04, OPS-01

Notes:
- Application-side `@Profile` wiring is written by Dev; DevOps owns the environment files and the values themselves — coordinate on the variable names

---

**OPS-04**

As a DevOps engineer, I want end-to-end local connectivity verified so that the team has confidence the environment is ready before feature work starts.

Acceptance Criteria:
- Recipient Service connects to MySQL and RabbitMQ on startup with no connection errors
- Donor Service connects to RabbitMQ on startup with no connection errors
- A manually published test message on RabbitMQ is visible in the management UI
- MinIO bucket for receipts is created and reachable

Dependencies: OPS-01, OPS-03

Notes:
- This is a verification checklist, not application code — pairs well with QA-03's test harness

---

**OPS-05**

As a DevOps engineer, I want a CI pipeline that builds and tests all three application components so that scaffolding regressions are caught automatically instead of found manually.

Acceptance Criteria:
- Pipeline triggers on push/PR and runs `mvn test` for both Spring Boot services and `ng test` for the UI
- Pipeline fails the build on any test or compile failure
- Build status is visible on pull requests

Dependencies: DEV-02, DEV-03, DEV-04

Notes:
- Keep this to build+test only for Sprint 1 — deployment automation is OPS-06/OPS-07, not this story

---

**OPS-06**

As a DevOps engineer, I want container build and registry push scripts so that images can move from a developer's machine to a shared registry consistently.

Acceptance Criteria:
- A single script (or CI job) builds and tags all three images from OPS-02 and pushes them to the team's chosen registry
- Image tags include the git commit SHA for traceability
- Documented in `/infra/README.md` so any team member can run it manually if needed

Dependencies: OPS-02, OPS-05

Notes:
- Feeds directly into OPS-07 — the Kubernetes manifests reference these image tags

---

**OPS-07**

As a DevOps engineer, I want baseline Kubernetes/Knative manifests scaffolded so that the same containers can be validated in a cloud environment.

Acceptance Criteria:
- Deployment/Service manifests exist for Recipient and Donor, using the `cloud` profile from OPS-03 and images from OPS-06
- Knative Service manifest exists for at least one component, demonstrating scale-to-zero
- Manifests target Kubernetes 1.36.3 / Knative 1.23 per the locked stack
- Successful deploy to a local dev cluster (kind/minikube) — production cluster deployment is out of scope for this sprint

Dependencies: OPS-03, OPS-06, DEV-11

Notes:
- This is the story that proves the "containerize once, run anywhere" claim from the concept doc

---

## Core Architecture Implementation (Dev)

**DEV-05**

As a developer, I want the `PortRequest` domain entity and repository interface defined in the Recipient Service so that the application layer has a stable contract to build on.

Acceptance Criteria:
- `PortRequest` entity includes an ID, status (`INITIATED`/`COMPLETED`), and timestamps
- Repository interface defined in the domain layer with zero framework imports (per the architecture doc's layering rule)
- Unit tests confirm entity construction and status transitions

Dependencies: DEV-03

Notes:
- Framework-free domain layer is a hard rule from the architecture doc — keep JPA annotations out of this class, or confirm the team's tolerance for annotated entities before proceeding

---

**DEV-06**

As a developer, I want a lightweight `PortRequest` domain representation in the Donor Service so that it can process events without needing its own database.

Acceptance Criteria:
- Donor Service has a domain object for the incoming event payload only (no persistence)
- No repository interface exists in the Donor Service (stateless, per architecture doc)

Dependencies: DEV-04

Notes:
- Keep this intentionally thin — Donor's whole job is "receive, auto-accept, publish"

---

**DEV-07**

As a developer, I want a RabbitMQ producer adapter in the Recipient Service so that submitting a port request publishes an event.

Acceptance Criteria:
- Publishing to the `port.request.initiated` topic works against the local RabbitMQ from OPS-01
- Message payload includes the port request ID and a correlation ID
- Integration test confirms message delivery using the management UI or a test consumer

Dependencies: OPS-03, DEV-05

Notes:
- Confirm AMQP client library version compatibility against RabbitMQ 4.3.4 per the tech stack doc's compatibility matrix

---

**DEV-08**

As a developer, I want a RabbitMQ consumer and producer in the Donor Service so that it auto-accepts incoming port requests and confirms them.

Acceptance Criteria:
- Donor consumes from `port.request.initiated`, with no manual approval step (REQ-FR-DON-2)
- On successful consumption, Donor immediately publishes to `port.request.accepted`
- Failed consumption is logged, not retried (per the architecture doc's error-handling concern)

Dependencies: OPS-03, DEV-06, DEV-07

Notes:
- Keep this a single, obvious code path with no conditional review branches — manual review is explicitly out of scope

---

**DEV-09**

As a developer, I want a MySQL persistence adapter in the Recipient Service so that port request state survives restarts.

Acceptance Criteria:
- JPA repository implementation backs the domain repository interface from DEV-05
- Schema created via a migration tool (e.g., Flyway) rather than `ddl-auto`, so schema changes are trackable
- Insert and status-update operations verified against the local MySQL 8.4 instance

Dependencies: OPS-03, DEV-05

Notes:
- MySQL 8.4.10 per the locked stack — confirm the JDBC driver version supports it

---

**DEV-10**

As a developer, I want a MinIO client adapter in the Recipient Service so that confirmation receipts can be stored and retrieved.

Acceptance Criteria:
- Adapter can upload an object to the local MinIO instance and retrieve it by key
- Bucket name and credentials are externalized per OPS-03's configuration approach
- Integration test confirms a round-trip upload/download against local MinIO

Dependencies: OPS-01, OPS-03

Notes:
- Uses the S3-compatible API — the AWS S3 SDK works against MinIO without modification

---

## Basic App Structure (Dev)

**DEV-11**

As a developer, I want the Recipient Service's REST API scaffolded so that the UI and n8n have endpoints to call.

Acceptance Criteria:
- `POST /api/v1/port-requests` creates a request and returns `INITIATED` status (calls DEV-07's producer)
- `GET /api/v1/port-requests/{id}` returns current status
- `POST /api/v1/port-requests/{id}/complete` accepts the n8n callback, triggers receipt generation (DEV-10) and sets status to `COMPLETED`
- All three endpoints match the interface contracts in the architecture doc exactly

Dependencies: DEV-07, DEV-09, DEV-10

Notes:
- No authentication on any endpoint — intentional per the requirements doc's exclusions, not an oversight to "fix" this sprint

---

**DEV-12**

As a developer, I want a basic submission form and status dashboard shell in the Angular UI so that a user can trigger and observe a port request.

Acceptance Criteria:
- Submission form posts to `POST /api/v1/port-requests` and displays the returned ID
- Dashboard polls `GET /api/v1/port-requests/{id}` and reflects status changes without a manual page reload (REQ-FR-DASH-2)
- No styling polish required — functional scaffolding only

Dependencies: DEV-02, DEV-11

Notes:
- Polling interval is a placeholder (e.g., 2s) — not a tuned production value

---

**DEV-13**

As a developer, I want a base n8n workflow scaffolded so that the Donor-to-Recipient callback loop can be demonstrated end-to-end.

Acceptance Criteria:
- n8n workflow contains a RabbitMQ trigger node subscribed to `port.request.accepted`
- On trigger, workflow calls `POST /api/v1/port-requests/{id}/complete` on the Recipient Service
- Manually publishing a test message on `port.request.accepted` results in the Recipient's status updating to `COMPLETED`

Dependencies: OPS-01, DEV-08, DEV-11

Notes:
- Proves the "no-code orchestration, zero custom code" claim from the concept doc — keep the workflow to trigger + HTTP call, nothing more

---

## QA & Test Scaffolding (QA)

**QA-01**

As a QA engineer, I want a test plan mapping the concept doc's six success criteria to concrete, verifiable test cases so that the team has a shared, unambiguous definition of "done" for the demo.

Acceptance Criteria:
- Each of the six success criteria in `project-concept.md` has at least one corresponding test case
- Test cases specify expected inputs, expected outcomes, and which service/log/UI element proves the outcome
- Document reviewed and agreed with Dev and DevOps before Wave 3 (see sprint plan)

Dependencies: none

Notes:
- Can start immediately alongside DEV-01 — this doesn't require any code to exist yet, only the planning documents

---

**QA-02**

As a QA engineer, I want test cases for each Recipient Service REST endpoint so that I can validate the API independently of the UI.

Acceptance Criteria:
- Test cases cover `POST /api/v1/port-requests`, `GET /api/v1/port-requests/{id}`, and `POST /api/v1/port-requests/{id}/complete`
- Cases include at least one negative test per endpoint (e.g., requesting a non-existent ID)
- Cases are written against the interface contracts in the architecture doc, so they can be authored before DEV-11 is built and executed once it lands

Dependencies: none to author; DEV-11 to execute

Notes:
- Author early (Wave 2–3), execute once DEV-11 is available — don't block authoring on code being ready

---

**QA-03**

As a QA engineer, I want a way to publish test messages directly to RabbitMQ so that I can validate Donor auto-accept and the n8n callback independently of the full UI flow.

Acceptance Criteria:
- A documented method (script or management-UI steps) publishes a well-formed `port.request.initiated` message
- Test confirms the Donor auto-accepts within an expected time window and publishes `port.request.accepted`
- Test confirms n8n picks up the confirmation and calls the Recipient's callback endpoint

Dependencies: OPS-01, OPS-04, DEV-08

Notes:
- This isolates messaging/orchestration bugs from UI bugs — valuable given the concept doc's core goal is proving the messaging pattern itself

---

**QA-04**

As a QA engineer, I want an end-to-end smoke test that submits a port request through the UI and verifies it reaches `COMPLETED` status with a retrievable receipt, so that regressions across all three services are caught before each demo.

Acceptance Criteria:
- Test submits a request via the Angular UI (or its API directly), and polls until `COMPLETED` or a timeout
- Test confirms a receipt object exists in MinIO for the completed request
- Test result is mapped back to the relevant success criteria from QA-01's test plan
- Test is repeatable and produces a clear pass/fail result

Dependencies: DEV-12, DEV-13, OPS-04

Notes:
- This is the sprint's final acceptance gate — run it before declaring Sprint 1 done
