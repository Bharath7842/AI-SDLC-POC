# Kubernetes Resource Sizing Rationale

This document explains the resource `requests` and `limits` configured for all services in the Port-POC deployment.

## Sizing Philosophy

- **Requests**: Reserved resources for scheduling. Kubernetes reserves these resources on the node when the pod starts.
- **Limits**: Hard caps to prevent runaway resource consumption. Pods exceeding memory limits are OOMKilled; CPU limits are throttled.

All values are **POC defaults** and should be profiled in production environments.

## Application Services

### Recipient Service
- **Requests**: CPU 250m, Memory 256Mi
- **Limits**: CPU 500m, Memory 512Mi
- **Rationale**: 
  - Recipient owns state (MySQL, MinIO interactions, REST API layer).
  - Spring Boot JPA stack needs ~200Mi heap + JVM overhead (~256Mi total).
  - CPU request (250m) allows startup and GC operations; limit (500m) allows 2x burst for processing traffic spikes.

### Donor Service
- **Requests**: CPU 100m, Memory 128Mi
- **Limits**: CPU 300m, Memory 256Mi
- **Rationale**:
  - Stateless consumer; simpler than Recipient.
  - JVM heap ~100Mi + overhead.
  - CPU request (100m) sufficient for message consumption; limit (300m) allows traffic handling.

### UI (nginx)
- **Requests**: CPU 50m, Memory 64Mi
- **Limits**: CPU 200m, Memory 128Mi
- **Rationale**:
  - Static file server (no business logic).
  - nginx is lightweight; 64Mi memory handles typical request buffers.
  - CPU request (50m) for baseline serving; limit (200m) for burst.

## Infrastructure Services (Support Stack)

These services are typically deployed in `infra/k8s/support/` (for test clusters) or managed externally in production.

### MySQL
- **Requests**: CPU 500m, Memory 512Mi
- **Limits**: CPU 1000m, Memory 1Gi
- **Rationale**:
  - Database server; memory-intensive for buffer pool and query cache.
  - CPU request (500m) for query processing and replication threads.
  - Limit (1Gi) ensures sufficient headroom for indexes and temporary tables.

### RabbitMQ
- **Requests**: CPU 200m, Memory 256Mi
- **Limits**: CPU 500m, Memory 512Mi
- **Rationale**:
  - Message broker; moderate memory for queue buffers and connection state.
  - CPU request (200m) for message routing and acknowledgment processing.

### Prometheus
- **Requests**: CPU 100m, Memory 256Mi
- **Limits**: CPU 500m, Memory 512Mi
- **Rationale**:
  - Time-series database; memory-intensive for in-memory WAL and block caches.
  - POC scrape intervals (15s) don't require high CPU; limit (500m) allows burst during compaction.

### Grafana
- **Requests**: CPU 100m, Memory 128Mi
- **Limits**: CPU 300m, Memory 256Mi
- **Rationale**:
  - Lightweight dashboard UI; minimal memory footprint.
  - CPU request (100m) sufficient for rendering; limit (300m) for large dashboards.

## Scheduling & QoS Class

Current setup uses **Burstable QoS** (requests < limits). In production:
- **Guaranteed QoS** (requests == limits): Use for critical services; prevents eviction under node pressure.
- **Burstable QoS** (requests < limits): Current approach; allows overcommit but pods evicted first under pressure.
- **BestEffort QoS** (no requests/limits): Not recommended for production.

## Monitoring & Adjustment

Monitor actual usage in your environment:
```bash
# Watch real-time resource usage
kubectl top pods -n port-poc -w

# Check node resource availability
kubectl top nodes
```

If pods consistently hit limits, increase the limit value. If requests are much higher than actual usage, decrease to improve scheduling density.

## Production Considerations

1. **Profile your workload**: These defaults are educated guesses. Real production sizing requires profiling.
2. **Add HPA**: Use HorizontalPodAutoscaler (OPS-S12) to scale replicas based on CPU utilization.
3. **Add PDB**: Pod Disruption Budgets ensure graceful scale-down.
4. **Add Network Policy**: Restrict traffic between pods.
5. **Add Resource Quotas**: Namespace-level caps on total resource consumption.
6. **Use Specific Image Tags**: Replace `:latest` with semantic versions (e.g., `v1.0.0`) to avoid surprises on pod restart.

---

Last updated: 2026-08-11
