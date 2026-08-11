# AI SDLC POC — Port Request Portability

Monorepo for the port-in/port-out portability POC. Four top-level directories:

| Directory | Purpose |
|---|---|
| [`/ui`](ui/) | Angular UI — submission form and status dashboard |
| [`/recipient-service`](recipient-service/) | Spring Boot service that owns port-request state (MySQL, MinIO) and publishes to RabbitMQ |
| [`/donor-service`](donor-service/) | Spring Boot service that consumes port-request events and auto-accepts them (stateless) |
| [`/infra`](infra/) | Local/cloud infrastructure — Docker Compose, Kubernetes manifests |

## Planning documents

- [Vision statement](docs/vision-statement.md)
- [Core requirements](docs/core-requirements.md)
- [Technology stack](docs/technology-stack.md)
- [Architecture documentation](docs/architecture-documentation.md)
