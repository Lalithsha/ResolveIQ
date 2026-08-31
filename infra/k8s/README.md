# ResolveIQ Kubernetes base

`base/` deploys only ResolveIQ application workloads. PostgreSQL with pgvector,
Kafka, MinIO, and ClamAV are explicit managed dependencies referenced by DNS in
the ConfigMap; the repository does not pretend a single-replica database is a
production deployment.

Before applying this base:

1. Replace the `ghcr.io/replace-me` image namespace or override images in an overlay.
2. Override managed dependency DNS values in `resolveiq-runtime`.
3. Create `resolveiq-runtime-secrets` through the cluster secret manager with
   `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`,
   `RESOLVEIQ_JWT_SECRET`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, and
   `RESOLVEIQ_AI_API_KEY`.
4. Override the example ingress host/TLS secret and tune the included network
   policies, PodDisruptionBudgets, and autoscaling limits for the target cluster.
   The HPA resources require Metrics Server.
5. Add registry pull credentials through the target cluster's secret manager.
6. Validate with `kubectl kustomize infra/k8s/base` before deployment.

No secret value is stored in Git. The production profile intentionally fails
startup when real AI, attachment scanner, storage, or security configuration is
missing.
