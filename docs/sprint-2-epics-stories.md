# Sprint 2: Epics & User Stories (Dev / DevOps)

This document defines Sprint 2 as a hierarchy of Epics and User Stories, structured for direct Jira backlog reference. Each story includes clear acceptance criteria, dependencies, and wave assignment. Sprint 2 adds production-readiness behaviors (security, reliability, notifications) and scalability/observability infrastructure to the Sprint-1 scaffolding.

**Prerequisite:** Sprint 1 domain logic, REST API, RabbitMQ messaging, and core infrastructure (`infra/docker-compose.yml`, `infra/k8s/`) must be fully implemented and tested before Sprint 2 stories can be executed. This document assumes Sprint 1 (`DEV-E1` through `DEV-E3`, `OPS-E1`/`E2`) is complete; each Sprint 2 story's Dependencies line cites its Sprint 1 blocker explicitly.

---

# DEVELOPER EPICS & STORIES

## EPIC: DEV-E4 - Production Readiness Enhancements

**Epic Goal:** Move the POC toward production readiness by adding the security, review, and reliability behaviors that were deliberately excluded from Sprint 1.

**Epic Success Criteria:**
- REST endpoints require an API key
- Donor requires explicit approval before accepting a port request (auto-accept removed)
- Failed RabbitMQ message consumption goes to a dead-letter queue instead of being silently dropped
- Recipient sends a notification on status change to COMPLETED

**Stories in this Epic:** DEV-S14, DEV-S15, DEV-S16, DEV-S17

---

### DEV-S14: Require API Key on REST Endpoints

**Epic:** DEV-E4  
**Wave:** 1  
**Story Type:** Story  
**Story Points:** 3

**User Story:**
As a security engineer, I want REST endpoints to require an API key so that unauthenticated callers cannot trigger port requests.

**Acceptance Criteria:**
- [ ] Spring Security `OncePerRequestFilter` implemented in `infrastructure/security/ApiKeyFilter.java`
  - Intercepts all requests matching `/api/v1/**`
  - Extracts `X-API-Key` header from incoming request
  - Validates header value against `API_KEY` environment variable (injected via Spring `@Value` or `application-local.yml`)
  - Returns 401 Unauthorized with error-response shape `{ "error": "Unauthorized", "timestamp": "ISO8601", "path": "/api/v1/..." }` on missing or invalid key
  - Logs validation failures with correlation ID (if present) for audit trail
  
- [ ] Filter registered in Spring configuration (e.g., via `@Configuration` + `addFilterBefore()` or `@Component` with `@Order`)
  - Donor Service filter **not** required (Donor has no REST endpoints, only RabbitMQ consumer)
  - Filter applies only to Recipient Service

- [ ] `application-local.yml` (Recipient) defines default API key (for local testing)
  - Recommendation: `api-key: test-key-local` (clearly marked as insecure)

- [ ] `infra/k8s/secret.yaml` includes new secret key
  - Key name: `API_KEY`
  - Value: base64-encoded production API key (to be set by DevOps at deployment time)
  - Recipient pod `env` section references this Secret: `- name: API_KEY; valueFrom: { secretKeyRef: { name: port-poc-secrets, key: API_KEY } }`

- [ ] n8n workflow updated to send API key
  - HTTP Request node (that calls Recipient's callback endpoint `POST /api/v1/port-requests/{id}/complete`) adds header: `X-API-Key: ${API_KEY_VALUE}` (n8n credential or environment variable)
  - Callback succeeds (200 response received by n8n) with header present
  - Callback fails (401 response received by n8n) with header missing or incorrect

- [ ] Integration test confirms filter behavior
  - Test fixture: POST to `/api/v1/port-requests` without header → 401
  - Test fixture: POST to `/api/v1/port-requests` with incorrect header → 401
  - Test fixture: POST to `/api/v1/port-requests` with correct header → 201 (success, or appropriate response per endpoint)
  - Test fixture: GET to `/api/v1/port-requests/{id}` without header → 401
  - Test fixture: POST to `/api/v1/port-requests/{id}/complete` without header → 401

**Dependencies:** DEV-S11 (Recipient REST API endpoints must exist)  
**Blocks:** None (but n8n workflow updates depend on this being documented)

**Notes:**
- This is a security gate, not an authentication mechanism — no identity/roles, only a pre-shared key. Consistent with the project's "no authentication" scope boundary being relaxed only to prevent accidental unauthenticated calls during the transition to production-like behavior.
- API key should be rotatable via environment variable without code changes.
- Header name `X-API-Key` is a convention; any header is acceptable as long as it's documented in an updated README or CLAUDE.md.

---

### DEV-S15: Donor Requires Explicit Approval (Remove Auto-Accept)

**Epic:** DEV-E4  
**Wave:** 1  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a product owner, I want the Donor to require explicit approval before accepting a port request so that we can implement a manual-review workflow instead of auto-acceptance.

**Acceptance Criteria:**
- [ ] **Design Decision:** Story implementer must explicitly choose one of two approaches and document it in commit message/PR description:
  
  **Option A: In-Memory Pending Queue (Donor-side approval)**
  - Donor service receives `port.request.initiated` event
  - Message consumed, but request held in a bounded in-memory queue (e.g., `ConcurrentHashMap<UUID, PortRequest>`, max 1000 entries with LRU eviction)
  - New REST endpoint added: `POST /donor-service/pending/{requestId}/approve` (internal, not called by UI)
    - Approval trigger: n8n workflow calls this instead of auto-publishing
    - Approves the request by requestId, publishes `port.request.accepted` to RabbitMQ, removes from pending queue
    - Returns 200 on success, 404 if requestId not in pending queue
  - Rejected requests timeout after 5 minutes (configurable) or must be manually rejected via a complementary `DELETE /pending/{requestId}` endpoint
  - Pending queue contents logged periodically (every 60s) for observability
  
  **Option B: Approval Gate in Recipient (Recipient owns approval state)**
  - Donor service receives `port.request.initiated` event and immediately publishes `port.request.accepted` (behavior unchanged from Sprint 1)
  - But Recipient adds a new intermediate status: `APPROVED_BY_DONOR` (or similar)
  - Recipient's status transitions: `INITIATED` → `AWAITING_APPROVAL` (on receipt of `port.request.initiated`) → `APPROVED_BY_DONOR` (on receipt of `port.request.accepted` OR on manual approval via a new REST endpoint `POST /api/v1/port-requests/{id}/approve-donor`)
  - Manual approval endpoint callable by internal services/admin tooling (API-key-authenticated per DEV-S14)
  - n8n polls for `AWAITING_APPROVAL` status and calls the approval endpoint
  
  **Recommendation in story:** Option A (in-memory pending) keeps Donor stateless per the architecture doc's hard rule ("no `@Repository`/`@Service` persistence layer"), making it the architecturally consistent choice. Document this choice clearly.

- [ ] Chosen approach fully implemented and tested:
  - Unit tests confirm pending-queue behavior or approval-gate transitions
  - Integration test against RabbitMQ (Option A: message consumed but held; Option B: message published immediately but Recipient status transitions correctly)
  - Manual test: trigger a submission, verify the request enters approval state, approve it, verify status progresses to next state

- [ ] Acceptance criteria for old auto-accept behavior removed
  - Any tests from Sprint 1's DEV-S8 that expected auto-acceptance without approval must be updated or removed

**Dependencies:** DEV-S8 (Donor RabbitMQ consumer must exist; this story modifies its behavior)  
**Blocks:** None (but n8n workflow orchestration will need to adapt to call an approval endpoint instead of assuming auto-acceptance)

**Notes:**
- This represents a significant reversal of Sprint 1's deliberate design ("no manual review"), so the choice of implementation approach should be driven by architectural consistency with the "stateless Donor" rule, not convenience.
- Timeout/eviction policies for pending requests should be documented in code comments if Option A is chosen.
- Consider whether approval can be audited (who/when approved) — if so, Option B (in Recipient) is more traceable since Recipient already owns state.

---

### DEV-S16: Failed Message Consumption Goes to a Dead-Letter Queue

**Epic:** DEV-E4  
**Wave:** 2  
**Story Type:** Story  
**Story Points:** 6

**User Story:**
As a DevOps engineer, I want failed RabbitMQ message consumption to route to a dead-letter queue so that transient failures don't silently drop messages and we can replay them later.

**Acceptance Criteria:**
- [ ] **RabbitMQ Topology (Spring AMQP `@Bean` configuration):**
  - Primary exchange: `port-exchange` (DirectExchange)
  - Primary queues:
    - `port.request.initiated` — bound to `port-exchange` with routing key `port.request.initiated`
    - `port.request.accepted` — bound to `port-exchange` with routing key `port.request.accepted`
  
  - Dead-letter exchange (DLX): `port-dlx` (DirectExchange)
  - Dead-letter queues (DLQ):
    - `port.request.initiated.dlq` — bound to `port-dlx` with routing key `port.request.initiated.dlq`
    - `port.request.accepted.dlq` — bound to `port-dlx` with routing key `port.request.accepted.dlq`
  
  - Each primary queue configured with `x-dead-letter-exchange=port-dlx` and `x-dead-letter-routing-key=<queue-name>.dlq` arguments
  - Each primary queue has TTL policy: messages expire after 1 hour if not consumed (optional, but recommended for cleanup)
  - Verify topology via RabbitMQ management UI: `http://localhost:15672` (local) or kubectl exec into RabbitMQ pod (cloud) → confirm exchanges/queues/bindings present

- [ ] **Consumer Error Handling (both Recipient Producer and Donor Consumer):**
  - RabbitMQ listener container configured with:
    - `AcknowledgeMode.MANUAL` (explicit acknowledgment, so failed messages are requeued)
    - `RetryTemplate` with exponential backoff: initial interval 1s, max interval 10s, max attempts 3
    - On exhaustion (3 attempts failed), reject message without requeue: `.reject()` → RabbitMQ routes to DLX
  
  - Consumer error handler logs full context: `logger.error("Failed to process message: {}", message, exception)` with correlation ID from message header
  - No silent failures — every failed message is visible in logs

- [ ] **Monitoring & Observability:**
  - Dashboard or ad-hoc query to check DLQ depth (number of messages in `port.request.initiated.dlq` / `port.request.accepted.dlq`)
  - Command to monitor: `docker-compose exec rabbitmq rabbitmq-diagnostics list_queues name messages` (local)
  - Or via RabbitMQ HTTP API: `curl http://localhost:15672/api/queues/port-poc` (local, requires auth)
  - Verify DLQ messages contain full original message (not just metadata) for replay/debugging

- [ ] **Replay Capability:**
  - Manual replay path documented (not automated):
    - DLQ consumer exists (optional, for this story) or operator must manually pull messages from DLQ via RabbitMQ CLI/UI and republish to primary queue
    - Acceptable for POC: document the manual steps in `infra/RUNBOOK.md` under a "Dead-Letter Queue Recovery" section

- [ ] **Integration Tests:**
  - Test: publish valid message to primary queue → consumed successfully, not in DLQ
  - Test: publish message with malformed payload → consumer error handler triggered, message rejected, message appears in DLQ
  - Test: publish same malformed message → retried 3 times, then moved to DLQ (confirm via DLQ inspection)
  - Test: simulate consumer exception (throw from handler) → message rejected, moved to DLQ on exhaustion

**Dependencies:** DEV-S7 (Recipient producer exchange topology must exist) and DEV-S8 (Donor consumer must exist)  
**Blocks:** None (observability dependency is OPS-S11, but DLQ is functional independently)

**Notes:**
- Dead-lettering is a production-readiness best practice — this reverses Sprint 1's explicit "no dead-lettering" scope boundary.
- Retention policy for DLQ messages (expire after X days, archive, etc.) is a post-POC enhancement; for now, DLQ messages persist indefinitely.
- If replay automation is desired in future, consider a separate story for a DLQ consumer that republishes on command or on exponential retry schedule.

---

### DEV-S17: Recipient Sends Notification on Completion

**Epic:** DEV-E4  
**Wave:** 2  
**Story Type:** Story  
**Story Points:** 4

**User Story:**
As an operator, I want the Recipient to send a notification when a port request completes so that stakeholders can be alerted without polling the dashboard.

**Acceptance Criteria:**
- [ ] **Notification Channel (HTTP Webhook Recommended):**
  - Recipient Service publishes notification via configurable HTTP POST to an external webhook URL
  - Configuration:
    - Environment variable: `NOTIFICATION_WEBHOOK_URL` (e.g., `http://webhook-receiver:3000/notify`)
    - If unset, notifications silently skipped (no error) — so the system works without a webhook infrastructure
  
  - HTTP request format:
    ```json
    {
      "requestId": "uuid",
      "status": "COMPLETED",
      "completedAt": "ISO8601",
      "correlationId": "uuid",
      "customerId": "string"
    }
    ```
  - Header: `Content-Type: application/json`, `X-Correlation-ID: {correlationId}` (for tracing)

- [ ] **Notification Trigger:**
  - Notification published in the same transaction/operation as status update to `COMPLETED`
  - Happens when Recipient's `POST /api/v1/port-requests/{id}/complete` endpoint (from DEV-S11) successfully updates status
  - Trigger happens after database commit (to avoid re-notifying on transaction rollback)

- [ ] **Error Handling (consistent with project's "log, don't retry" pattern):**
  - HTTP request timeout: 5 seconds
  - On HTTP error (4xx, 5xx, timeout, connection refused):
    - Log error: `logger.warn("Failed to send completion notification for request {}: {}", requestId, error)` with full exception trace
    - Do NOT retry the notification
    - Do NOT block the status update — status transition still completes even if notification fails
  - On HTTP success (2xx): log at INFO level: `logger.info("Sent completion notification for request {} to {}", requestId, webhookUrl)`

- [ ] **Implementation Options (choose one, document choice):**

  **Option A: Spring RestTemplate/RestClient (Recommended)**
  - Use Spring `RestClient` (Spring 6.1+, recommended) or `RestTemplate` (legacy but stable)
  - Injected as `@Bean` in a service class (e.g., `NotificationService`)
  - Called from the service layer after status update succeeds
  
  **Option B: Spring WebClient (Reactive, optional)**
  - If chosen, must handle reactive model correctly (call `.block()` or wrap in reactive context)
  - For simplicity in a POC, RestTemplate is preferred over WebClient

- [ ] **Testing:**
  - Unit test: mock HTTP client, verify request body format is correct
  - Integration test (with mock webhook):
    - Start a simple mock webhook server on localhost (e.g., WireMock or testcontainers)
    - Trigger a port request completion
    - Verify webhook receives expected request
  - Integration test (no webhook configured):
    - `NOTIFICATION_WEBHOOK_URL` unset
    - Trigger completion
    - Verify status updates successfully (no errors logged about missing webhook)

- [ ] **Documentation:**
  - README or CLAUDE.md updated to document `NOTIFICATION_WEBHOOK_URL` configuration
  - Example provided for a sample webhook endpoint (e.g., Slack webhook, internal logging service)

**Dependencies:** DEV-S11 (Recipient's `complete` endpoint / status update logic must exist)  
**Blocks:** None

**Notes:**
- "Notification" was explicitly out-of-scope in Sprint 1 (`docs/core-requirements.md`); this story implements a minimal, pluggable version.
- Webhook channel chosen over SMTP/Mailhog because it's simpler to integrate (no mail server setup needed for POC) and more flexible (can point to any HTTP endpoint: Slack, custom service, etc.).
- No retry/exponential backoff — if notification fails, operator is responsible for checking the dashboard; the system prioritizes consistency of the core request state over guaranteed notification delivery.

---

# DEVOPS EPICS & STORIES

## EPIC: OPS-E3 - Scalability & Observability Enhancements

**Epic Goal:** Package the deployment for repeatable installs and add the visibility and scaling behavior needed to run beyond a single-demo POC.

**Epic Success Criteria:**
- Kubernetes manifests are packaged as a versioned, configurable Helm chart
- Services expose Prometheus-compatible metrics with a starter Grafana dashboard
- Recipient and Donor scale horizontally based on load via HPA
- Resource requests/limits are right-sized and documented with a cost rationale

**Stories in this Epic:** OPS-S10, OPS-S11, OPS-S12, OPS-S13

---

### OPS-S10: Package Kubernetes Manifests as a Versioned, Configurable Helm Chart

**Epic:** OPS-E3  
**Wave:** 1  
**Story Type:** Story  
**Story Points:** 6

**User Story:**
As a platform engineer, I want Kubernetes manifests packaged as a Helm chart so that the system can be deployed repeatably across multiple environments with configurable values.

**Acceptance Criteria:**
- [ ] **Helm Chart Directory Structure:**
  ```
  infra/helm/port-poc/
  ├── Chart.yaml
  ├── values.yaml
  ├── values-local.yaml                  # override for local development
  ├── values-cloud.yaml                  # override for Kubernetes deployments
  ├── templates/
  │   ├── namespace.yaml
  │   ├── configmap.yaml
  │   ├── secret.yaml
  │   ├── recipient-service/
  │   │   ├── deployment.yaml
  │   │   └── service.yaml
  │   ├── donor-service/
  │   │   ├── deployment.yaml
  │   │   └── service.yaml
  │   │   └── knative-service.yaml       # (optional, controlled by values)
  │   ├── ui/
  │   │   ├── deployment.yaml
  │   │   └── service.yaml
  │   └── support/                        # (MySQL, RabbitMQ, MinIO for test clusters)
  │       ├── mysql.yaml
  │       ├── rabbitmq.yaml
  │       └── minio.yaml
  └── charts/                             # (empty for now, reserved for subchart dependencies)
  ```

- [ ] **Chart.yaml:**
  - `apiVersion: v2` (Helm 3)
  - `name: port-poc`
  - `version: 0.1.0` (starts at 0.1.0 for POC)
  - `appVersion: "1.0"` (application version)
  - `description: "Port Request Portability POC - Kubernetes deployment"`
  - `type: application`
  - `keywords: ["port-request", "kubernetes", "helm"]`

- [ ] **values.yaml (main defaults):**
  - **Namespace:**
    - `namespace: port-poc`
  
  - **Image configuration:**
    - `image.registry: docker.io` (or your registry)
    - `image.tag: latest` (default; override per deployment)
    - `image.pullPolicy: IfNotPresent`
    - Individual service images: `recipient.image`, `donor.image`, `ui.image` (if different registries needed)
  
  - **Replica counts:**
    - `recipient.replicas: 1`
    - `donor.replicas: 1`
    - `ui.replicas: 1`
  
  - **Resource requests/limits (POC defaults, to be refined by OPS-S13):**
    - `recipient.resources.requests: { cpu: 250m, memory: 256Mi }`
    - `recipient.resources.limits: { cpu: 500m, memory: 512Mi }`
    - (same pattern for donor, ui)
  
  - **Environment & configuration:**
    - `spring.profiles.active: cloud` (for Kubernetes deployments)
    - `mysql.host: mysql.port-poc.svc.cluster.local`
    - `mysql.port: 3306`
    - `rabbitmq.host: rabbitmq.port-poc.svc.cluster.local`
    - `rabbitmq.port: 5672`
    - `minio.host: minio.port-poc.svc.cluster.local`
    - `minio.port: 9000`
  
  - **Feature toggles:**
    - `donorKnativeEnabled: false` (plain Deployment by default)
    - If true, uses `templates/donor-service/knative-service.yaml` instead of plain Deployment
    - If false, uses plain `templates/donor-service/deployment.yaml`
  
  - **Service types:**
    - `recipient.service.type: ClusterIP` (or LoadBalancer if needing external access)
    - `donor.service.type: ClusterIP`
    - `ui.service.type: ClusterIP`

- [ ] **values-local.yaml (override for Docker Compose workflow, optional):**
  - `namespace: default`
  - `image.tag: dev` (points to locally-built images)
  - `mysql.host: mysql` (Docker Compose service name, not Kubernetes DNS)
  - `rabbitmq.host: rabbitmq`
  - `minio.host: minio`

- [ ] **values-cloud.yaml (override for production clusters):**
  - `image.registry: your-registry.azurecr.io` (or ECR, GCR, etc.)
  - `image.tag: v1.0.0` (semantic versioning)
  - `recipient.replicas: 3` (scale up for production)
  - `donor.replicas: 2`
  - `donorKnativeEnabled: true` (if desired for production)
  - Resource limits increased as per OPS-S13

- [ ] **Templates converted from existing manifests:**
  - Each existing YAML file under `infra/k8s/` templated with Helm variable syntax (`{{ .Values.xxx }}`, `{{ .Release.Namespace }}`, `{{ include "port-poc.labels" . }}`, etc.)
  - Template helpers (e.g., `port-poc.labels`, `port-poc.fullname`) defined via Helm template functions
  - Conditional blocks for Knative vs. plain Deployment: `{{ if .Values.donorKnativeEnabled }}`

- [ ] **Helm Lint & Validation:**
  - Chart passes `helm lint infra/helm/port-poc` without warnings (or with documented exceptions)
  - Chart templates render without errors: `helm template my-release infra/helm/port-poc` produces valid YAML
  - Rendered manifests match original plain YAML in structure

- [ ] **Deployment Verification (kind cluster):**
  - Local Kubernetes cluster created: `kind create cluster --name poc`
  - Helm chart deployed: `helm install my-release infra/helm/port-poc --values infra/helm/port-poc/values.yaml`
  - All pods reach Running state: `kubectl get pods -n port-poc -w`
  - Services have endpoints: `kubectl get endpoints -n port-poc`
  - Port-forward and test endpoint: `kubectl port-forward svc/recipient-service 8080:8080` → `curl http://localhost:8080/actuator/health` returns `UP`

- [ ] **Helm Chart Versioning & Updates:**
  - `Chart.yaml` version bumped to `0.2.0` after any template changes (not required for values-only changes)
  - Chart included in version control (`infra/helm/` committed to git)

**Dependencies:** OPS-S7 (plain Kubernetes manifests must exist from Sprint 1; this story packages them as a chart)  
**Blocks:** OPS-S12 (HPA templates reference Helm values), OPS-S13 (resource sizing values)

**Notes:**
- Helm 3+ required (no Tiller server).
- For initial POC, hardcoding non-sensitive values in `values.yaml` is acceptable; production deployments would layer additional overrides via CI/CD pipelines.
- Knative toggle (`donorKnativeEnabled`) allows chart to support both the scale-to-zero (Knative) and traditional (Deployment) paths already documented in `CLAUDE.md`.

---

### OPS-S11: Expose Prometheus Metrics with a Starter Grafana Dashboard

**Epic:** OPS-E3  
**Wave:** 2  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a DevOps engineer, I want the services to expose Prometheus-compatible metrics and a prebuilt Grafana dashboard so that operators can monitor request latency, error rates, and system health.

**Acceptance Criteria:**
- [ ] **Micrometer/Prometheus Dependency (pom.xml):**
  - Add to both `recipient-service/pom.xml` and `donor-service/pom.xml`:
    ```xml
    <dependency>
      <groupId>io.micrometer</groupId>
      <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    ```
  - Micrometer auto-configures when Prometheus is on the classpath (no additional Spring bean required)

- [ ] **Spring Boot Actuator Prometheus Endpoint:**
  - Update `application.yml` for both services:
    ```yaml
    management:
      endpoints:
        web:
          exposure:
            include: health,info,prometheus
    ```
  - Verify endpoint is accessible: `curl http://localhost:8080/actuator/prometheus` (Recipient) returns Prometheus text format
  - Endpoint returns metrics in Prometheus text format (lines of `metric_name{labels} value`)

- [ ] **Prometheus Service (Docker Compose):**
  - Add to `infra/docker-compose.yml`:
    ```yaml
    prometheus:
      image: prom/prometheus:latest
      container_name: prometheus
      ports:
        - "9090:9090"
      volumes:
        - ./prometheus.yml:/etc/prometheus/prometheus.yml
        - prometheus-data:/prometheus
      command:
        - '--config.file=/etc/prometheus/prometheus.yml'
        - '--storage.tsdb.path=/prometheus'
      healthcheck:
        test: ["CMD", "curl", "-f", "http://localhost:9090/-/healthy"]
        interval: 10s
        timeout: 5s
        retries: 10
      networks:
        - port-poc
    ```
  - Add named volume: `prometheus-data:` under `volumes:` section

- [ ] **Prometheus Configuration (infra/prometheus.yml):**
  - Create new file `infra/prometheus.yml`:
    ```yaml
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
    
    scrape_configs:
      - job_name: recipient-service
        static_configs:
          - targets: ['recipient-service:8080']
        metrics_path: /actuator/prometheus
      
      - job_name: donor-service
        static_configs:
          - targets: ['donor-service:8081']
        metrics_path: /actuator/prometheus
    ```
  - Prometheus UI accessible at `http://localhost:9090` (local)
  - Graph a sample metric (e.g., `http_requests_total`) to verify scraping works

- [ ] **Grafana Service (Docker Compose):**
  - Add to `infra/docker-compose.yml`:
    ```yaml
    grafana:
      image: grafana/grafana:latest
      container_name: grafana
      ports:
        - "3000:3000"
      environment:
        GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD:-admin}
        GF_USERS_ALLOW_SIGN_UP: "false"
      volumes:
        - grafana-data:/var/lib/grafana
        - ./grafana/provisioning/dashboards:/etc/grafana/provisioning/dashboards
        - ./grafana/provisioning/datasources:/etc/grafana/provisioning/datasources
      healthcheck:
        test: ["CMD", "curl", "-f", "http://localhost:3000/api/health"]
        interval: 10s
        timeout: 5s
        retries: 10
      networks:
        - port-poc
    ```
  - Add named volume: `grafana-data:` under `volumes:` section
  - Grafana UI accessible at `http://localhost:3000` (login: admin/admin by default)

- [ ] **Grafana Prometheus Datasource (infra/grafana/provisioning/datasources/prometheus.yml):**
  - Create directory structure: `infra/grafana/provisioning/datasources/`
  - File `prometheus.yml`:
    ```yaml
    apiVersion: 1
    datasources:
      - name: Prometheus
        type: prometheus
        access: proxy
        url: http://prometheus:9090
        isDefault: true
    ```
  - On Grafana startup, Prometheus datasource auto-provisioned (no manual configuration needed)

- [ ] **Grafana Starter Dashboard (infra/grafana/provisioning/dashboards/port-poc-dashboard.json):**
  - Dashboard JSON file with charts:
    - **HTTP Request Latency** (50th, 95th, 99th percentiles)
      - Query: `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))`
    - **Request Rate** (requests per second)
      - Query: `rate(http_requests_total[1m])`
    - **Error Rate** (4xx + 5xx responses as % of total)
      - Query: `rate(http_requests_total{status=~"4..|5.."}[1m]) / rate(http_requests_total[1m])`
    - **RabbitMQ Message Count** (messages in queues)
      - Query: (if exposed via RabbitMQ exporter, optional; placeholder OK for POC)
    - **System Health** (JVM memory usage, GC time)
      - Queries: `jvm_memory_usage_bytes`, `jvm_gc_pause_seconds`
  
  - Dashboard title: "Port-POC System Overview"
  - Set refresh interval to 30 seconds

- [ ] **Kubernetes Deployment (Helm Chart or plain manifests):**
  - Prometheus service added to `infra/k8s/support/` or templated in Helm chart (optional for POC, but recommended)
  - Grafana service similar placement
  - ServiceMonitor or plain static scrape config (no Prometheus Operator required for POC)

- [ ] **Testing & Verification:**
  - Docker Compose stack running: `docker-compose up -d`
  - Services healthy and exposing metrics:
    - `curl http://localhost:8080/actuator/prometheus | head -20` (Recipient)
    - `curl http://localhost:8081/actuator/prometheus | head -20` (Donor)
  - Prometheus scraping targets: visit `http://localhost:9090/targets` → both services marked as "Up"
  - Grafana dashboard loads: `http://localhost:3000` → select "Port-POC System Overview" dashboard → metrics visible

**Dependencies:** existing actuator dependency (already in `pom.xml`), OPS-S1 (Docker Compose stack)  
**Blocks:** OPS-S12 (HPA needs metrics for CPU-based scaling)

**Notes:**
- Micrometer is auto-configured by Spring Boot; no additional bean wiring needed.
- Prometheus scrape interval (15s) is fine for POC; production would likely use 30s or longer.
- Grafana dashboards can be exported as JSON and versioned in git for repeatability.
- RabbitMQ metrics (queue depth, message rate) not included in starter dashboard (would require a separate RabbitMQ exporter or manual AMQP instrumentation); placeholder for future enhancement.

---

### OPS-S12: Recipient and Donor Scale Horizontally via HPA

**Epic:** OPS-E3  
**Wave:** 3  
**Story Type:** Story  
**Story Points:** 4

**User Story:**
As a platform engineer, I want Recipient and Donor services to scale horizontally based on CPU load so that the system handles traffic spikes without manual intervention.

**Acceptance Criteria:**
- [ ] **HorizontalPodAutoscaler Manifests/Helm Templates:**
  - `infra/k8s/recipient-service/hpa.yaml` (or templated in Helm chart):
    ```yaml
    apiVersion: autoscaling/v2
    kind: HorizontalPodAutoscaler
    metadata:
      name: recipient-service-hpa
      namespace: port-poc
    spec:
      scaleTargetRef:
        apiVersion: apps/v1
        kind: Deployment
        name: recipient-service
      minReplicas: 1
      maxReplicas: 5
      metrics:
        - type: Resource
          resource:
            name: cpu
            target:
              type: Utilization
              averageUtilization: 70
    ```
  
  - `infra/k8s/donor-service/hpa.yaml` (similar structure, but **only for plain Deployment path**, not Knative)

- [ ] **Preconditions:**
  - Kubernetes cluster has `metrics-server` installed (required for resource metrics; e.g., `kind` includes it by default, EKS requires explicit install)
  - Target Deployment has `resources.requests.cpu` defined (needed for HPA to calculate utilization %; this is fulfilled by OPS-S13)
  - Target Deployment has `resources.limits.cpu` defined (optional but recommended for QoS)

- [ ] **HPA Behavior:**
  - When pod average CPU exceeds 70%, add a new replica (up to maxReplicas=5)
  - When pod average CPU falls below 70%, scale down (respecting cooldown period of 5 minutes, default Kubernetes behavior)
  - Scale decisions re-evaluated every 15 seconds (default)
  - Verify via: `kubectl get hpa -n port-poc -w` (watch live scaling events)

- [ ] **Donor Knative Special Case:**
  - Donor's Knative Service (`infra/k8s/knative/donor-service-ksvc.yaml`) already has autoscaling via KPA (Knative Pod Autoscaler) with concurrency-based targets
  - This story's HPA applies only to the plain Deployment path (`infra/k8s/donor-service/deployment.yaml`)
  - If `donorKnativeEnabled: true` in Helm values (OPS-S10), the Knative Service is deployed and its KPA takes over; HPA is ignored/not deployed
  - Document this choice explicitly in commit message and/or story notes

- [ ] **Verification (kind cluster):**
  - Apply manifests: `kubectl apply -f infra/k8s/`
  - HPA created: `kubectl get hpa -n port-poc` → shows `recipient-service-hpa` and `donor-service-hpa`
  - Generate load on Recipient service: `kubectl run -i --tty load-generator --rm --image=busybox --restart=Never -- /bin/sh -c "while sleep 0.01; do wget -q -O- http://recipient-service:8080/actuator/health; done"`
  - Monitor scaling: `kubectl get hpa -n port-poc -w` → TARGETS column shows current/desired CPU %, replicas increase as load rises
  - Load test completes, scaling decreases replicas after 5-minute cooldown

- [ ] **Production Considerations (document, not implement in POC):**
  - Consider custom metrics (e.g., RabbitMQ queue depth) instead of just CPU if queue-based scaling is desired
  - Consider Pod Disruption Budgets (PDB) to ensure graceful scale-down
  - Set appropriate `request.memory` for HPA memory-based scaling (optional)

**Dependencies:** OPS-S13 (resource requests must be defined for HPA to calculate utilization), OPS-S10 (Helm chart templates if using them)  
**Blocks:** None

**Notes:**
- Knative Service (if enabled) provides more sophisticated autoscaling (scale-to-zero, concurrency-based) — HPA is the "traditional Kubernetes" alternative for the plain Deployment path.
- CPU-utilization-based scaling is simple and works well for CPU-bound workloads; queue-depth scaling is more appropriate for message-driven workloads but requires custom metrics.

---

### OPS-S13: Right-Size and Document Resource Requests/Limits

**Epic:** OPS-E3  
**Wave:** 3  
**Story Type:** Story  
**Story Points:** 5

**User Story:**
As a platform engineer, I want resource requests and limits defined for all containers so that Kubernetes can make scheduling decisions and prevent runaway memory/CPU usage.

**Acceptance Criteria:**
- [ ] **Add Resources to Existing Manifests (infra/k8s/):**
  - Recipient Service Deployment (`infra/k8s/recipient-service/deployment.yaml`):
    ```yaml
    resources:
      requests:
        cpu: 250m
        memory: 256Mi
      limits:
        cpu: 500m
        memory: 512Mi
    ```
  
  - Donor Service Deployment (`infra/k8s/donor-service/deployment.yaml`):
    ```yaml
    resources:
      requests:
        cpu: 100m
        memory: 128Mi
      limits:
        cpu: 300m
        memory: 256Mi
    ```
    (Donor is stateless and simpler, so lower footprint than Recipient)
  
  - UI Deployment (`infra/k8s/ui/deployment.yaml`):
    ```yaml
    resources:
      requests:
        cpu: 50m
        memory: 64Mi
      limits:
        cpu: 200m
        memory: 128Mi
    ```
    (Static nginx, minimal footprint)
  
  - Support services (MySQL, RabbitMQ, MinIO, Prometheus, Grafana in `infra/k8s/support/` or Helm subchart):
    - MySQL: `requests: { cpu: 500m, memory: 512Mi }`, `limits: { cpu: 1000m, memory: 1Gi }`
    - RabbitMQ: `requests: { cpu: 200m, memory: 256Mi }`, `limits: { cpu: 500m, memory: 512Mi }`
    - MinIO: `requests: { cpu: 200m, memory: 256Mi }`, `limits: { cpu: 500m, memory: 512Mi }`
    - Prometheus: `requests: { cpu: 100m, memory: 256Mi }`, `limits: { cpu: 500m, memory: 512Mi }`
    - Grafana: `requests: { cpu: 100m, memory: 128Mi }`, `limits: { cpu: 300m, memory: 256Mi }`

- [ ] **Helm Chart values.yaml (OPS-S10):**
  - Add resource block to each service's values section:
    ```yaml
    recipient:
      resources:
        requests:
          cpu: 250m
          memory: 256Mi
        limits:
          cpu: 500m
          memory: 512Mi
    ```
  - Same for donor, ui, mysql, rabbitmq, minio, prometheus, grafana
  - Allow override via `values-cloud.yaml` or per-deployment `--set` flag if higher resources needed

- [ ] **Cost Rationale Documentation (new file: infra/k8s/RESOURCE_SIZING.md or infra/RESOURCE_SIZING.md):**
  - Document the reasoning for each service's resource allocation:
    ```markdown
    # Resource Sizing Rationale
    
    ## Recipient Service
    - **Requests:** CPU 250m (quorum of host CPU for JVM startup + GC overhead), Memory 256Mi (Java heap ~200Mi + JVM overhead)
    - **Limits:** CPU 500m (2x request, allows burst), Memory 512Mi (double request, safety margin for heap)
    - **Rationale:** Recipient owns state (MySQL, MinIO) and REST API traffic; moderate CPU/memory footprint for Spring Boot JPA stack
    
    ## Donor Service
    - **Requests:** CPU 100m (minimal, stateless message consumer), Memory 128Mi (JVM heap ~100Mi)
    - **Limits:** CPU 300m, Memory 256Mi
    - **Rationale:** Stateless consumer, lightweight; lower than Recipient
    
    ## UI (nginx)
    - **Requests:** CPU 50m, Memory 64Mi
    - **Limits:** CPU 200m, Memory 128Mi
    - **Rationale:** Static file server, minimal footprint; nginx is lightweight
    
    ## MySQL
    - **Requests:** CPU 500m, Memory 512Mi
    - **Limits:** CPU 1000m, Memory 1Gi
    - **Rationale:** Database server, memory-heavy for buffer pool and caching; needs dedicated CPU for query processing
    
    ## RabbitMQ
    - **Requests:** CPU 200m, Memory 256Mi
    - **Limits:** CPU 500m, Memory 512Mi
    - **Rationale:** Message broker, moderate memory for queue buffers; CPU for message routing
    
    ## Prometheus
    - **Requests:** CPU 100m, Memory 256Mi
    - **Limits:** CPU 500m, Memory 512Mi
    - **Rationale:** Time-series database, memory-heavy for in-memory data structures; minimal CPU needed for POC scrape intervals
    
    ## Notes
    - All values are POC defaults (development cluster on laptop or small cloud instance)
    - Production deployments should profile actual usage and adjust requests/limits accordingly
    - "Requests" are what Kubernetes reserves for scheduling; "Limits" prevent runaway usage
    - If QoS class matters (Guaranteed, Burstable, BestEffort), keep requests == limits for Guaranteed QoS
    ```

- [ ] **Verification (kind cluster or small cloud cluster):**
  - Deploy with resources defined: `kubectl apply -f infra/k8s/` or `helm install`
  - Check pod resource allocation: `kubectl get pods -n port-poc -o wide` → all pods Running
  - Check node resource summary: `kubectl top nodes` and `kubectl top pods -n port-poc` → resources reported (requires metrics-server)
  - Verify no OOMKill events: `kubectl describe pod <pod-name> -n port-poc` → Events section shows no "OOMKilled"
  - Run basic load test and monitor: `kubectl top pods -n port-poc -w` → see CPU/memory usage under limits

- [ ] **Update Kubernetes Manifests & Helm:**
  - All YAML files in `infra/k8s/*/deployment.yaml` include `resources` blocks
  - Helm templates in `infra/helm/port-poc/templates/*/deployment.yaml` templated with `{{ .Values.<service>.resources }}`
  - `values.yaml`, `values-local.yaml`, `values-cloud.yaml` all include complete resource specs

**Dependencies:** OPS-S7 (Sprint 1, existing deployments), OPS-S10 (Helm chart)  
**Blocks:** OPS-S12 (HPA needs requests to calculate utilization percentages)

**Notes:**
- Resource sizing is empirical; these POC defaults are educated guesses based on typical Spring Boot/JVM footprints and should be profiled in a real deployment.
- "Requests" prevent over-subscription; "Limits" prevent one pod from starving others.
- Documenting rationale (not just numbers) helps future maintainers understand trade-offs.

---

# SPRINT 2 SUMMARY & DEPENDENCIES

## Wave-by-Wave Dependency Graph

```
WAVE 1 (Production Security & Reliability — Depends on Sprint 1 completion)
├── DEV-S14: Require API Key on REST Endpoints (← DEV-S11)
├── DEV-S15: Donor Requires Explicit Approval (← DEV-S8)
└── OPS-S10: Package Kubernetes Manifests as Helm Chart (← OPS-S7)

WAVE 2 (Fault Tolerance & Notifications — Depends on WAVE 1)
├── DEV-S16: Failed Message Consumption Goes to DLQ (← DEV-S7, DEV-S8, DEV-S14)
├── DEV-S17: Recipient Sends Notification on Completion (← DEV-S11)
└── OPS-S11: Expose Prometheus Metrics with Grafana Dashboard (← OPS-S1, existing Actuator)

WAVE 3 (Scaling & Sizing — Depends on WAVE 2 + OPS-S10)
├── OPS-S13: Right-Size Resource Requests/Limits (← OPS-S7)
└── OPS-S12: Recipient and Donor Scale Horizontally via HPA (← OPS-S13, OPS-S10)
```

## Role-Based Story Counts

| Role | Epic Count | Story Count | Story Points |
|------|-----------|------------|--------------|
| **Dev** | 1 | 4 | 18 |
| **DevOps** | 1 | 4 | 20 |
| **TOTAL** | 2 | 8 | 38 |

## Recommended Sprint Velocity & Duration

- **Small Team (1-2 per role):** 10–15 points/week → Sprint 2 ~3 weeks
- **Medium Team (2-3 per role):** 15–20 points/week → Sprint 2 ~2 weeks
- **Large Team (3+ per role):** 20–30 points/week → Sprint 2 ~1.5 weeks

**Recommendation:** Plan for 2–3 weeks; execute in parallel across Dev and DevOps tracks.

## Critical Path

Sprint 2 has two independent tracks:

- **Dev Track:** DEV-S14 → DEV-S15, DEV-S16, DEV-S17 (all need Sprint 1 to be complete)
- **DevOps Track:** OPS-S10 → OPS-S13 → OPS-S12; OPS-S11 can run in parallel with OPS-S13

Dev track completes faster (3 stories after DEV-S14); DevOps track takes longer due to HPA/OPS-S12 dependency chain.

---

# SPRINT 2 PLANNING NOTES

## Assumption: Sprint 1 Complete

This document assumes all Sprint-1 stories (`DEV-S1` through `DEV-S13`, `OPS-S1` through `OPS-S9`) have shipped, tested, and been merged to main. If Sprint 1 is still in progress:

- **Dev-blocking items** (DEV-S14/15/16/17) cannot start until their Sprint-1 blockers merge
- **DevOps track** (OPS-S10/11/12/13) is largely independent and can proceed in parallel with Sprint-1 completion, except where it references Sprint-1 deliverables (Kubernetes manifests, actuator endpoints)

## Future Enhancements (Out of Scope)

- Real-time notifications (WebSocket/SSE instead of webhook)
- DLQ replay automation (currently manual)
- Custom metrics for RabbitMQ queue-depth-based scaling
- Pod Disruption Budgets, Network Policies, resource quotas
- Helm subchart dependencies (e.g., Bitnami MySQL, RabbitMQ Helm charts instead of support manifests)
- Helm chart publication to Artifactory/ChartMuseum
- Multi-environment Helm overlays/kustomize integration

---
