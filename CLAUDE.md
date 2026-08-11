# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A POC monorepo proving a port-in/port-out portability flow: a Recipient submits a request through an Angular UI, a Donor service auto-accepts it over RabbitMQ (no manual review), and n8n routes the Donor's confirmation back to the Recipient with zero custom callback code in either service. See `docs/project-concept.md`, `docs/vision-statement.md`, `docs/core-requirements.md`, and `docs/architecture-documentation.md` for the full planning context — read `architecture-documentation.md` before touching cross-service behavior, it defines the layering and integration contracts below.

Work is tracked as Jira epics/stories (project `KAN`, see `docs/sprint-1-epics-stories.md` for the full breakdown) — epics exist as real Jira issues, but individual stories generally only exist as text inside the epic description, not as separate linked issues.

**Current state**: infrastructure and scaffolding are complete (all four components build, containerize, and deploy); the actual domain/business logic (`PortRequest` entity, REST endpoints, RabbitMQ producer/consumer, MinIO adapter, n8n workflow) has not been implemented yet — `RecipientServiceApplication`/`DonorServiceApplication` are empty Spring Boot shells and the Angular app is the default `ng new` scaffold.

## Repository layout

| Directory | What it is |
|---|---|
| `/ui` | Angular 20 UI (standalone components, strict TS) |
| `/recipient-service` | Spring Boot 4.1.0 / Java 21 — owns all `PortRequest` state, MySQL persistence, MinIO receipts, exposes the REST API |
| `/donor-service` | Spring Boot 4.1.0 / Java 21 — stateless, consumes RabbitMQ events and auto-accepts, no database |
| `/infra` | Docker Compose stack, Kubernetes/Knative manifests, connectivity/push scripts |
| `/docs` | Planning docs (vision, requirements, tech stack, architecture, sprint breakdown) |

## Architecture (non-obvious parts)

- **Recipient and Donor never call each other directly.** All communication is `Recipient → RabbitMQ (port.request.initiated) → Donor → RabbitMQ (port.request.accepted) → n8n → REST callback → Recipient`. Don't introduce direct HTTP calls between the two services.
- **Domain layer must stay framework-free** in both services — no JPA/Spring annotations on domain entities (`PortRequest`, etc.). JPA mapping, RabbitMQ adapters, and the MinIO client belong in an infrastructure/adapter layer that implements domain-owned interfaces. This is a hard architectural rule from `docs/architecture-documentation.md`, not a suggestion.
- **Donor is intentionally minimal**: no `@Repository`/`@Service` persistence layer, no manual-review branch — "receive, auto-accept, publish" is the whole job. Resist adding conditional approval logic.
- **No authentication anywhere** — deliberate scope exclusion (`docs/core-requirements.md`), not something to "fix".
- **RabbitMQ consumer failures are logged, not retried** — no dead-lettering. Also deliberate.
- **Every request carries a correlation ID** from submission through the RabbitMQ messages and the n8n callback, for end-to-end log tracing.
- Interface contracts (exact REST/AMQP shapes) are enumerated in `docs/architecture-documentation.md` — implement endpoints to match those exactly rather than improvising the shape.

## Environment-driven config (`local` vs `cloud` profiles)

Both Spring services and the Angular UI must run as the **same built artifact/image** in both Docker Compose and Kubernetes — no hardcoded hostnames, no rebuilds per environment.

- Spring: `SPRING_PROFILES_ACTIVE=local|cloud` selects `application-local.yml` / `application-cloud.yml`. Defaults in these files assume running the jar directly on the host machine (`localhost` + Compose's host-mapped ports); `/infra/docker-compose.yml` and the Kubernetes manifests override the same variables (`MYSQL_HOST`, `RABBITMQ_HOST`, `MINIO_HOST`, etc.) to in-network service DNS names when the service itself runs inside that network.
- Angular: the API URL is **not** baked in at compile time via `environment.ts` (a single built image must work against different backends). Instead `public/env.js` sets `window.__env.apiUrl` at runtime, and `docker-entrypoint.sh` regenerates that file from the `API_URL` container env var at container start, before nginx serves it. `environment.ts` just reads `window.__env` with a localhost fallback; `environment.development.ts` (used only by `ng serve`) is static.
- Recipient Service tests are **hermetic** — `src/test/resources/application-local.yml` shadows the main `application-local.yml` on the test classpath to force an in-memory H2 database, so `mvn test` never depends on a live MySQL container (needed for CI, where no Docker Compose stack is running). Donor Service needs no such override: its RabbitMQ `ConnectionFactory` connects lazily, so `mvn test` passes without a live broker as long as no `@RabbitListener` requires one at context startup.

## Commands

### Recipient / Donor Service (Spring Boot, Java 21, Maven)
Run from `recipient-service/` or `donor-service/`:
```sh
./mvnw clean install          # build + run tests
./mvnw test                   # tests only
./mvnw test -Dtest=ClassName  # single test class
./mvnw spring-boot:run        # run locally (defaults to `local` profile)
```
Recipient listens on 8080, Donor on 8081. `/actuator/health` is exposed on both (`management.endpoint.health.probes.enabled: true`, so `/actuator/health/liveness` and `/actuator/health/readiness` exist too — required by the Kubernetes probes in `infra/k8s/`).

### UI (Angular 20)
Run from `ui/`:
```sh
npm ci
ng serve                                          # dev server, localhost:4200
ng build                                          # production build -> dist/ui/browser
ng test --watch=false --browsers=ChromeHeadless   # unit tests (Karma/Jasmine), CI mode
ng lint                                           # ESLint via angular-eslint
```

### Full local stack (Docker Compose)
Run from `infra/`:
```sh
docker compose --env-file .env.local up -d   # RabbitMQ, MySQL, MinIO, n8n + all 3 app services
./verify-connectivity.sh                     # exits non-zero on first failed check
```
See `infra/CONNECTIVITY_CHECK.md` for the manual checklist and `infra/README.md` for details. Notable: MySQL is mapped to **host port 3307**, not 3306 (3306 is commonly taken by a local MySQL install) — containers still reach it at `mysql:3306` internally. nginx (UI image) healthchecks must target `127.0.0.1`, not `localhost` — the container has no IPv6 listener and `localhost` resolves to `::1` first, which reads as a false failure.

### Building/pushing images
```sh
cd infra/scripts
REGISTRY=<your-registry> ./push-images.sh          # build + tag only (default)
REGISTRY=<your-registry> PUSH=1 ./push-images.sh    # also push (requires prior `docker login`)
```
Registry-agnostic by design — nothing is hardcoded to a specific provider.

### Kubernetes / Knative
Plain manifests in `infra/k8s/` (`namespace.yaml`, `configmap.yaml`, `secret.yaml`, per-service `deployment.yaml`/`service.yaml`); `infra/k8s/knative/donor-service-ksvc.yaml` deploys Donor as a Knative `Service` instead (demonstrates scale-to-zero) — it replaces, rather than supplements, Donor's plain Deployment/Service. `infra/k8s/support/` holds throwaway MySQL/RabbitMQ Deployments needed only to give the app manifests something real to connect to when verifying on a local cluster (not meant for actual production use).

When testing against `kind`: images built locally aren't in any registry, so `imagePullPolicy: IfNotPresent` is required on every container (Kubernetes defaults `:latest` to `Always`, which would try — and fail — a real pull). Knative additionally resolves tags to digests via a registry call by default even for cluster-local images; tag images `dev.local/...` and load with `kind load docker-image` — `dev.local` is on `config-deployment`'s `registries-skipping-tag-resolving` list in `knative-serving`, which skips that lookup.

### CI
`.github/workflows/build.yml` runs three parallel jobs (Recipient `mvn test`, Donor `mvn test`, UI `ng build && ng test`) on push/PR to `main`. It `chmod +x mvnw` before invoking it, since the wrapper's executable bit doesn't reliably survive a Windows checkout.
