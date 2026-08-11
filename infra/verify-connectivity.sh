#!/bin/sh
# End-to-end local connectivity check for the port-poc Docker Compose stack.
# Run after `docker compose --env-file .env.local up -d` from /infra.
# Exits 0 if every check passes, non-zero on the first failure.
set -eu

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

pass() {
  echo "OK:   $1"
}

# 1. Recipient Service health
status=$(curl -sf http://localhost:8080/actuator/health | grep -o '"status":"UP"' || true)
[ -n "$status" ] || fail "recipient-service /actuator/health did not report UP"
pass "recipient-service /actuator/health UP"

# 2. Recipient Service MySQL connection
db_status=$(curl -sf http://localhost:8080/actuator/health | grep -o '"database":"MySQL"' || true)
[ -n "$db_status" ] || fail "recipient-service is not connected to MySQL"
pass "recipient-service connected to MySQL"

# 3. Donor Service health
status=$(curl -sf http://localhost:8081/actuator/health | grep -o '"status":"UP"' || true)
[ -n "$status" ] || fail "donor-service /actuator/health did not report UP"
pass "donor-service /actuator/health UP"

# 4. Donor Service RabbitMQ connection
rabbit_status=$(curl -sf http://localhost:8081/actuator/health | grep -o '"rabbit":{"details":{"version":"[^"]*"},"status":"UP"}' || true)
[ -n "$rabbit_status" ] || fail "donor-service is not connected to RabbitMQ"
pass "donor-service connected to RabbitMQ"

# 5. RabbitMQ management API reachable
curl -sf -u guest:guest http://localhost:15672/api/overview >/dev/null \
  || fail "RabbitMQ management API not reachable on :15672"
pass "RabbitMQ management API reachable"

# 6. MySQL reachable and port_requests schema exists
docker exec mysql mysql -uroot -prootpassword -e "USE port_requests; SHOW TABLES;" >/dev/null 2>&1 \
  || fail "MySQL not reachable or port_requests schema missing"
pass "MySQL reachable, port_requests schema exists"

# 7. MinIO port-requests bucket exists
docker exec minio-init sh -c "true" >/dev/null 2>&1 || true # minio-init is one-shot, ignore its own state
docker run --rm --network port-poc_port-poc --entrypoint sh minio/mc:latest -c \
  "mc alias set local http://minio:9000 minioadmin minioadmin >/dev/null && mc ls local/port-requests >/dev/null" \
  || fail "MinIO port-requests bucket not reachable"
pass "MinIO port-requests bucket exists"

# 8. UI reachable
curl -sf http://localhost:4200/ >/dev/null || fail "UI not reachable on :4200"
pass "UI reachable"

# 9. n8n reachable
curl -sf http://localhost:5678/healthz >/dev/null || fail "n8n not reachable on :5678"
pass "n8n reachable"

echo
echo "All connectivity checks passed."
