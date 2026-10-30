# PrecisionAuto Care – Fleet Maintenance & Service Operations Platform

```
                       ┌────────────────┐
   client / tech ────► │  API Gateway   │ :8080  (JWT check, lb:// routing)
                       └───────┬────────┘
       Eureka Server :8761 ◄───┤ discovery + client-side load balancing
        ┌──────────────┬───────┴───────┬───────────────┐
   auth-service   booking-service  record-service   billing-service
   /auth/**       /bookings/**  ─►  /records/**   ─►  /invoices/**
   users, JWT     bay scheduling     service history    invoice generation
```

| Module | Port (local) | Responsibility |
|---|---|---|
| eureka-server | 8761 | Service registry |
| api-gateway | 8080 | Single entry, JWT validation, `lb://` routes |
| auth-service | 8081 | Register / login, issues JWT (roles `CLIENT`, `TECHNICIAN`) |
| booking-service | 8082 | Appointments, bay-collision prevention, completion pipeline |
| record-service | 8083 | Vehicle service history (idempotent per appointment) |
| billing-service | 8084 | Invoice generation (labour + parts + tax), payment status |
| common-security | – | Shared JWT service/filter + load-balanced RestClient that forwards the token |

## Key design points
* **JWT**: HS256, issued by auth-service. Validated at the gateway *and* in each service (defence in depth); role checks via `@PreAuthorize`.
* **No booking collisions**: overlap query (`existing.start < new.end AND existing.end > new.start`) inside a `SERIALIZABLE` transaction, so concurrent requests on different service instances cannot both win; the loser gets `409`. Back-to-back slots are allowed; cancelled slots free the bay.
* **Booking → Record → Billing**: `POST /bookings/{id}/complete` (technician) → booking calls record-service → record persists history, then calls billing-service → invoice returned back up the chain and stored on the appointment.
* **Idempotency / failure handling**: record is unique per `appointmentId`, invoice unique per `recordId`. If billing is down the history is still saved and the appointment stays `SCHEDULED`; retrying the same call only re-attempts the missing step.
* **Load balancing**: every service registers in Eureka with a unique instance id; gateway routes use `lb://name`, service-to-service calls use a `@LoadBalanced` RestClient (round-robin).

## Run locally (no Docker) – Java 17 + Maven
```bash
mvn clean install                      # builds + runs all tests
# start in this order, each in its own terminal
mvn -pl eureka-server spring-boot:run
mvn -pl auth-service spring-boot:run
mvn -pl billing-service spring-boot:run
mvn -pl record-service spring-boot:run
mvn -pl booking-service spring-boot:run
mvn -pl api-gateway spring-boot:run
```
(H2 in-memory DBs locally.)

## Deploy with Docker Compose (PostgreSQL, 2× booking, 2× record)
```bash
export JWT_SECRET='use-a-long-random-secret-of-32+bytes'
docker compose up --build
docker compose up -d --scale booking-service=3      # scale out
```
Eureka dashboard: http://localhost:8761 – API: http://localhost:8080

## Try it
```bash
GW=http://localhost:8080
# demo users: client1 / Client@12345   tech1 / Tech@12345
CT=$(curl -s $GW/auth/login -H 'Content-Type: application/json' -d '{"username":"client1","password":"Client@12345"}' | jq -r .token)
TT=$(curl -s $GW/auth/login -H 'Content-Type: application/json' -d '{"username":"tech1","password":"Tech@12345"}'   | jq -r .token)

# 1. client books bay 2
curl -s $GW/bookings -H "Authorization: Bearer $CT" -H 'Content-Type: application/json' -d '{
  "vehicleReg":"KA01AB1234","customerName":"Alice","serviceType":"General service",
  "bayNumber":2,"startTime":"2030-01-15T09:00:00","durationMinutes":120}'
# 2. same bay, overlapping -> 409
curl -i $GW/bookings -H "Authorization: Bearer $CT" -H 'Content-Type: application/json' -d '{
  "vehicleReg":"KA02XX0001","customerName":"Bob","serviceType":"Oil change",
  "bayNumber":2,"startTime":"2030-01-15T10:00:00","durationMinutes":60}'
# 3. free bays for a window
curl -s "$GW/bookings/availability?start=2030-01-15T09:30:00&end=2030-01-15T10:30:00" -H "Authorization: Bearer $CT"
# 4. technician completes job -> record + invoice created
curl -s $GW/bookings/1/complete -H "Authorization: Bearer $TT" -H 'Content-Type: application/json' -d '{
  "workDescription":"Oil, filter, plugs","laborHours":2,
  "parts":[{"name":"Oil filter","quantity":1,"unitPrice":500},{"name":"Spark plug","quantity":2,"unitPrice":250}]}'
# 5. history + invoice
curl -s $GW/records/vehicle/KA01AB1234 -H "Authorization: Bearer $CT"
curl -s $GW/invoices/record/1           -H "Authorization: Bearer $CT"
```
Technician self-registration requires the invite code (`TECH_INVITE_CODE`, default `GARAGE-STAFF-2026`). Change the defaults (JWT secret, invite code, DB password) and set `auth.seed-demo-users=false` for production.

## Tests
`mvn test` – unit (Mockito: booking rules, record idempotency, invoice maths), repository (`@DataJpaTest` overlap logic), and MockMvc integration tests with real JWTs (auth flow, 401/403, 409 collision, completion pipeline, idempotent invoicing).

## API summary
| Method & path | Role | Purpose |
|---|---|---|
| POST /auth/register, /auth/login | public | Account + JWT |
| POST /bookings | any | Schedule a slot (409 on collision) |
| GET /bookings, /bookings/{id}, /bookings/availability | any | List / view / free bays |
| PATCH /bookings/{id}/cancel | owner/tech | Cancel |
| POST /bookings/{id}/complete | TECHNICIAN | Triggers Record → Billing |
| GET /records/vehicle/{reg}, /records/{id} | any | Maintenance history |
| POST /records | TECHNICIAN | Create record (called by booking) |
| GET /invoices/{id}, /invoices/record/{recordId} | any | View invoice |
| POST /invoices, PATCH /invoices/{id}/pay | TECHNICIAN | Generate / mark paid |
