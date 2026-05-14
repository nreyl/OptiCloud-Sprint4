# OptiCloud — Sprint 4

Implementation of the OptiCloud Sprint 4 component and deployment architecture:
six microservices behind a Kong API Gateway, with per-service databases
(PostgreSQL, MongoDB, Redis), deployed on AWS via Terraform — one EC2 instance
per service and per datastore, exactly as drawn in the deployment diagram.

## Architecture at a glance

```
client ─► Kong (8000)
            │
            ├─ /auth/*       ─► auth-service          (NestJS) ─► MongoDB
            │                       │                            │
            │                       └─► notification-service ◄───┘
            │                                (FastAPI: incident + email)
            │
            ├─ /ingest/*     ─► data-injestion        (Django)
            │                       │
            │                       ▼
            ├─ /reports/*    ─► reports-service       (Spring Boot, CQRS)
            │                       │
            │                       ├─► PostgreSQL  (writes via CommandService)
            │                       └─► Redis       (cache for QueryService)
            │
            └─ /adapters/*   ─► cloud-adapter         (Django)
                                    │   ├─► PostgreSQL (audit & accounts)
                                    └─► normalization-service (Spring Boot)
                                            │   (JSON Schema Validator)
                                            └─► reports-service (Report Forwarder)
```

## Repository layout

| Path | Contents |
| --- | --- |
| `services/auth-service/`          | NestJS · JWT Strategy · Company Guard · AccessLogWriter · MongoDB |
| `services/notification-service/`  | FastAPI · Security Incident Handler · Email Dispatcher (background task) |
| `services/data-injestion/`        | Django · canonical-payload ingestion endpoint |
| `services/cloud-adapter/`         | Django + Postgres · `BaseCloudAdapter` chassis · `AwsAdapter` |
| `services/normalization-service/` | Spring Boot · JSON Schema validator + provider normalizers · `ReportForwarder` |
| `services/reports-service/`       | Spring Boot · CQRS (`CommandService` / `QueryService`) · Postgres + Redis cache |
| `kong/kong.yaml`                  | Kong DB-less config used by the AWS deployment (placeholders for IPs) |
| `iac/`                            | Terraform scripts that deploy every service + every datastore + Kong in AWS |

## Deploying to AWS (Terraform)

The IaC under `iac/` provisions one EC2 instance per microservice and one
EC2 instance per datastore. Every VM installs Docker, clones this repo,
builds the relevant image and runs it with the right environment variables
(private IPs are wired in by Terraform).

```bash
cd iac
terraform init
terraform apply \
  -var "repository_url=https://github.com/nreyl/OptiCloud-Sprint4.git" \
  -var "repository_branch=main" \
  -var "jwt_secret=$(openssl rand -hex 32)"
```

When `apply` finishes Terraform prints the Kong public IP — that's the entry
point for all four routes (`/auth`, `/ingest`, `/reports`, `/adapters`).

To destroy the deployment afterwards:

```bash
cd iac
terraform destroy
```

> **Note for AWS Academy LabRole**: the Terraform uses plain EC2 + Docker for
> every datastore (instead of RDS) because RDS is restricted in the academy
> environment. Swapping `aws_instance.postgres_reports`/`postgres_adapter` for
> `aws_db_instance` is mechanical if you have permissions.

## Smoke tests

Replace `KONG_IP` with the `kong_public_ip` output Terraform printed.

```bash
# Health checks through Kong
curl http://KONG_IP:8000/auth/health
curl http://KONG_IP:8000/ingest/health
curl http://KONG_IP:8000/reports/queries/health
curl http://KONG_IP:8000/adapters/health

# Register + login
curl -X POST http://KONG_IP:8000/auth/register \
     -H 'Content-Type: application/json' \
     -d '{"username":"nico","password":"secret123","companyCode":"ACME","companyName":"Acme Corp"}'

curl -X POST http://KONG_IP:8000/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"username":"nico","password":"secret123"}'

# Register an AWS account in the adapter, then trigger a synthetic run
curl -X POST http://KONG_IP:8000/adapters/accounts \
     -H 'Content-Type: application/json' \
     -d '{"company_code":"ACME","provider":"aws","account_id":"123456789012","region":"us-east-1"}'

curl -X POST http://KONG_IP:8000/adapters/trigger \
     -H 'Content-Type: application/json' \
     -d '{"account_id":1,
          "period_start":"2026-05-01T00:00:00Z",
          "period_end":"2026-05-02T00:00:00Z"}'

# Read what landed in reports
curl "http://KONG_IP:8000/reports/queries/companies/ACME/reports"
curl "http://KONG_IP:8000/reports/queries/companies/ACME/summary?from=2026-05-01T00:00:00Z&to=2026-05-31T00:00:00Z"
```

## How the data flows

1. **Authentication.** A client hits `POST /auth/login`; the auth-service
   validates credentials, issues a JWT, and on any failure writes to MongoDB
   (`unauthorized_access_logs`) and fans the incident out to the
   notification-service.
2. **Direct ingestion.** A client (already authenticated) submits a
   canonical OptiCloud report to `POST /ingest/reports`; data-injestion
   forwards it to `reports-service` (`CommandService`).
3. **Cloud-provider ingestion.** A client (or a scheduler) hits
   `POST /adapters/trigger`; `cloud-adapter` fetches raw provider data,
   `BaseCloudAdapter` dispatches it to `normalization-service`. There it is
   validated against `raw-report.schema.json`, normalized into the canonical
   shape, and forwarded to `reports-service`.
4. **Queries.** `GET /reports/queries/...` is served by `QueryService`,
   which uses Redis as a write-through cache. Every successful command in
   `CommandService` evicts the cache.

## Where the deployment diagram lives in the code

| Diagram element | Implementation |
| --- | --- |
| Kong orchestrator + routes `/auth/*`, `/ingest/*`, `/reports/*`, `/adapters/*` | `kong/kong.yaml` + `iac/kong.tf` |
| `auth-service.jar (NestJS)` with Company Guard, AccessLogWriter, JWT Strategy | `services/auth-service/src/` |
| `notification-service (FastAPI)` with Security Incident Handler + Email Dispatcher | `services/notification-service/app/` |
| `data-injestion (Django)` | `services/data-injestion/` |
| `cloud-adapter (Django)` with BaseCloudAdapter + adapter-aws | `services/cloud-adapter/adapters/` |
| `normalization-service.jar (Spring Boot)` with JSON Schema Validator + Report Forwarder | `services/normalization-service/src/main/java/com/opticloud/normalization/` |
| `reports-service.jar (Spring Boot, CQRS)` with CommandService + QueryService | `services/reports-service/src/main/java/com/opticloud/reports/` |
| MongoDB (`Colección de logs de accesos no autorizados`) | `iac/databases.tf::mongo_auth` + `auth-service/src/access-log/` |
| PostgreSQL `postgresql-reports` | `iac/databases.tf::postgres_reports` |
| PostgreSQL `postgresql-adapter` | `iac/databases.tf::postgres_adapter` |
| Redis `caché de reportes pre-calculados` | `iac/databases.tf::redis` + `reports-service/.../config/CacheConfig.java` |

## Testing the ASRs

The three Sprint 4 ASRs are validated with a **hybrid** approach: JMeter for the
quantitative thresholds, `curl` for the functional behaviour. Files live in
`testing/`.

### JMeter — quantitative thresholds

`testing/opticloud-asr-tests.jmx` contains three thread groups plus a `setUp`
that seeds users and one report. Edit the `KONG_IP` variable inside the plan (or
override on the CLI) and run headless:

```bash
cd testing
jmeter -n -t opticloud-asr-tests.jmx \
       -JKONG_IP=<kong-public-ip> \
       -l results.jtl -e -o reporte-html
# open reporte-html/index.html for the dashboard
```

Thread counts are JMeter variables so you can ramp up without editing the plan,
e.g. `-JASR1_THREADS=1000`.

| Thread group | ASR | What it measures | Pass criterion |
| --- | --- | --- | --- |
| `ASR1 - Latencia` | ASR 1 | `GET /reports/queries/companies/ACME/reports` under load; run twice to see the Redis cache effect (cold vs warm) | p99 ≤ 100 ms (Duration Assertion) |
| `ASR2 - Seguridad` | ASR 2 | `GET /auth/company/BETA/check` with an ACME token → CompanyGuard must block | HTTP 403 **and** block time < 500 ms |
| `ASR3 - Modificabilidad` | ASR 3 | latency baseline of the `adapter → normalization → reports` pipeline (`POST /adapters/trigger`) | run before/after adding a new adapter; delta ≤ 100 ms |

> **Honest note on ASR 1.** The target is 5.000 concurrent users at ≤ 100 ms.
> The IaC deploys each service on a single `t3.small` with single-node
> PostgreSQL/Redis — that hardware will *not* sustain 5.000@100 ms. The test
> still produces the architecturally relevant evidence (cache-hit latency
> collapses vs cache-miss, proving the CQRS + Redis design), and the report
> should state the measured ceiling and argue that horizontal scaling behind an
> ELB reaches the target. Don't fake the number.

### curl — functional behaviour (ASR 2 & ASR 3)

JMeter proves the *timing*; these prove the *behaviour*. Replace `KONG_IP`.

**ASR 2 — detect, block, log, notify:**
```bash
# 1. login as a user of company ACME
TOKEN=$(curl -s -X POST http://KONG_IP:8000/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"attacker","password":"Passw0rd!"}' | jq -r .access_token)

# 2. try to reach a resource of company BETA with ACME's token → expect 403
curl -i http://KONG_IP:8000/auth/company/BETA/check \
  -H "Authorization: Bearer $TOKEN"

# 3. evidence is persisted: connect to mongo-auth and check the collection
#    (ssh into the mongo-auth EC2, then:)
docker exec -it mongo-auth mongosh opticloud_auth \
  --eval 'db.unauthorized_access_logs.find().sort({occurredAt:-1}).limit(3)'

# 4. notification: check the notification-service container logs for the
#    dispatched incident (Email Dispatcher logs when SMTP is not configured)
docker logs notification-service | grep "Security incident"
```

**ASR 3 — add a provider without touching existing services:**
1. Create `services/cloud-adapter/adapters/gcp.py` with a `GcpAdapter(BaseCloudAdapter)`
   implementing only `fetch_raw_lines()`.
2. Register it in `adapters/registry.py` (one line: `"gcp": GcpAdapter`).
3. `git diff --stat` must show **only** those two files changed — no other
   service touched. That is the modifiability evidence.
4. Rebuild/redeploy only the `cloud-adapter` instance
   (`terraform taint aws_instance.cloud_adapter && terraform apply`); the other
   10 instances stay `Up` → no downtime.
5. Re-run the `ASR3` JMeter thread group and compare the latency delta.

The `Normalizer` in `normalization-service` already ships `normalizeGcp()` and
`normalizeAzure()` branches, so the normalization side needs no change either.
