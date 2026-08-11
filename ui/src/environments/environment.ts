// Production build. apiUrl is resolved at container *runtime* (see
// public/env.js and ui/docker-entrypoint.sh), not at compile time — the
// same built image must work unmodified against both the local Docker
// Compose stack and a Kubernetes cluster, which have different hostnames.
export const environment = {
  apiUrl: (window as any).__env?.apiUrl ?? 'http://localhost:8080',
};
