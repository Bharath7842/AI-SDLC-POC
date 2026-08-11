# /infra/k8s/support

Minimal, ephemeral MySQL and RabbitMQ Deployments — **not** part of the OPS-S7 application manifests. They exist purely so `recipient-service`/`donor-service` have something real to connect to when verifying the Kubernetes manifests on a local `kind` cluster (mirroring what the Docker Compose stack in OPS-S1 provides locally). No persistent volumes, no HA, not meant for anything beyond throwaway verification.

A real deployment target (on-prem cluster, managed cloud Kubernetes) would instead point `MYSQL_HOST`/`RABBITMQ_HOST` at managed services or a properly operated in-cluster deployment (e.g. a MySQL/RabbitMQ Helm chart with persistence) — that's out of scope here.
