# /infra

Local and cloud infrastructure for the port-poc stack: Docker Compose, Kubernetes/Knative manifests, environment profiles, and operational scripts.

## Local development

```sh
cd infra
docker compose --env-file .env.local up -d
./verify-connectivity.sh
```

This starts the full stack: RabbitMQ, MySQL, MinIO, n8n, and all three application services (`recipient-service` on :8080, `donor-service` on :8081, `ui` on :4200). See [CONNECTIVITY_CHECK.md](CONNECTIVITY_CHECK.md) for the manual checklist version.

Note: host port 3306 is commonly taken by a local MySQL install, so this stack maps MySQL to host port **3307** instead (containers still reach it at `mysql:3306` internally).

## Building & pushing images

```sh
cd infra/scripts
REGISTRY=<your-registry> ./push-images.sh          # build + tag only
REGISTRY=<your-registry> PUSH=1 ./push-images.sh    # build + tag + push
```

`push-images.sh` is registry-agnostic — it doesn't assume Docker Hub, GHCR, or any specific provider. Run `docker login <registry>` yourself first if you intend to push. Images are tagged with both the current commit SHA and `latest`.

## Kubernetes / Knative

See [`k8s/`](k8s/) for plain Kubernetes manifests and [`k8s/knative/`](k8s/knative/) for the Knative Service demonstrating scale-to-zero. Both were verified against a local `kind` cluster.
