# Local Connectivity Checklist

Run after `docker compose --env-file .env.local up -d` from `/infra`. Automated in [`verify-connectivity.sh`](verify-connectivity.sh); this is the same checklist in human-readable form.

| # | Check | How to verify manually |
|---|---|---|
| 1 | Recipient Service starts and reports healthy | `curl http://localhost:8080/actuator/health` → `"status":"UP"` |
| 2 | Recipient Service connects to MySQL | Same response includes `"db":{"details":{"database":"MySQL"},"status":"UP"}` |
| 3 | Donor Service starts and reports healthy | `curl http://localhost:8081/actuator/health` → `"status":"UP"` |
| 4 | Donor Service connects to RabbitMQ | Same response includes `"rabbit":{"status":"UP"}` |
| 5 | RabbitMQ management UI reachable | Open `http://localhost:15672` (guest/guest) |
| 6 | MySQL reachable, `port_requests` schema exists | `docker exec mysql mysql -uroot -prootpassword -e "USE port_requests; SHOW TABLES;"` |
| 7 | MinIO `port-requests` bucket exists | Open `http://localhost:9001` (minioadmin/minioadmin) console, or `mc ls local/port-requests` |
| 8 | UI reachable | Open `http://localhost:4200` |
| 9 | n8n reachable | Open `http://localhost:5678` |

Run the automated version:

```sh
cd infra
./verify-connectivity.sh
```
