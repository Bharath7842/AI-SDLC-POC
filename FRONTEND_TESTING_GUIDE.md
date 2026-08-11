# Frontend Testing Guide

## Quick Start

### 1. Start the Backend Stack
```bash
cd infra
docker-compose up -d
# Wait for services to be healthy (~30 seconds)
docker-compose logs -f recipient-service
```

Verify backend is ready:
```bash
curl -X GET http://localhost:8080/actuator/health \
  -H "X-API-Key: test-key-local"
# Should return: {"status":"UP"}
```

### 2. Start the Angular Dev Server
```bash
cd ui
npm ci
ng serve --open
```

This automatically opens `http://localhost:4200` in your browser.

## Frontend Application

The UI is a **Port Request submission portal** with:

### Features Implemented

1. **Submit New Request Form**
   - Customer ID input field
   - Submit button (enabled only when customer ID is provided)
   - Error message display on failure

2. **Request Status Display**
   - Shows request ID after submission
   - Displays current status (INITIATED or COMPLETED)
   - Shows creation and completion timestamps
   - Refresh button to poll for updates
   - Clear button to reset the form

3. **API Configuration**
   - Displays configured API URL
   - Shows API Key being used (default: `test-key-local`)

### Component Structure

```
app-root
└── PortRequestComponent
    ├── Form Section (submit request)
    ├── Status Section (view result)
    └── Info Section (configuration)
```

## End-to-End Testing

### Test Scenario 1: Submit & View Request

**Steps:**
1. Enter `customer-123` in the Customer ID field
2. Click "Submit Request"
3. Observe:
   - ✅ Request ID appears in Status section
   - ✅ Status shows "INITIATED"
   - ✅ Created timestamp displayed

**Expected Result:**
- Form submission succeeds with API key validation
- Request stored in MySQL database
- Event published to RabbitMQ (`port.request.initiated`)
- Donor service consumes event and publishes acceptance

**Browser Console (F12):**
- No errors
- Network tab shows: `POST /api/v1/port-requests` → 201 Created

### Test Scenario 2: Refresh Status

**Steps:**
1. After submitting a request, click "Refresh Status"
2. Observe status remains "INITIATED"

**Expected Result:**
- ✅ Status query succeeds with API key validation
- ✅ Shows current state from database
- ✅ No errors in console

### Test Scenario 3: Complete Request (n8n Integration)

**Prerequisites:**
- n8n workflow must be configured (not yet implemented in Sprint 1)

**Manual Completion (Workaround):**
```bash
# Call the complete endpoint directly
curl -X POST http://localhost:8080/api/v1/port-requests/{REQUEST_ID}/complete \
  -H "X-API-Key: test-key-local" \
  -H "Content-Type: application/json" \
  -d '{"receiptUri":"s3://port-requests/test-receipt.txt"}'

# Then refresh the UI to see updated status
```

**Expected Result After Refresh:**
- ✅ Status changes to "COMPLETED"
- ✅ Completed timestamp appears
- ✅ Status badge color changes (green for completed)

### Test Scenario 4: API Key Validation

**Test Missing API Key:**
```bash
# Try without X-API-Key header in browser dev tools
```

**Expected Result:**
- ✅ 401 Unauthorized response
- ✅ Error message displayed in UI: "Error: Unauthorized"

## Advanced Testing

### Monitor RabbitMQ Flow

While testing, watch RabbitMQ management UI to see message flow:

1. Open `http://localhost:15672` (guest/guest)
2. Go to "Queues" tab
3. Watch `port.request.initiated` queue:
   - Messages appear when you submit requests
   - Messages disappear as Donor consumes them
4. Watch `port.request.accepted` queue:
   - Messages appear as Donor publishes acceptances

### Monitor Logs

**Recipient Service:**
```bash
docker-compose logs -f recipient-service | grep -i "port.request.initiated\|submitted"
```

**Donor Service:**
```bash
docker-compose logs -f donor-service | grep -i "accepted\|consuming"
```

**Look for correlation IDs** to trace requests end-to-end:
```
[correlationId=abc-123-def] Published port.request.initiated event
[correlationId=abc-123-def] Received and published port.request.accepted
```

### Check Database State

```bash
# Connect to MySQL
mysql -h 127.0.0.1 -u root -prootpassword -P 3307 port_requests

# Query submitted requests
SELECT id, status, created_at, completed_at, customer_id FROM port_requests;
```

### Check MinIO Storage

```bash
# Open MinIO console
http://localhost:9001 (minioadmin/minioadmin)

# Navigate to port-requests bucket
# You should see receipt files after completing requests
```

## Testing Checklist

### Basic Flow
- [ ] Submit request with valid customer ID
- [ ] Request ID appears immediately
- [ ] Status shows "INITIATED"
- [ ] Refresh shows same status
- [ ] No console errors

### Error Handling
- [ ] Submit empty customer ID → shows error
- [ ] Missing API key → 401 error displayed
- [ ] Invalid customer ID (very long string) → handled gracefully
- [ ] Network disconnect → error message shown

### RabbitMQ Integration
- [ ] Message appears in `port.request.initiated` queue after submission
- [ ] Donor service log shows consumption
- [ ] Message appears in `port.request.accepted` queue
- [ ] Correlation ID traced through all logs

### Database Integrity
- [ ] New request persists in MySQL
- [ ] Status updates persist after completion
- [ ] No duplicate requests created
- [ ] Timestamps are accurate (UTC)

### UI/UX
- [ ] Form validates before submission
- [ ] Button disabled while loading
- [ ] Clear button resets form properly
- [ ] Status colors update correctly
- [ ] Responsive on mobile (try F12 device emulation)

## Troubleshooting

### Issue: "API Key: Not configured"
**Solution:** Set API key in localStorage:
```javascript
// Paste in browser console (F12)
localStorage.setItem('apiKey', 'test-key-local');
location.reload();
```

### Issue: CORS errors in console
**Solution:** This is expected if frontend and backend run on different ports. CORS is not configured in the Spring backend (by design, as stated in CLAUDE.md — authentication is out of scope for Sprint 1). For testing, use the docker-compose setup which puts services on the same internal network.

### Issue: "Network error" when submitting
**Checklist:**
- [ ] Backend services running: `docker-compose ps`
- [ ] Recipient service healthy: `curl http://localhost:8080/actuator/health`
- [ ] API URL correct in browser: check "API Configuration" section
- [ ] API Key matches: should be `test-key-local` for local testing

### Issue: Status never changes from INITIATED
**Expected for now:** n8n workflow integration not yet implemented (DEV-S13 pending). Use the manual curl workaround above to test completion flow.

## Performance Testing

### Load Test (Optional)

**Using Apache Bench (10 requests, 5 concurrent):**
```bash
ab -n 10 -c 5 -H "X-API-Key: test-key-local" -p request.json \
  -T "application/json" http://localhost:8080/api/v1/port-requests
```

Where `request.json`:
```json
{"customerId":"perf-test"}
```

**Expected Result:**
- ~50-100ms per request (depending on hardware)
- No 5xx errors
- Database remains consistent

## Unit Tests (ng test)

Run Angular unit tests:
```bash
ng test --watch=false --browsers=ChromeHeadless
```

Current test coverage:
- ✅ App component renders
- ⏳ PortRequestComponent tests (to be added)

## Next Steps

After testing the current setup, upcoming work:

1. **DEV-S13** (n8n workflow) — Auto-complete requests via messaging
2. **DEV-S15** (Donor approval) — Add approval flow to Recipient UI
3. **DEV-S17** (Notifications) — Alert on status changes
4. **OPS-S11** (Metrics) — Add Prometheus metrics, Grafana dashboard
5. **OPS-S10** (Helm) — Package as Helm chart for cloud deployment

## Useful Commands

| Command | Purpose |
|---------|---------|
| `docker-compose ps` | Check service health |
| `docker-compose logs -f <service>` | Stream service logs |
| `curl http://localhost:8080/actuator/health -H "X-API-Key: test-key-local"` | Test Recipient API |
| `curl http://localhost:8081/actuator/health -H "X-API-Key: test-key-local"` | Test Donor API |
| `ng serve --open` | Start dev server, open browser |
| `ng build` | Production build |
| `ng test` | Run unit tests |
| `ng lint` | Run ESLint |

## Architecture Diagram

```
Browser (Angular UI)
    ↓
    ├─ POST /api/v1/port-requests (with X-API-Key)
    │  ↓
    ├─ Recipient Service (Spring Boot)
    │  ├─ Domain layer (framework-free)
    │  ├─ JPA/MySQL adapter
    │  ├─ RabbitMQ producer
    │  └─ MinIO storage
    │
    ├─ RabbitMQ Broker
    │  ├─ port-exchange (topic)
    │  ├─ port.request.initiated (queue)
    │  └─ port.request.accepted (queue)
    │
    ├─ Donor Service (Spring Boot)
    │  ├─ RabbitMQ consumer
    │  └─ RabbitMQ producer
    │
    └─ n8n (Workflow orchestration)
       └─ HTTP callback to Recipient /complete endpoint
```

---

**Last Updated:** 2026-08-11  
**Frontend Version:** Angular 20 (standalone components)  
**Backend API:** Spring Boot 4.1.0 with Recipient/Donor services
