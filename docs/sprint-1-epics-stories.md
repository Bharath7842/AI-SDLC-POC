# Sprint 1: Epics & User Stories (Dev / QA / DevOps)

This document defines Sprint 1 as a hierarchy of Epics and User Stories, structured for direct Jira backlog creation. Each story includes clear acceptance criteria, dependencies, and wave assignment.

---

# DEVELOPER EPICS & STORIES

## EPIC: DEV-E1 - Project Infrastructure & Scaffolding

**Epic Goal:** Establish the foundational monorepo structure and project scaffolds so all three application components (UI, Recipient Service, Donor Service) have a consistent, ready-to-code baseline.

**Epic Success Criteria:**
- Monorepo structure exists with /ui, /recipient-service, /donor-service, /infra directories
- All four component projects scaffold successfully with correct framework versions
- Local development environment is reproducible from version control
- CI pipeline compiles and runs unit tests on all components

---

### DEV-S1: Initialize Monorepo Structure

**Epic:** DEV-E1  
**Wave:** 1  
**Story Type:** Story  
**Story Points:** 3

**User Story:**
As a developer, I want to initialize the monorepo directory structure so that all four components (UI, Recipient, Donor, infra) have a consistent, predictable home.

**Acceptance Criteria:**
- [ ] Repository root contains `/ui`, `/recipient-service`, `/donor-service`, `/infra` directories
- [ ] Root `README.md` documents directory layout and links to four planning documents (vision, requirements, tech stack, architecture)
- [ ] `.gitignore` includes patterns for Node.js (`node_modules/`, `dist/`), Maven (`target/`, `*.class`), Docker (`.dockerignore`), and IDE artifacts (`.vscode/`, `.idea/`)
- [ ] `.gitattributes` configured for consistent line endings across platforms
- [ ] Root `.editorconfig` specifies indentation (2 spaces for JSON/YAML, 4 for Java)

**Dependencies:** None  
**Blocks:** DEV-S2, DEV-S3, DEV-S4, QA-S1, OPS-S1

**Notes:**
- This is the sprint's blocker story — all other Dev/QA/DevOps work depends on it
- QA-S1 (test plan) can start in parallel; it needs no code

---

### DEV-S2: Scaffold Angular UI Project

**Epic:** DEV-E1  
**Wave:** 2  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a developer, I want to scaffold the Angular UI project so that frontend work has a running baseline.

**Acceptance Criteria:**
- [ ] Angular 22.1.0 project initialized via `ng new` in `/ui` directory
- [ ] Node.js version verified as ^22.12.0 (output of `node -v` matches constraint)
- [ ] TypeScript version verified as ^6.0.0 (output of `tsc -v` matches constraint)
- [ ] Project builds without errors: `ng build` produces output in `/ui/dist`
- [ ] Development server runs: `ng serve` serves the default Angular welcome page at `localhost:4200`
- [ ] `package.json` locked to Angular 22.1.0, rxjs, and Angular Material (if needed)
- [ ] Linting configured: `ng lint` runs ESLint on all TypeScript files

**Dependencies:** DEV-S1  
**Blocks:** DEV-S8 (UI integration), OPS-S7 (Docker image for Angular)

**Notes:**
- Use standalone components (Angular 22 default) — no NgModules
- Configure strict mode in `tsconfig.json`: `"strict": true`

---

### DEV-S3: Scaffold Recipient Service Project

**Epic:** DEV-E1  
**Wave:** 2  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a developer, I want to scaffold the Recipient Service Spring Boot project so that backend work has a running baseline.

**Acceptance Criteria:**
- [ ] Maven 3.9+ project created in `/recipient-service` using Spring Boot 4.1.0 and Java 25
- [ ] `pom.xml` includes Spring Boot Starter Web, Actuator, and JPA dependencies (locked to versions in tech stack)
- [ ] `spring-boot-starter-actuator` dependency added for `/actuator/health` endpoint
- [ ] Project builds cleanly: `mvn clean install` completes with BUILD SUCCESS
- [ ] Application starts: `mvn spring-boot:run` runs without errors
- [ ] Health endpoint responds: `curl http://localhost:8080/actuator/health` returns `UP` status
- [ ] Configuration supports `local` and `cloud` profiles via `application-local.yml` and `application-cloud.yml`

**Dependencies:** DEV-S1  
**Blocks:** DEV-S6, DEV-S7, DEV-S9, OPS-S3, OPS-S5

**Notes:**
- Java 25 LTS is the locked version; verify `java -version` reports 25.0.3 or later
- Actuator endpoints will be used by DevOps for container health probes

---

### DEV-S4: Scaffold Donor Service Project

**Epic:** DEV-E1  
**Wave:** 2  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a developer, I want to scaffold the Donor Service Spring Boot project so that it can independently consume events from the Recipient.

**Acceptance Criteria:**
- [ ] Maven 3.9+ project created in `/donor-service` using Spring Boot 4.1.0 and Java 25
- [ ] `pom.xml` derived from Recipient's template with `artifactId` changed to `donor-service`
- [ ] **Explicitly removed** dependencies: `spring-boot-starter-data-jpa`, `mysql-connector-j` (Donor is stateless)
- [ ] `spring-boot-starter-actuator` included for `/actuator/health` endpoint
- [ ] `spring-boot-starter-amqp` and RabbitMQ client dependencies included (locked versions per tech stack)
- [ ] Project builds cleanly: `mvn clean install` completes with BUILD SUCCESS
- [ ] Application starts independently: `mvn spring-boot:run` runs without database connection errors
- [ ] Health endpoint responds: `curl http://localhost:8081/actuator/health` returns `UP` status
- [ ] Configuration supports `local` and `cloud` profiles

**Dependencies:** DEV-S1  
**Blocks:** DEV-S7, DEV-S10, OPS-S3, OPS-S5

**Notes:**
- Donor service deliberately minimal — design for "receive, auto-accept, publish" pattern only
- Port configured to 8081 to avoid collision with Recipient (8080)
- No @Repository or @Service layer — stateless, event-driven only

---

## EPIC: DEV-E2 - Domain Layer & Integration Adapters

**Epic Goal:** Implement the domain layer (entities, repositories) and infrastructure adapters (RabbitMQ, MySQL, MinIO) so that business logic can be tested in isolation from frameworks.

**Epic Success Criteria:**
- Domain entities can be instantiated and persisted without framework coupling
- Messaging between Recipient and Donor flows through RabbitMQ without manual intervention
- Port request state survives service restarts via MySQL persistence
- Confirmation receipts are stored and retrievable from MinIO

---

### DEV-S5: Implement PortRequest Domain Entity & Repository Interface (Recipient)

**Epic:** DEV-E2  
**Wave:** 3  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a developer, I want to implement the `PortRequest` domain entity and repository interface in the Recipient Service so that the application layer has a stable, framework-free contract to build on.

**Acceptance Criteria:**
- [ ] Domain class `PortRequest` created in `recipient-service/src/main/java/domain/model/PortRequest.java` with **zero framework imports** (no JPA, Spring, Lombok)
- [ ] Fields include: `id` (UUID), `status` (enum: INITIATED, COMPLETED), `createdAt` (timestamp), `completedAt` (nullable timestamp), `customerId` (String)
- [ ] Status transitions implemented: `INITIATED` → `COMPLETED` only, with guard checks
- [ ] Repository interface defined in `domain/ports/PortRequestRepository.java` with methods: `save(PortRequest)`, `findById(UUID)`, `findAll()` — **no implementation, only interface**
- [ ] Domain entity is fully testable via unit tests: `PortRequestTest` covers construction, status transitions, and validation
- [ ] Immutable by design (builder pattern or records) to prevent accidental state mutation

**Dependencies:** DEV-S3  
**Blocks:** DEV-S7, DEV-S9, DEV-S11

**Notes:**
- Framework-free domain layer is a hard architectural rule from the architecture doc — no JPA annotations in this class
- Status enum must be an atomic type (no string magic)

---

### DEV-S6: Implement Lightweight PortRequest Domain Model (Donor)

**Epic:** DEV-E2  
**Wave:** 3  
**Story Type:** Story  
**Story Points:** 3

**User Story:**
As a developer, I want to implement a lightweight `PortRequest` domain representation in the Donor Service so that it can process events without needing its own database.

**Acceptance Criteria:**
- [ ] Domain class `PortRequest` created in `donor-service/src/main/java/domain/model/PortRequest.java` with **zero framework imports**
- [ ] Fields include: `id` (UUID), `status` (enum: INITIATED), `createdAt` (timestamp) — **event payload fields only, no persistence fields**
- [ ] No repository interface or persistence logic in Donor domain layer
- [ ] Class is deserialization-ready (can be constructed from JSON event payload)
- [ ] Immutable by design
- [ ] Unit tests confirm construction from JSON-like input

**Dependencies:** DEV-S4  
**Blocks:** DEV-S10

**Notes:**
- Keep this intentionally thin — Donor's whole job is "receive, auto-accept, publish"
- No status transitions in Donor domain — only Recipient manages state progression

---

### DEV-S7: Implement RabbitMQ Producer Adapter (Recipient)

**Epic:** DEV-E2  
**Wave:** 4  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a developer, I want to implement a RabbitMQ producer adapter in the Recipient Service so that submitting a port request publishes an event to the Donor.

**Acceptance Criteria:**
- [ ] Producer class `PortRequestEventPublisher` created in `infrastructure/messaging/RabbitMQPublisher.java`
- [ ] Publishes to exchange `port-exchange` with routing key `port.request.initiated`
- [ ] Message payload includes: `requestId` (UUID), `customerId` (String), `createdAt` (ISO 8601), `correlationId` (UUID for tracing)
- [ ] Correlation ID propagated throughout the flow (same ID appears in logs for end-to-end tracing)
- [ ] RabbitMQ connection uses configuration from `local` profile (hostname: `rabbitmq`) and `cloud` profile (Kubernetes DNS: `rabbitmq.default.svc.cluster.local`)
- [ ] Integration test confirms message delivery against local RabbitMQ (Docker Compose)
- [ ] Failed publishes are logged with full error context, not retried (per architecture doc)

**Dependencies:** OPS-S3 (Docker Compose running), DEV-S5 (domain entity exists)  
**Blocks:** DEV-S8, DEV-S10, QA-S3

**Notes:**
- Use Spring AMQP `RabbitTemplate` for message publishing
- Message serialization to JSON; deserializer configured on Donor side
- Confirm AMQP client version compatibility with RabbitMQ 4.3.4 per tech stack

---

### DEV-S8: Implement RabbitMQ Consumer & Producer Adapters (Donor)

**Epic:** DEV-E2  
**Wave:** 5  
**Story Type:** Story  
**Story Points:** 6

**User Story:**
As a developer, I want to implement RabbitMQ consumer and producer adapters in the Donor Service so that it auto-accepts incoming port requests and confirms them.

**Acceptance Criteria:**
- [ ] Consumer class `PortRequestEventListener` created, subscribes to `port.request.initiated` queue
- [ ] On successful message consumption, Donor **immediately** publishes to `port.request.accepted` (no manual review step)
- [ ] Accepted event payload includes: `requestId` (UUID), `acceptedAt` (ISO 8601), `correlationId` (same as incoming)
- [ ] Failed consumption is logged with full context (message, error, timestamp) but **not retried** (per requirements)
- [ ] Configuration uses `local` profile (Docker Compose) and `cloud` profile (Kubernetes DNS)
- [ ] Integration test confirms full flow: message published → consumed → confirmation published (against local RabbitMQ)
- [ ] Concurrency control: single consumer thread processes one message at a time (prefetch=1)

**Dependencies:** OPS-S3 (Docker Compose), DEV-S6 (domain model), DEV-S7 (message contract established)  
**Blocks:** DEV-S11, DEV-S12, OPS-S4, QA-S3

**Notes:**
- Keep this a single, obvious code path — no conditional branches or approval queues
- Consumer error handler must log and move on (no dead-letter queue for POC)
- Correlation ID tracking essential for understanding the full flow in logs

---

### DEV-S9: Implement MySQL Persistence Adapter (Recipient)

**Epic:** DEV-E2  
**Wave:** 4  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a developer, I want to implement a MySQL persistence adapter in the Recipient Service so that port request state survives application restarts.

**Acceptance Criteria:**
- [ ] JPA entity `PortRequestJPAEntity` created in `infrastructure/persistence/jpa/`; maps domain `PortRequest` to database schema
- [ ] Database schema: `port_requests` table with columns: `id` (UUID, PK), `status` (enum stored as VARCHAR), `created_at` (TIMESTAMP), `completed_at` (TIMESTAMP nullable), `customer_id` (VARCHAR)
- [ ] Schema migrations managed via Flyway (not Hibernate `ddl-auto`) in `src/main/resources/db/migration/V1__init.sql`
- [ ] Repository implementation `JpaPortRequestRepository` extends Spring Data JPA `CrudRepository`, implements domain `PortRequestRepository` interface
- [ ] Adapter maps domain `PortRequest` ↔ JPA entity (domain layer never touches JPA annotations)
- [ ] Insert and status-update operations verified against MySQL 8.4.10 in Docker Compose
- [ ] Unit tests confirm entity construction; integration tests confirm CRUD against real database

**Dependencies:** OPS-S3 (MySQL running), DEV-S5 (domain interface defined)  
**Blocks:** DEV-S11

**Notes:**
- MySQL 8.4.10 LTS per tech stack; verify JDBC driver version supports it
- Flyway migration ensures schema changes are tracked in version control
- No Hibernate `ddl-auto` — migrations are explicit and reviewable

---

### DEV-S10: Implement MinIO Client Adapter (Recipient)

**Epic:** DEV-E2  
**Wave:** 4  
**Story Type:** Story  
**Story Points:** 4

**User Story:**
As a developer, I want to implement a MinIO client adapter in the Recipient Service so that confirmation receipts can be stored and retrieved.

**Acceptance Criteria:**
- [ ] MinIO client class `ReceiptStorageService` created in `infrastructure/storage/minio/`
- [ ] Uses AWS S3 SDK (MinIO-compatible) for `PutObject`, `GetObject` operations
- [ ] Bucket name and credentials externalized to environment variables (per OPS-S4 config)
- [ ] Upload method: receives receipt content (as InputStream), generates unique key (`{requestId}-receipt.txt`), returns storage URI
- [ ] Download method: receives storage key, returns InputStream for client to stream
- [ ] Configuration supports `local` profile (MinIO endpoint: `localhost:9000`) and `cloud` profile (MinIO service DNS)
- [ ] Integration test confirms round-trip: upload object → retrieve → verify content against local MinIO
- [ ] Client initialization handles connection errors gracefully (logged, not thrown during app startup)

**Dependencies:** OPS-S1 (MinIO running), OPS-S4 (environment configuration)  
**Blocks:** DEV-S11

**Notes:**
- MinIO uses S3-compatible API — AWS SDK works unmodified
- Bucket creation automated on first write if bucket doesn't exist
- Error handling: failed uploads logged; failed downloads propagated as ApplicationException for controller to handle

---

## EPIC: DEV-E3 - REST API & Application Layer

**Epic Goal:** Build the REST API endpoints and application service layer so the UI and n8n orchestration layer have a contract to work against.

**Epic Success Criteria:**
- Three REST endpoints exposed: POST (submit), GET (status), POST (complete)
- All endpoints match the interface contracts in the architecture doc exactly
- Application service layer orchestrates domain, messaging, and persistence adapters
- No authentication applied (intentional per requirements)

---

### DEV-S11: Implement Recipient Service REST API

**Epic:** DEV-E3  
**Wave:** 6  
**Story Type:** Story  
**Story Points:** 6

**User Story:**
As a developer, I want to implement the Recipient Service REST API so that the UI and n8n have endpoints to call for submitting, tracking, and completing port requests.

**Acceptance Criteria:**
- [ ] **POST /api/v1/port-requests** — Submits a new port request
  - Request body: `{ "customerId": "string" }`
  - Response (201): `{ "id": "uuid", "status": "INITIATED", "createdAt": "ISO8601" }`
  - Calls DEV-S7 producer to publish `port.request.initiated` event
  - Persists via DEV-S9 adapter
  - Correlation ID generated and logged for end-to-end tracing
  
- [ ] **GET /api/v1/port-requests/{id}** — Retrieves current status
  - Response (200): `{ "id": "uuid", "status": "INITIATED|COMPLETED", "createdAt": "ISO8601", "completedAt": "ISO8601|null" }`
  - Returns 404 if request ID not found
  - No authentication required
  
- [ ] **POST /api/v1/port-requests/{id}/complete** — Called by n8n to finalize request
  - Request body: `{ "receiptUri": "string" }` (where receipt was stored)
  - Response (200): `{ "id": "uuid", "status": "COMPLETED", "completedAt": "ISO8601" }`
  - Persists via DEV-S9 adapter
  - Returns 404 if request ID not found; 400 if status already COMPLETED
  
- [ ] Error responses consistent: `{ "error": "string", "timestamp": "ISO8601", "path": "string" }`
- [ ] Correlation ID extracted from request headers (if present) or generated; included in all log entries
- [ ] All endpoints return application/json; no custom content negotiation
- [ ] No @RequestBody or @PathVariable validation annotations — validation happens in service layer
- [ ] Integration test covers all three endpoints against in-memory database

**Dependencies:** DEV-S5, DEV-S7, DEV-S9, DEV-S10  
**Blocks:** DEV-S12, OPS-S7, QA-S2

**Notes:**
- No authentication on any endpoint — intentional per requirements, not an oversight
- ControllerAdvice for global error handling
- Correlation ID must appear in every log entry for that request

---

### DEV-S12: Implement Angular Submission Form & Status Dashboard

**Epic:** DEV-E3  
**Wave:** 6  
**Story Type:** Story  
**Story Points:** 6

**User Story:**
As a developer, I want to implement the Angular UI with a submission form and status dashboard so that users can trigger and observe a port request.

**Acceptance Criteria:**
- [ ] **Submission Form Component** — Collects customer ID, submits to POST /api/v1/port-requests
  - Form fields: Customer ID (text input, required)
  - Submit button triggers POST call; displays returned request ID
  - Error handling: displays error message if POST fails (backend error or network)
  - On success, form clears and request ID shown in confirmation panel
  
- [ ] **Status Dashboard Component** — Polls GET /api/v1/port-requests/{id} every 2 seconds
  - Displays current status (INITIATED or COMPLETED)
  - Auto-refreshes on interval without page reload
  - Stops polling once status reaches COMPLETED
  - Shows last-updated timestamp
  - Displays error message if request ID not found
  
- [ ] **UI Layout** — Minimal, functional (no CSS polish required for POC)
  - Form section on left or top
  - Dashboard section on right or bottom
  - Status badge shows current state with visual indicator (color or icon)
  
- [ ] **Error Handling** — Displays user-facing error messages from API errors
  - Network errors: "Unable to connect to service"
  - 404 errors: "Request not found"
  - 500 errors: "Server error occurred"
  
- [ ] **Component Lifecycle** — Standalone Angular components (no NgModule)
  - Uses Angular HttpClient for API calls
  - Uses RxJS intervals for polling; properly unsubscribes on component destroy
  - No external UI libraries (Bootstrap, Material) required for POC

**Dependencies:** DEV-S2 (Angular scaffold), DEV-S11 (REST API exists)  
**Blocks:** QA-S4, OPS-S7

**Notes:**
- Polling interval (2s) is a placeholder, not production tuned
- Push-based updates (WebSocket/SSE) listed as future enhancement, not this sprint
- Form validation minimal — server-side validation is source of truth

---

### DEV-S13: Implement Base n8n Workflow for Donor Callback

**Epic:** DEV-E3  
**Wave:** 6  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a developer, I want to scaffold a base n8n workflow so that the Donor-to-Recipient callback loop can be demonstrated end-to-end without custom code.

**Acceptance Criteria:**
- [ ] n8n workflow deployed and accessible at `localhost:5678` (local) or via cloud DNS (cloud)
- [ ] **RabbitMQ Trigger Node** — Subscribes to `port.request.accepted` queue
  - Extracts message payload: `{ "requestId": "uuid", "acceptedAt": "ISO8601", "correlationId": "uuid" }`
  - Emits to HTTP Request node on each message
  
- [ ] **HTTP Request Node** — Calls Recipient Service callback
  - Method: POST
  - URL: `http://recipient-service:8080/api/v1/port-requests/{requestId}/complete` (templated with message data)
  - Body: `{ "receiptUri": "http://minio:9000/receipts/{requestId}-receipt.txt" }`
  - Includes correlation ID in header: `X-Correlation-ID: {correlationId}`
  
- [ ] **Error Handling Node** — Logs failed callbacks
  - On HTTP error, logs request details and error response
  - Does not retry (per architecture doc)
  - Emits to a debug output for visibility
  
- [ ] **Testing Flow** — Manually published test message to `port.request.accepted` results in:
  - n8n receives message
  - HTTP call executed to Recipient endpoint
  - Recipient's GET status endpoint shows COMPLETED
  
- [ ] **Deployment** — Workflow exported as `.json` and version-controlled in `/infra/n8n/workflows/`
- [ ] Configuration — n8n credentials (RabbitMQ, HTTP endpoints) managed via environment variables

**Dependencies:** OPS-S1 (n8n and RabbitMQ running), DEV-S8 (Donor publishing events), DEV-S11 (Recipient callback endpoint exists)  
**Blocks:** QA-S4, OPS-S7

**Notes:**
- Proves the "no-code orchestration" claim — keep workflow to trigger + HTTP call, nothing more
- Workflow logic must be visually understandable (no complex JavaScript, minimal conditional branches)
- Error handling simple: log and move on (no retry queue for POC)

---

# QA EPICS & STORIES

## EPIC: QA-E1 - Test Planning & Automation Framework

**Epic Goal:** Establish a comprehensive automated testing framework using Robot Framework to cover all aspects of the POC (API, messaging, UI), with clear test cases, reporting, and CI/CD integration.

**Epic Success Criteria:**
- Automated test suite covers all six success criteria from the POC concept
- Robot Framework test cases are executable and reproducible in CI/CD pipeline
- Test reports generated with pass/fail status, timing, and failure details
- Regression test suite runs on every build
- Tests integrated into CI/CD pipeline (run before deployment)

---

### QA-S1: Create Test Plan Mapped to Success Criteria

**Epic:** QA-E1  
**Wave:** 1  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a QA engineer, I want a comprehensive test plan that maps the concept doc's six success criteria to concrete, verifiable test cases so that the team has a shared, unambiguous definition of "done" for the POC demo.

**Acceptance Criteria:**
- [ ] Test plan document created: `/qa/test-plan.md`
- [ ] Each of the six success criteria has at least one corresponding test case ID and description
- [ ] Test cases specify:
  - **Expected Input** (data, sequence of actions, preconditions)
  - **Expected Output** (response format, state change, visible result)
  - **Verification Method** (which service/log/UI element proves the outcome)
  - **Success Criteria** (measurable: response code, status field value, log entry present, etc.)
  
- [ ] Six mappings:
  1. Port request submitted in Angular UI → request persisted with status INITIATED
  2. Request event published to RabbitMQ → Donor consumes it
  3. Donor auto-accepts → confirmation published to RabbitMQ
  4. n8n routes Donor confirmation to Recipient → Recipient callback endpoint called
  5. Dashboard reflects status change to COMPLETED without page reload
  6. Confirmation receipt retrievable from MinIO
  
- [ ] Test plan reviewed and agreed with Dev and DevOps leads before Wave 3
- [ ] All test cases documented with traceability to architecture doc components

**Dependencies:** None (can start immediately)  
**Blocks:** QA-S2, QA-S3, QA-S4

**Notes:**
- This is pure planning — no code or infrastructure required yet
- Document serves as acceptance gate for entire POC

---

### QA-S2: Author Automated REST API Test Cases (Robot Framework)

**Epic:** QA-E1  
**Wave:** 2  
**Story Type:** Story  
**Story Points:** 6

**User Story:**
As a QA engineer, I want to author automated test cases for each Recipient Service REST endpoint using Robot Framework so that the API can be validated independently of the UI and messaging layers.

**Acceptance Criteria:**
- [ ] Test suite created: `/qa/tests/api/recipient_service_api.robot`
- [ ] Robot Framework test cases written against interface contracts from architecture doc
- [ ] **Test Case Suite Covers:**
  - **POST /api/v1/port-requests** — Success case
    - Input: valid customerId
    - Expected output: 201 response, returns requestId and INITIATED status
    - Verification: response JSON contains id, status, createdAt
  
  - **POST /api/v1/port-requests** — Negative case: invalid input
    - Input: empty or malformed customerId
    - Expected output: 400 Bad Request
    - Verification: error response includes error message
  
  - **GET /api/v1/port-requests/{id}** — Success case: request exists
    - Input: valid requestId from previous test
    - Expected output: 200 response with current status
    - Verification: response includes id, status, timestamps
  
  - **GET /api/v1/port-requests/{id}** — Negative case: request not found
    - Input: non-existent requestId (random UUID)
    - Expected output: 404 Not Found
    - Verification: error response message
  
  - **POST /api/v1/port-requests/{id}/complete** — Success case
    - Input: valid requestId, receiptUri
    - Expected output: 200 response with status COMPLETED
    - Verification: response shows status=COMPLETED, completedAt is set
  
  - **POST /api/v1/port-requests/{id}/complete** — Negative case: already completed
    - Input: requestId already marked COMPLETED
    - Expected output: 400 Bad Request
    - Verification: error message indicates status conflict

- [ ] Test variables externalized:
  - BASE_URL (http://recipient-service:8080 local, cloud DNS cloud)
  - TIMEOUT (5s for API calls)
  - Custom keywords for common operations (create request, get status, etc.)

- [ ] Test execution:
  - Tests executable locally against running service: `robot /qa/tests/api/recipient_service_api.robot`
  - Tests generate HTML report with pass/fail details
  - Can be authored and reviewed before DEV-S11 is implemented (tests written against contract)

**Dependencies:** QA-S1 (test plan exists); DEV-S11 to execute  
**Blocks:** QA-S5

**Notes:**
- Tests authored early (Wave 2) against contracts; executed once API exists (Wave 6)
- Robot Framework syntax is readable and doesn't require programming background
- Keywords kept DRY (Don't Repeat Yourself) — `Create Port Request` keyword used in multiple test cases

---

### QA-S3: Create RabbitMQ Test Harness for Messaging Validation

**Epic:** QA-E1  
**Wave:** 5  
**Story Type:** Story  
**Story Points:** 6

**User Story:**
As a QA engineer, I want a test harness that can publish and consume RabbitMQ messages directly so that messaging and orchestration layers can be validated independently of the full UI flow.

**Acceptance Criteria:**
- [ ] Test harness script created: `/qa/tests/messaging/rabbitmq_tests.robot`
- [ ] **Test Cases Cover:**
  - **Donor Auto-Accept Flow**
    - Publish `port.request.initiated` message to RabbitMQ (simulating Recipient)
    - Verify Donor consumes within 5 seconds (check RabbitMQ management UI or log tailing)
    - Verify Donor publishes `port.request.accepted` to RabbitMQ within 5 seconds
    - Verify message contains requestId, acceptedAt, correlationId
  
  - **n8n Workflow Trigger**
    - Publish `port.request.accepted` message (simulating Donor)
    - Verify n8n workflow is triggered within 5 seconds (check n8n UI execution log)
    - Verify HTTP request node called with correct URL and body
    - Verify Recipient Service received callback (check service logs for POST call and correlation ID)
  
  - **Message Correlation**
    - Publish `port.request.initiated` with correlationId = "test-corr-123"
    - Verify same correlationId appears in `port.request.accepted` message
    - Verify correlationId propagates through n8n logs and HTTP call headers

- [ ] Test execution helpers:
  - Custom keyword `Publish Message To RabbitMQ` (uses RabbitMQ AMQP library)
  - Custom keyword `Consume Message From Queue` with timeout
  - Custom keyword `Get Service Log Entries Since` (tails Docker logs or application logs)

- [ ] Preconditions:
  - Docker Compose environment running (OPS-S1)
  - Services (Recipient, Donor, n8n) started and healthy

**Dependencies:** OPS-S1 (infrastructure running), DEV-S8 (Donor messaging), DEV-S13 (n8n workflow)  
**Blocks:** QA-S4

**Notes:**
- Isolates messaging/orchestration bugs from UI bugs — critical for proving the POC's core claim
- Uses RabbitMQ Python library (or Robot Framework AMQP library) to publish/consume directly
- Service logs tailed via Docker: `docker logs <container> --since <time>`

---

### QA-S4: Implement End-to-End Smoke Test Suite (Robot Framework)

**Epic:** QA-E1  
**Wave:** 7  
**Story Type:** Story  
**Story Points:** 7

**User Story:**
As a QA engineer, I want an end-to-end smoke test that executes the full POC flow from UI submission through completion and verifies all six success criteria are met.

**Acceptance Criteria:**
- [ ] End-to-end test suite created: `/qa/tests/e2e/port_request_e2e.robot`
- [ ] **Test Case: Full Flow Submission to Completion**
  - **Setup:** Clear previous data (delete test requests from MySQL, clear test receipts from MinIO)
  - **Step 1:** Submit port request via Angular UI: enter customerId → click Submit
  - **Step 2:** Verify request ID displayed and status shown as INITIATED
  - **Step 3:** Poll status dashboard every 2 seconds, wait up to 30 seconds for status = COMPLETED
  - **Step 4:** Verify status dashboard updates to COMPLETED without manual page reload
  - **Step 5:** Retrieve confirmation receipt from MinIO via API or direct MinIO client
  - **Step 6:** Verify receipt exists and contains request metadata
  - **Verification:** All six success criteria mapped to test steps and marked passed/failed

- [ ] **Test Case: Failure Path — Invalid Customer ID**
  - Submit request with empty customerId
  - Verify error message displayed in UI
  - Verify request NOT created in database (query MySQL for non-existent request)

- [ ] **Regression Test Suite**
  - Executes all QA-S2 API test cases
  - Executes all QA-S3 messaging test cases
  - Executes full E2E flow
  - Reports overall pass/fail and individual test timings

- [ ] **Test Execution & Reporting:**
  - Command: `robot /qa/tests/e2e/port_request_e2e.robot`
  - HTML report generated with screenshots on failure (Angular UI screenshots, browser console errors)
  - Log entries tagged with test ID and timestamp
  - Report uploaded to CI artifact storage

- [ ] **Repeatability:**
  - Test can be run multiple times in sequence without manual cleanup
  - Each run uses unique customerId to avoid data conflicts
  - MinIO cleanup between runs (previous test receipts removed)

**Dependencies:** DEV-S12 (Angular UI), DEV-S13 (n8n workflow), QA-S2 (API tests), QA-S3 (messaging tests)  
**Blocks:** None (final acceptance test)

**Notes:**
- This is the sprint's final gate — run before declaring Sprint 1 done
- Timing tolerances (5s for messaging, 30s for E2E) are POC defaults, not production values
- Screenshots on failure help diagnose UI issues

---

### QA-S5: Integrate Automated Tests into CI/CD Pipeline

**Epic:** QA-E1  
**Wave:** 7  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a QA engineer, I want automated tests integrated into the CI/CD pipeline so that regressions are caught automatically on every build.

**Acceptance Criteria:**
- [ ] CI Pipeline Configuration: `.github/workflows/test.yml` (or equivalent for chosen CI tool)
- [ ] **Pipeline Stages:**
  - **Unit & Lint** — Dev commits trigger: `mvn test` (Recipient/Donor), `ng test` (Angular)
  - **API Tests** — Run QA-S2 test suite: `robot /qa/tests/api/recipient_service_api.robot` (requires Recipient running in Docker)
  - **Messaging Tests** — Run QA-S3 test suite: `robot /qa/tests/messaging/rabbitmq_tests.robot` (requires full stack running in Docker Compose)
  - **E2E Tests** — Run QA-S4 suite: `robot /qa/tests/e2e/port_request_e2e.robot` (requires full stack + browser/Selenium)
  - **Report & Archive** — Upload HTML reports and log files to CI artifact storage

- [ ] **Pipeline Triggers:**
  - On every push to main/develop branches
  - On every pull request
  - Nightly scheduled run (optional)

- [ ] **Environment Setup:**
  - Docker Compose stack started as pre-test step: `docker-compose up -d` in `/infra`
  - Wait for health checks: all services responding at `/actuator/health` (services) or management UI (RabbitMQ, MinIO, n8n)
  - Environment variables injected for `local` profile (service hostnames = Docker Compose service names)

- [ ] **Failure Handling:**
  - Pipeline fails if any test fails (blocks merge)
  - Test reports and logs made available for investigation
  - Developers can re-run failed tests locally: `robot --include <tag> /qa/tests/`

- [ ] **Success Criteria:**
  - All unit tests pass
  - All API tests pass
  - All messaging tests pass
  - E2E smoke test passes
  - HTML report generated with 0 failures

**Dependencies:** OPS-S5 (CI pipeline infrastructure), QA-S2, QA-S3, QA-S4 (test suites exist)  
**Blocks:** None

**Notes:**
- CI pipeline uses Docker Compose to isolate test environment from host system
- Services tagged with Docker labels for health checks: `healthcheck: { test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"] }`
- Parallel test execution optional (Robot Framework supports it with `--processes` flag)

---

# DEVOPS EPICS & STORIES

## EPIC: OPS-E1 - Cloud-Agnostic Infrastructure & Containerization

**Epic Goal:** Build a containerized, cloud-agnostic infrastructure that deploys identically to Docker Compose (local), Kubernetes (on-prem), or managed Kubernetes (AWS EKS, Azure AKS, GCP GKE) without modification.

**Epic Success Criteria:**
- All services containerized with minimal, production-ready images
- Docker Compose stack runs locally with all dependencies (RabbitMQ, MySQL, MinIO, n8n)
- Kubernetes manifests deploy services to any CNCF-conformant cluster without re-imaging
- Configuration (hostnames, credentials) driven by environment variables — no hardcoded cloud-specific paths
- Health checks integrated into container runtimes (Docker, Kubernetes)

---

### OPS-S1: Deploy Local Docker Compose Infrastructure Stack

**Epic:** OPS-E1  
**Wave:** 2  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a DevOps engineer, I want the Docker Compose environment running locally so that RabbitMQ, MySQL, MinIO, n8n, and the application services are available for development and testing.

**Acceptance Criteria:**
- [ ] `docker-compose.yml` created in `/infra` with services:
  - **RabbitMQ 4.3.4** — Management UI on 15672, AMQP on 5672, health check enabled
  - **MySQL 8.4.10 LTS** — Port 3306, root password set, port_requests database auto-created, health check enabled
  - **MinIO (OSS)** — Version RELEASE.2025-10-15T17-29-55Z, port 9000, bucket `port-requests` auto-created, health check enabled
  - **n8n** — Port 5678, persistent storage in named volume, health check enabled

- [ ] `.env.local` file created with local overrides:
  - `SPRING_PROFILES_ACTIVE=local`
  - `RABBITMQ_HOST=rabbitmq` (Docker Compose service name)
  - `MYSQL_HOST=mysql`
  - `MINIO_HOST=minio:9000`
  - `N8N_HOST=http://n8n:5678`

- [ ] **Startup & Verification:**
  - `docker-compose up -d` starts all services without errors
  - All services marked `healthy` in `docker ps` output after 30 seconds
  - RabbitMQ management UI accessible at `http://localhost:15672` (login: guest/guest)
  - MySQL accessible: `mysql -h localhost -u root -p<password>` lists databases (port_requests exists)
  - MinIO console accessible at `http://localhost:9000` (login: minioadmin/minioadmin)
  - n8n accessible at `http://localhost:5678`

- [ ] **Named Volumes:** Data persists across `docker-compose down/up` cycles
- [ ] **Network:** Services communicate via Docker Compose network (service names resolve to IPs)
- [ ] **Logging:** All service logs accessible via `docker-compose logs <service>`

**Dependencies:** None  
**Blocks:** All other OPS stories, most DEV stories

**Notes:**
- Local environment is the developer's workbench — quick startup essential
- Health checks ensure services are ready before application starts
- `.env.local` never committed to version control (added to `.gitignore`)

---

### OPS-S2: Create Production-Ready Dockerfiles for All Services

**Epic:** OPS-E1  
**Wave:** 3  
**Story Type:** Story  
**Story Points:** 6

**User Story:**
As a DevOps engineer, I want production-ready Dockerfiles for the Recipient Service, Donor Service, and Angular UI so that all three application components can be containerized and deployed anywhere.

**Acceptance Criteria:**
- [ ] **Recipient Service Dockerfile** — `/recipient-service/Dockerfile`
  - **Base Image:** `eclipse-temurin:25-jre-jammy` (Java 25 JRE, minimal)
  - **Build Stage:** Uses Maven to compile and package JAR (`mvn clean package -DskipTests`)
  - **Runtime Stage:** Copies only JAR and runs with `java -jar` (multi-stage build)
  - **Health Check:** `CMD curl -f http://localhost:8080/actuator/health || exit 1`
  - **Port:** Exposes 8080
  - **Environment:** Accepts `SPRING_PROFILES_ACTIVE`, `RABBITMQ_HOST`, `MYSQL_HOST`, etc. as build/runtime args
  - **Image Size:** < 500MB

- [ ] **Donor Service Dockerfile** — `/donor-service/Dockerfile`
  - Same structure as Recipient
  - **Port:** Exposes 8081 (to avoid collision)
  - **Health Check:** `CMD curl -f http://localhost:8081/actuator/health || exit 1`
  - **Image Size:** < 500MB

- [ ] **Angular UI Dockerfile** — `/ui/Dockerfile`
  - **Build Stage:** `node:22-alpine` runs `npm ci && ng build --configuration production`
  - **Runtime Stage:** `nginx:latest-alpine` serves static files from `/ui/dist`
  - **Port:** Exposes 80
  - **Health Check:** `CMD curl -f http://localhost/index.html || exit 1`
  - **Image Size:** < 100MB

- [ ] **Build Verification:**
  - Each Dockerfile builds cleanly: `docker build -t <image>:<tag> .`
  - Built images run standalone: `docker run -p <port>:<port> <image>:<tag>` responds on mapped port
  - Health checks pass immediately after container start

- [ ] **Image Tagging Convention:**
  - Format: `<registry>/<image>:<version>-<commit-sha>`
  - Example: `bhrthmahesh09.azurecr.io/port-request-recipient:v1.0.0-a1b2c3d4`

**Dependencies:** DEV-S2, DEV-S3, DEV-S4 (application code scaffolded)  
**Blocks:** OPS-S6, OPS-S7

**Notes:**
- Multi-stage builds reduce image size significantly (final stage contains only runtime, not build artifacts)
- Alpine variants used where possible for minimal footprint
- Health checks ensure Kubernetes/Docker can verify service readiness

---

### OPS-S3: Implement Cloud-Agnostic Environment Configuration

**Epic:** OPS-E1  
**Wave:** 3  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a DevOps engineer, I want environment-variable-based configuration profiles so that the same containers run identically in Docker Compose (local), Kubernetes (on-prem), or managed Kubernetes (AWS/Azure/GCP) without re-imaging.

**Acceptance Criteria:**
- [ ] **Spring Boot Services** — Both Recipient and Donor support two profiles:
  - **local** profile — Docker Compose service names as hostnames
    - `application-local.yml` sets:
      - `spring.rabbitmq.host=rabbitmq` (Docker Compose service name)
      - `spring.datasource.url=jdbc:mysql://mysql:3306/port_requests` (Recipient only)
      - `minio.host=minio:9000` (Recipient only)
  
  - **cloud** profile — Kubernetes DNS and managed service endpoints
    - `application-cloud.yml` sets:
      - `spring.rabbitmq.host=rabbitmq.default.svc.cluster.local` (Kubernetes DNS)
      - `spring.datasource.url=jdbc:mysql://mysql.default.svc.cluster.local:3306/port_requests` (Recipient)
      - `minio.host=minio.default.svc.cluster.local:9000` (Recipient)
  
  - **Environment Variable:** `SPRING_PROFILES_ACTIVE=local` or `cloud` selects active profile at runtime
  - **Fallback:** If unset, defaults to `local` (safe for developers)

- [ ] **Angular UI** — Environment configuration per environment file
  - `src/environments/environment.local.ts` — API URL = `http://localhost:8080`
  - `src/environments/environment.cloud.ts` — API URL = `http://recipient-service.default.svc.cluster.local:8080`
  - Build step passes environment: `ng build --configuration=production --output-hashing=all --base-href=/`
  - Runtime env selected via `src/environments/environment.ts` (symlink or conditional import)

- [ ] **Docker Compose** — `.env.local` supplies all overrides
  - `SPRING_PROFILES_ACTIVE=local`
  - `RABBITMQ_HOST=rabbitmq`
  - `MYSQL_HOST=mysql`
  - `MINIO_HOST=minio:9000`
  - `N8N_URL=http://n8n:5678`

- [ ] **Kubernetes Secrets & ConfigMaps** — Cloud profile environment injected via:
  - `ConfigMap` for non-sensitive config (hostnames, profiles)
  - `Secret` for credentials (DB passwords, MinIO keys) — base64 encoded
  - Pod `env` sections reference ConfigMap and Secret keys

- [ ] **Verification:**
  - Local: Services connect via Docker Compose names → test with local Docker Compose
  - Cloud: Services connect via Kubernetes DNS → test with `kind` (local Kubernetes) or cloud cluster

**Dependencies:** DEV-S3, DEV-S4, OPS-S1  
**Blocks:** OPS-S4, OPS-S7

**Notes:**
- No hardcoded AWS/Azure/GCP paths — pure Kubernetes DNS (works on any CNCF cluster)
- Credentials never in image layers — injected at runtime via Secrets
- Environment files version-controlled; `.gitignore` protects local overrides

---

### OPS-S4: Verify End-to-End Local Connectivity & Health Checks

**Epic:** OPS-E1  
**Wave:** 4  
**Story Type:** Story  
**Story Points:** 4

**User Story:**
As a DevOps engineer, I want end-to-end connectivity verified so that the team has confidence the Docker Compose environment is fully functional before feature work starts.

**Acceptance Criteria:**
- [ ] **Connectivity Checklist** (can be a script or manual steps in `/infra/CONNECTIVITY_CHECK.md`):
  - [ ] Recipient Service connects to MySQL on startup with no connection errors
    - Verify: `docker logs recipient-service | grep -i "connected\|pool\|datasource"` shows successful connection
    - Verify: `curl http://localhost:8080/actuator/health` returns `UP`
  
  - [ ] Recipient Service connects to RabbitMQ on startup
    - Verify: `docker logs recipient-service | grep -i "rabbitmq\|amqp"` shows successful connection
  
  - [ ] Donor Service connects to RabbitMQ on startup
    - Verify: `curl http://localhost:8081/actuator/health` returns `UP`
  
  - [ ] MySQL data persists:
    - Query: `mysql -h localhost -u root port_requests -e "SELECT COUNT(*) FROM port_requests;"`
    - Result: Returns 0 (table exists and is empty)
  
  - [ ] MinIO bucket reachable:
    - Create test object via MinIO CLI or S3 SDK
    - Verify bucket `port-requests` exists: `mc ls minio/port-requests` (using MinIO client)
    - Verify object persists across service restart
  
  - [ ] RabbitMQ management UI accessible:
    - URL: `http://localhost:15672`
    - Login: guest/guest
    - Verify: Exchanges (port-exchange) and queues visible in UI
  
  - [ ] n8n accessible:
    - URL: `http://localhost:5678`
    - Verify: n8n UI loads (no 500 errors)
    - Verify: RabbitMQ trigger node can be configured (credentials UI accessible)

- [ ] **Automated Connectivity Test Script** — Optional but recommended
  - Shell script or Python script in `/infra/verify-connectivity.sh`
  - Runs all checks above; exits 0 if all pass, non-zero otherwise
  - Can be called from CI/CD pipeline pre-test step

**Dependencies:** OPS-S1, OPS-S3  
**Blocks:** All feature development stories

**Notes:**
- This is a verification checklist, not application code — helps catch environment issues early
- Pairs well with QA-S3's messaging test harness

---

### OPS-S5: Set Up CI/CD Pipeline for Build & Test

**Epic:** OPS-E1  
**Wave:** 3  
**Story Type:** Story  
**Story Points:** 6

**User Story:**
As a DevOps engineer, I want a CI/CD pipeline that builds and tests all three components so that scaffolding regressions are caught automatically instead of found manually.

**Acceptance Criteria:**
- [ ] **Pipeline Definition:** `.github/workflows/build.yml` (GitHub Actions) or equivalent (GitLab CI, Jenkins)
- [ ] **Trigger:** On push to main/develop, on pull request, nightly scheduled run
- [ ] **Build Stage:**
  - Recipient Service: `mvn clean install -DskipTests` (compiles and packages JAR)
  - Donor Service: `mvn clean install -DskipTests`
  - Angular UI: `npm ci && ng build --configuration production`
  - All build artifacts cached (Maven cache, npm cache) for speed

- [ ] **Test Stage:**
  - Recipient Service: `mvn test` (runs unit tests)
  - Donor Service: `mvn test`
  - Angular UI: `ng test --watch=false` (runs Karma unit tests)
  - Failure halts pipeline (no partial builds deployed)

- [ ] **Docker Build Stage:**
  - Build images for all three services using OPS-S2 Dockerfiles
  - Tag images: `<image>:<branch>-<commit-sha>`
  - Store images in CI artifact registry or push to container registry (AWS ECR, Azure ACR, Docker Hub)

- [ ] **Integration Test Stage:**
  - Spin up Docker Compose stack with built images
  - Run OPS-S4 connectivity checks
  - Run API tests (QA-S2) against containerized services
  - Tear down stack on completion

- [ ] **Reporting & Artifacts:**
  - Test reports uploaded to CI artifact storage (JUnit, Karma coverage)
  - Build logs retained for debugging
  - Failure notifications sent to Slack or email

- [ ] **Success Criteria:**
  - Build succeeds if all stages pass
  - Images pushed to registry with semantic versioning tags (v1.0.0, latest)
  - Pipeline re-runnable if infrastructure is down (eventual consistency)

**Dependencies:** OPS-S2 (Dockerfiles), OPS-S1 (Docker Compose), QA-S2 (test suite)  
**Blocks:** OPS-S6, OPS-S7

**Notes:**
- Docker Compose stack used in CI for integration testing — same stack as local development
- Caching critical for speed — CI runs should complete in < 10 minutes
- Artifact retention: 30 days for all builds, indefinite for releases

---

### OPS-S6: Implement Container Registry Push & Image Management

**Epic:** OPS-E1  
**Wave:** 5  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a DevOps engineer, I want container images built and pushed to a registry so that they can be deployed to any Kubernetes cluster (on-prem, AWS, Azure, GCP).

**Acceptance Criteria:**
- [ ] **Registry Setup** — Choose and configure container registry (Docker Hub, AWS ECR, Azure ACR, or Harbor on-prem)
  - Registry accessible to CI/CD pipeline (credentials injected as secrets)
  - Registry accessible to target Kubernetes clusters (pull secrets configured)

- [ ] **Image Build & Tag Strategy:**
  - Base tag: `<registry>/<image-name>:<commit-sha>` (unique per commit)
  - Release tag: `<registry>/<image-name>:v<semantic-version>` (on git tags)
  - Latest tag: `<registry>/<image-name>:latest` (on successful main branch builds)
  - Example: `myregistry.azurecr.io/port-request-recipient:a1b2c3d4` (commit SHA), `myregistry.azurecr.io/port-request-recipient:v1.0.0` (release)

- [ ] **Push Script** — `/infra/scripts/push-images.sh` or CI job
  - Builds images locally: `docker build -t <image>:<tag> .`
  - Authenticates to registry: `docker login <registry>` (credentials from env vars or secrets file)
  - Pushes images: `docker push <image>:<tag>`
  - Verifies push success: `docker image inspect <registry>/<image>:<tag>` from remote registry

- [ ] **Image Manifest & Metadata:**
  - Image metadata includes git commit, build timestamp, builder version
  - Dockerfile includes `LABEL` directives: `LABEL git.commit="<sha>"`, `LABEL build.timestamp="<date>"`
  - Images scanned for vulnerabilities (optional, but recommended) — use registry's built-in scanner or Trivy

- [ ] **Rollback Support:**
  - All pushed images retained (never deleted) — allows rollback to any previous version
  - Image digest (SHA256) recorded for every tag — enables immutable deployments

- [ ] **Verification:**
  - Manually push image and verify via registry UI or CLI
  - Pull image from another machine: `docker pull <registry>/<image>:<tag>` succeeds
  - Run pulled image: `docker run --rm -p <port>:<port> <registry>/<image>:<tag>` works

**Dependencies:** OPS-S2 (Dockerfiles), OPS-S5 (CI pipeline)  
**Blocks:** OPS-S7

**Notes:**
- Registry credentials never hardcoded — stored as CI secrets (GitHub Secrets, GitLab Variables, etc.)
- Image cleanup policy: retain last 10 versions, delete others (configurable per registry)
- Immutable tags recommended for production: once v1.0.0 is pushed, cannot be overwritten

---

### OPS-S7: Deploy to Kubernetes/Knative (Cloud-Agnostic)

**Epic:** OPS-E1  
**Wave:** 6  
**Story Type:** Story  
**Story Points:** 7

**User Story:**
As a DevOps engineer, I want baseline Kubernetes and Knative manifests so that the same containerized services can be deployed to any CNCF-conformant Kubernetes cluster (on-prem, AWS EKS, Azure AKS, GCP GKE) without modification.

**Acceptance Criteria:**
- [ ] **Kubernetes Manifests** — `/infra/k8s/` directory structure:
  ```
  k8s/
  ├── namespace.yaml               # Namespace: default or custom
  ├── configmap.yaml               # ConfigMap for cloud profile env vars
  ├── secret.yaml                  # Secret for DB passwords, MinIO keys
  ├── recipient-service/
  │   ├── deployment.yaml          # Recipient Service Deployment
  │   └── service.yaml             # Recipient ClusterIP Service
  ├── donor-service/
  │   ├── deployment.yaml
  │   └── service.yaml
  ├── ui/
  │   ├── deployment.yaml          # Angular UI Deployment
  │   └── service.yaml
  └── kustomization.yaml           # Kustomize base (optional, for overlay support)
  ```

- [ ] **Deployment Manifest Details** (Recipient Service example):
  - Replicas: 1 (for POC; production would scale based on load)
  - Image: `<registry>/<image>:<commit-sha>` (pulled from env var or image policy)
  - Container ports: 8080, 8081 (Recipient, Donor)
  - Environment variables injected from ConfigMap (SPRING_PROFILES_ACTIVE=cloud, hostnames)
  - Secrets injected from Secret (DB_PASSWORD, MINIO_KEY)
  - Resource requests/limits: CPU 250m/500m, Memory 256Mi/512Mi (POC defaults)
  - Health check: `livenessProbe` and `readinessProbe` using `/actuator/health` endpoint
  - Service: ClusterIP (internal DNS: `recipient-service.default.svc.cluster.local`)

- [ ] **Knative Service Manifest** (optional, for scale-to-zero):
  - Knative Service wraps Deployment for auto-scaling and scale-to-zero
  - One Knative Service per application component
  - Traffic split: 100% to latest revision
  - Autoscaling: minScale=0 (scale to zero when no traffic), maxScale=10, targetConcurrency=1
  - Timeout: 60s (sufficient for POC)
  - Example: `port-request-recipient.default.knative.dev` as public DNS

- [ ] **ConfigMap & Secret Examples:**
  - **ConfigMap:** Service hostnames (MySQL, RabbitMQ, MinIO using Kubernetes DNS)
  - **Secret:** Base64-encoded MySQL root password, MinIO access key/secret

- [ ] **Deployment Verification** — Local Kubernetes cluster (kind, minikube):
  - Cluster created: `kind create cluster --name poc` or `minikube start`
  - Images pre-loaded into cluster (or cluster configured to pull from registry)
  - Manifests applied: `kubectl apply -f /infra/k8s/`
  - Services start and reach Ready state: `kubectl get pods -w` shows all pods running
  - Health checks pass: `kubectl get endpoints` shows all service endpoints
  - Port-forward to test: `kubectl port-forward svc/recipient-service 8080:8080`
  - Curl endpoint: `curl http://localhost:8080/actuator/health` returns `UP`

- [ ] **Cloud Compatibility** — Manifests work on:
  - AWS EKS (no AWS-specific APIs; uses standard Kubernetes)
  - Azure AKS (no Azure-specific APIs)
  - GCP GKE (no GCP-specific APIs)
  - On-prem Kubernetes (bare metal, vSphere, OpenStack)
  - Proof: Deploy to at least one public cloud cluster (or use kind/minikube as proxy test)

- [ ] **Image Pull & Credentials:**
  - If registry requires authentication, create Kubernetes Secret: `kubectl create secret docker-registry <secret-name> --docker-server=<registry> --docker-username=<user> --docker-password=<password>`
  - Pod spec references imagePullSecret: `spec.imagePullSecrets: [name: <secret-name>]`

**Dependencies:** OPS-S2 (Dockerfiles), OPS-S3 (cloud-agnostic config), OPS-S6 (images in registry), DEV-S11, DEV-S12, DEV-S13 (services exist)  
**Blocks:** None

**Notes:**
- Manifests use `kind: Deployment` for standard Kubernetes; optional `kind: KnativeService` for scale-to-zero
- No cloud-specific annotations, storage classes, or IAM roles — pure Kubernetes
- Production deployment would add: ingress, network policies, resource quotas, pod disruption budgets
- Helm charts optional (added in post-POC enhancement)

---

## EPIC: OPS-E2 - Observability & Operational Readiness

**Epic Goal:** Implement logging, health checks, and metrics so that the operations team can monitor the system's health and debug issues.

**Epic Success Criteria:**
- All services emit structured logs with correlation IDs
- Health checks integrated into containers and Kubernetes probes
- Error rates and latencies visible via aggregated logs (or optional metrics)
- Troubleshooting runbook available for common failure scenarios

---

### OPS-S8: Implement Structured Logging with Correlation IDs

**Epic:** OPS-E2  
**Wave:** 6  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a DevOps engineer, I want structured logging with correlation IDs propagated across services so that the operations team can trace a single request end-to-end through logs.

**Acceptance Criteria:**
- [ ] **Spring Boot Services** — Both Recipient and Donor configured for structured logging:
  - Logback configuration in `src/main/resources/logback-spring.xml`
  - Log format: JSON with fields: `timestamp`, `level`, `logger`, `message`, `correlationId`, `service`, `request.id`
  - Example JSON log:
    ```json
    {
      "timestamp": "2026-08-11T10:30:45.123Z",
      "level": "INFO",
      "logger": "com.example.PortRequestService",
      "message": "Port request submitted",
      "correlationId": "abc-123-def",
      "service": "recipient-service",
      "requestId": "xyz-789"
    }
    ```

- [ ] **Correlation ID Propagation:**
  - Recipient Service generates or extracts correlation ID from `X-Correlation-ID` header on incoming requests
  - Correlation ID stored in Spring's `MDC` (Mapped Diagnostic Context) for automatic inclusion in logs
  - Correlation ID passed to RabbitMQ message headers when publishing events
  - Donor Service extracts correlation ID from RabbitMQ message and stores in MDC
  - n8n receives correlation ID in HTTP request headers and passes to Recipient callback
  - All logs for same request share same correlationId

- [ ] **Angular UI** — Error logging:
  - Frontend errors (JavaScript exceptions, HTTP errors) logged to browser console with timestamp and request ID
  - Optional: send JavaScript error events to backend logging service (for aggregation)

- [ ] **Verification:**
  - Manually trigger a port request in UI
  - Grep logs for correlationId: `docker-compose logs -f | grep <correlationId>`
  - Trace appears in all services: Recipient (request received), RabbitMQ → Donor (consumed), Donor (published), n8n (triggered), Recipient (callback received)

**Dependencies:** DEV-S11, DEV-S13, OPS-S1  
**Blocks:** OPS-S9

**Notes:**
- Structured logging (JSON) enables automated log parsing and aggregation (Elasticsearch, Splunk, DataDog, etc.)
- MDC automatic propagation via Spring Boot Actuator or manual context propagation

---

### OPS-S9: Create Operational Runbook for Common Issues

**Epic:** OPS-E2  
**Wave:** 7  
**Story Type:** Story  
**Story Points:** 4

**User Story:**
As a DevOps engineer, I want an operational runbook documenting common failure scenarios and resolution steps so that on-call teams can troubleshoot without waiting for developers.

**Acceptance Criteria:**
- [ ] **Runbook Document:** `/infra/RUNBOOK.md` with sections:
  1. **Recipient Service Pod Won't Start**
     - Check symptom: `kubectl logs <pod-name>`
     - Causes: database unreachable, RabbitMQ unreachable, image pull error
     - Resolution: verify MySQL service running, check RabbitMQ connectivity, check image registry credentials
  
  2. **Donor Service Not Consuming Messages**
     - Check symptom: Messages accumulate in RabbitMQ queue
     - Causes: consumer crashed, consumer not connected to queue, malformed message
     - Resolution: check Donor pod logs, verify queue bindings in RabbitMQ UI, replay message
  
  3. **Dashboard Not Updating Status**
     - Check symptom: UI shows INITIATED, status doesn't change to COMPLETED
     - Causes: n8n workflow failed, callback endpoint unreachable, database update failed
     - Resolution: check n8n execution logs, check Recipient service health, query database for status
  
  4. **MinIO Bucket Not Found**
     - Check symptom: Receipt upload fails, 404 error from MinIO
     - Causes: bucket not created, wrong bucket name, MinIO service down
     - Resolution: create bucket manually, verify MINIO_BUCKET env var, check MinIO health
  
  5. **Database Connection Pool Exhausted**
     - Check symptom: Recipient service logs show "connection timeout"
     - Causes: too many concurrent requests, database server down, network latency
     - Resolution: check MySQL health, reduce connection pool size, scale up replicas

- [ ] **Health Check Commands** — Quick diagnostics:
  - Health check all services: script to curl `/actuator/health` on all services
  - Check RabbitMQ: connect to management UI, verify queues and exchanges
  - Check MySQL: query system tables to verify connectivity
  - Check MinIO: test bucket access via MinIO client

- [ ] **Log Query Cheatsheet:**
  - View logs for specific service: `docker-compose logs <service-name>`
  - View logs for specific correlation ID: `docker-compose logs | grep <correlationId>`
  - View logs since timestamp: `docker-compose logs --since 2026-08-11T10:00:00`

- [ ] **Escalation Path:**
  - Who to contact if runbook resolution doesn't work
  - On-call developer contact, dev team Slack channel, ticket system

**Dependencies:** OPS-S8 (logging), QA-S4 (E2E test for health verification)  
**Blocks:** None

**Notes:**
- Runbook updated as new issues discovered — treat as living document
- Operational readiness check: on-call engineer should be able to diagnose any of the above scenarios in < 5 minutes using runbook

---

# SPRINT SUMMARY & DEPENDENCIES

## Wave-by-Wave Dependency Graph

```
WAVE 1 (Kickoff — No blockers)
├── DEV-S1: Initialize Monorepo
└── QA-S1: Test Plan

WAVE 2 (Scaffolds — Depends on WAVE 1)
├── DEV-S2: Angular UI Scaffold (← DEV-S1)
├── DEV-S3: Recipient Service Scaffold (← DEV-S1)
├── DEV-S4: Donor Service Scaffold (← DEV-S1)
├── OPS-S1: Docker Compose Infra (← DEV-S1)
└── QA-S2: Author API Tests (← QA-S1, no code dependency)

WAVE 3 (Domain Layer — Depends on WAVE 2)
├── DEV-S5: PortRequest Entity (Recipient) (← DEV-S3)
├── DEV-S6: PortRequest Model (Donor) (← DEV-S4)
├── OPS-S2: Dockerfiles (← DEV-S2, DEV-S3, DEV-S4)
├── OPS-S3: Config Profiles (← DEV-S3, DEV-S4)
└── OPS-S5: CI/CD Pipeline (← OPS-S2)

WAVE 4 (Adapters — Depends on WAVE 3)
├── DEV-S7: RabbitMQ Producer (← DEV-S5, OPS-S3)
├── DEV-S9: MySQL Persistence (← DEV-S5, OPS-S3)
├── DEV-S10: MinIO Storage (← OPS-S1, OPS-S3)
└── OPS-S4: Connectivity Verification (← OPS-S1, OPS-S3)

WAVE 5 (Cross-Service — Depends on WAVE 4)
├── DEV-S8: Donor RabbitMQ (← DEV-S6, DEV-S7)
├── OPS-S6: Container Registry (← OPS-S2, OPS-S5)
└── QA-S3: Messaging Tests (← OPS-S1, DEV-S8)

WAVE 6 (Application Layer — Depends on WAVE 5)
├── DEV-S11: REST API (← DEV-S7, DEV-S9, DEV-S10)
├── DEV-S12: Angular UI (← DEV-S2, DEV-S11)
├── DEV-S13: n8n Workflow (← OPS-S1, DEV-S8, DEV-S11)
├── OPS-S7: Kubernetes Manifests (← OPS-S2, OPS-S3, OPS-S6, DEV-S11)
├── OPS-S8: Structured Logging (← DEV-S11, DEV-S13)
└── QA-S2: Execute API Tests (← QA-S2 authoring, DEV-S11)

WAVE 7 (Acceptance — Final gate)
├── QA-S4: E2E Smoke Test (← DEV-S12, DEV-S13, QA-S2, QA-S3)
├── QA-S5: CI/CD Integration (← OPS-S5, QA-S2, QA-S3, QA-S4)
└── OPS-S9: Operational Runbook (← OPS-S8, QA-S4)
```

## Role-Based Story Counts

| Role | Epic Count | Story Count | Story Points |
|------|-----------|------------|--------------|
| **Dev** | 3 | 13 | 63 |
| **QA** | 1 | 5 | 27 |
| **DevOps** | 2 | 9 | 49 |
| **TOTAL** | 6 | 27 | 139 |

## Recommended Sprint Velocity & Duration

- **Small Team (1-2 per role):** 15–20 points/week → Sprint 1 ~7–9 weeks
- **Medium Team (2-3 per role):** 25–30 points/week → Sprint 1 ~5–6 weeks
- **Large Team (3+ per role):** 35–50 points/week → Sprint 1 ~3–4 weeks

**Recommendation:** Plan for 6 weeks, execute in parallel across waves.

---

# JIRA IMPORT CHECKLIST

To import into Jira:

1. **Create Project & Epics First:**
   - Project Key: `KAN` (AI SDLC)
   - Create Epics: DEV-E1, DEV-E2, DEV-E3, QA-E1, OPS-E1, OPS-E2

2. **Import Stories as Bulk:**
   - Use Jira CSV import or API bulk create
   - Fields: Summary, Description, Story Type (Story), Story Points, Epic Link, Labels, Component

3. **Link Dependencies:**
   - Use "Blocks" and "Is Blocked By" link types to establish wave dependencies
   - DEV-S2/S3/S4 all blocked by DEV-S1, etc.

4. **Add Custom Fields (if available):**
   - Wave (dropdown: 1–7)
   - Role (dropdown: Dev, QA, DevOps)
   - Acceptance Criteria (rich text)

5. **Assign to Team Members:**
   - Assign based on role and availability
   - Ensure no single person blocked by too many dependencies

---
