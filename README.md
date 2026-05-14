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
  -var "repository_url=https://github.com/<your-user>/OptiCloud-Sprint4.git" \
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
