# Project Vision Statement

## Purpose

To prove — cheaply, quickly, and without domain-specific noise — that an event-driven, two-party "portability" workflow can be built on async messaging and no-code orchestration alone. The POC demonstrates a Recipient requesting something, a Donor releasing it, and ownership moving from one system to another, entirely automatically. It answers the underlying architectural questions (reliable async communication between independent services, orchestration without hardcoded logic, identical containers from laptop to cloud) before any real-world portability domain — number porting, policy switching, subscription transfer — adds its regulatory and procedural complexity on top.

## Target Users

Engineering and architecture teams who need to validate an integration pattern before committing to a production build. This includes technical stakeholders evaluating whether async messaging + no-code orchestration is a viable foundation for future portability-style products (telecom number porting, insurance policy switching, subscription/account transfers, etc.).

## Value Proposition

Unlike a full production build, this POC deliberately strips out authentication, manual review, SLA timers, billing, and multi-tenancy so the team can get a clean yes/no on the plumbing itself:

- Two independently owned services (Recipient, Donor) communicate only through RabbitMQ — proving true decoupling.
- n8n mediates the Donor-to-Recipient callback with no custom code, proving a no-code orchestration layer can carry real business logic.
- The same Docker Compose stack runs unmodified on Kubernetes/Knative, proving cloud-agnostic, scale-to-zero economics.

Because the domain complexity is deferred to a documented "future enhancements" list rather than left as an unknown gap, the POC gives a fast, low-cost validation of the architecture before anyone invests in the harder domain-specific work.

## Key Features

- Port request submission through a simple Angular UI
- Automatic, event-driven Donor acceptance (no manual approval step)
- No-code orchestration (n8n) that routes the Donor's confirmation back to the Recipient
- Live status dashboard reflecting request progress through to `COMPLETED`
- Confirmation receipt generated and retrievable from object storage
- Fully containerized deployment, portable from local Docker Compose to any CNCF-conformant cloud cluster
