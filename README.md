# OptiCloud — Sprint 4

Implementation of the OptiCloud Sprint 4 component and deployment architecture:
six microservices behind a Kong API Gateway with per-service databases
(PostgreSQL on RDS, MongoDB + Redis on a shared EC2), deployed on AWS via
Terraform — one EC2 per microservice, one EC2 for Kong, one EC2 for Mongo +
Redis, and two managed RDS PostgreSQL instances (8 EC2 + 2 RDS).

**Current versions:** Java 25 LTS, Spring Boot 3.5.6, Terraform AWS provider.

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
            └─ /adapters/aws/* ─► adapter-aws         (Django, one per provider)
                                    │   ├─► PostgreSQL (audit & accounts)
                                    └─► normalization-service (Spring Boot)
                                            │   (JSON Schema Validator)
                                            └─► reports-service (Report Forwarder)
```

> **On the "Orchestrator".** The component diagram draws an *Orquestador*
> between the gateway and the services. In the implementation that role is
> played by **Kong itself** — it is the single entry point that authenticates,
> rate-limits and routes each request to the right service by path. There is no
> separate orchestrator process to deploy; "Kong" and "Orquestador" are the
> same box.

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

## Deploying to AWS — step by step

The IaC under `iac/` provisions:

- **6 EC2** — one per microservice (auth, notification, data-injestion,
  cloud-adapter, normalization, reports).
- **1 EC2** — Kong API Gateway.
- **1 EC2** — Mongo + Redis colocated (RDS does not offer those engines).
- **2 RDS `db.t3.micro`** — managed PostgreSQL for reports and cloud-adapter.

Totals: **8 EC2** (16 vCPU at `t2.small`/`t3.micro`, fits a 16-vCPU
account quota) **+ 2 RDS** (separate quota pool). Every EC2 installs Docker,
clones this repo, builds the relevant image and runs it; the RDS endpoints
and private IPs are wired in by Terraform.

### Step 0 · Publish the repo

The EC2 instances `git clone` over HTTPS with no credentials, so the repo
must be **public**. Push the branch you want to deploy:

```bash
# deploy main (includes Java 25 upgrade and t3.micro instances)
git checkout main
git push origin main
```

### Step 1 · Open a working environment

Use **CloudShell** (the `>_` icon in the AWS console top bar) — it already has
your account credentials and `git`/`aws` preinstalled. A local terminal works
too if you have run `aws configure` with an IAM user's access keys.

```bash
aws sts get-caller-identity   # confirms you are authenticated
aws configure get region      # confirms a default region is set (or run: aws configure)
```

### Step 2 · Install Terraform

CloudShell does not ship Terraform; `install_terraform.sh` installs it via
`tfenv`. (Skip this step if you already have Terraform on a local machine.)

```bash
cd ~
git clone https://github.com/nreyl/OptiCloud-Sprint4.git
cd OptiCloud-Sprint4
bash install_terraform.sh
terraform --version
```

### Step 3 · Create an SSH key pair

Needed to read logs on the VMs and for the ASR 2 functional test.

```bash
cd ~/OptiCloud-Sprint4/iac
aws ec2 create-key-pair --key-name opticloud-key \
  --query 'KeyMaterial' --output text > opticloud-key.pem
chmod 400 opticloud-key.pem
```

### Step 4 · Configure variables

Create `iac/terraform.tfvars` (git-ignored, so secrets stay out of the repo):

```bash
cd ~/OptiCloud-Sprint4/iac
cat > terraform.tfvars <<EOF
repository_url    = "https://github.com/nreyl/OptiCloud-Sprint4.git"
repository_branch = "main"
key_name          = "opticloud-key"
jwt_secret        = "$(openssl rand -hex 32)"
postgres_password = "isis2503"
EOF
cat terraform.tfvars
```

> If you deployed a feature branch, set
> `repository_branch = "your-feature-branch"`.

### Step 5 · Deploy

```bash
cd ~/OptiCloud-Sprint4/iac
terraform init
terraform plan      # expect 8 EC2 + 2 RDS + 6 security groups + 1 DB subnet group
terraform apply     # type 'yes'
```

> **vCPU quota.** The stack is **8 EC2 instances**. At `t2.small` (1 vCPU
> each, default) that totals 8 vCPU; at `t3.micro` (2 vCPU each, Free-Tier-
> eligible) that totals 16 vCPU — both fit the default 16-vCPU "Running
> On-Demand Standard instances" quota of a fresh AWS account. The 2 RDS
> instances run on a **separate RDS quota** and do not consume EC2 vCPU.
> If you still hit `VcpuLimitExceeded`, request an increase in
> *Service Quotas → EC2*.

### Step 6 · Wait for the VMs to finish building (~8–12 min)

Creating the EC2 is fast, but each VM then installs Docker, clones the repo
and **builds its image** (the two Spring Boot services are the slow ones).
Watch one instance:

```bash
terraform output reports_service_public_ip
ssh -i opticloud-key.pem ubuntu@<PUBLIC_IP>
sudo tail -f /var/log/cloud-init-output.log   # Ctrl+C to stop
docker ps                                     # "Up" means that service is ready
exit
```

### Step 7 · Get the entry point

```bash
terraform output                       # all IPs
KONG=$(terraform output -raw kong_public_ip)
echo $KONG
```

### Step 8 · Smoke test (from CloudShell)

```bash
curl http://$KONG:8000/auth/health
curl http://$KONG:8000/ingest/health
curl http://$KONG:8000/reports/queries/health
curl http://$KONG:8000/adapters/aws/health

# full flow: register -> login -> register cloud account -> trigger -> query
curl -X POST http://$KONG:8000/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"nico","password":"secret123","companyCode":"ACME","companyName":"Acme Corp"}'

# login and capture the token (company-scoped report queries require it)
TOKEN=$(curl -s -X POST http://$KONG:8000/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"nico","password":"secret123"}' | jq -r .access_token)

curl -X POST http://$KONG:8000/adapters/aws/accounts -H 'Content-Type: application/json' \
  -d '{"company_code":"ACME","provider":"aws","account_id":"123456789012","region":"us-east-1"}'

curl -X POST http://$KONG:8000/adapters/aws/trigger -H 'Content-Type: application/json' \
  -d '{"account_id":1,"period_start":"2026-05-01T00:00:00Z","period_end":"2026-05-02T00:00:00Z"}'

# company-scoped reads need the Bearer token (Kong jwt + CompanyAuthFilter)
curl -H "Authorization: Bearer $TOKEN" "http://$KONG:8000/reports/queries/companies/ACME/reports"
curl -H "Authorization: Bearer $TOKEN" "http://$KONG:8000/reports/queries/companies/ACME/spend"
```

A **502** on a `/health` means that container is still building or retrying —
wait and retry (services run with `--restart=always`, they self-heal once
their database is reachable).

### Step 9 · Tear down (when finished — it burns credits)

```bash
cd ~/OptiCloud-Sprint4/iac
terraform destroy      # type 'yes'
```

Recreate a single broken VM without destroying everything:

```bash
terraform taint aws_instance.<name>   # e.g. aws_instance.kong
terraform apply
```
EC2 resource names (for `terraform taint aws_instance.<name>`): `kong`,
`auth_service`, `reports_service`, `normalization_service`, `cloud_adapter`,
`data_injestion`, `notification_service`, `datastores`.

RDS resource names (use `aws_db_instance.<name>`): `postgres_reports`,
`postgres_adapter`.

> **Datastore layout.** The two PostgreSQL databases are managed RDS
> (`db.t3.micro`) — they match the deployment diagram and do not consume EC2
> vCPU. MongoDB and Redis are colocated on a single EC2 (`aws_instance.datastores`)
> because RDS does not offer those engines and DocumentDB/ElastiCache are not
> Free-Tier-eligible. RDS provisioning takes a few minutes longer than EC2 —
> expect ~5–10 min for `postgres_reports` and `postgres_adapter` to become
> available during `apply`.

## How the data flows

1. **Authentication.** A client hits `POST /auth/login`; the auth-service
   validates credentials, issues a JWT, and on any failure writes to MongoDB
   (`unauthorized_access_logs`) and fans the incident out to the
   notification-service.
2. **Direct ingestion.** A client (already authenticated) submits a
   canonical OptiCloud report to `POST /ingest/reports`; data-injestion
   forwards it to `reports-service` (`CommandService`).
3. **Cloud-provider ingestion.** A client (or a scheduler) hits
   `POST /adapters/aws/trigger`; `adapter-aws` fetches raw provider data,
   `BaseCloudAdapter` dispatches it to `normalization-service`. There it is
   validated against `raw-report.schema.json`, normalized into the canonical
   shape, and forwarded to `reports-service`.
4. **Queries.** `GET /reports/queries/...` is served by `QueryService`,
   which uses Redis as a write-through cache. Every successful command in
   `CommandService` evicts the cache. Company-scoped queries
   (`/reports/queries/companies/{company}/**`) are protected twice: Kong's
   `jwt` plugin rejects requests without a valid, unexpired token at the
   gateway, and reports-service's `CompanyAuthFilter` then verifies the
   token's `companyCode` claim matches the `{company}` in the path
   (returns 403 on mismatch) — the JWT is decoded locally, no call to
   auth-service, keeping the read within the latency budget.
5. **Monthly spend (materialized view).** `GET /reports/queries/companies/{company}/spend`
   returns pre-aggregated monthly spend per provider read from the
   `company_spend_summary` **PostgreSQL materialized view** (`SpendSummaryService`).
   `CommandService` runs `REFRESH MATERIALIZED VIEW` after every write, and the
   read is itself cached in Redis — so the monthly report never recomputes the
   aggregation on a request.

## Where the deployment diagram lives in the code

| Diagram element | Implementation |
| --- | --- |
| Kong orchestrator + routes `/auth/*`, `/ingest/*`, `/reports/*`, `/adapters/<provider>/*` | `kong/kong.yaml` + `iac/kong.tf` |
| `auth-service.jar (NestJS)` with Company Guard, AccessLogWriter, JWT Strategy | `services/auth-service/src/` |
| `notification-service (FastAPI)` with Security Incident Handler + Email Dispatcher | `services/notification-service/app/` |
| `data-injestion (Django)` | `services/data-injestion/` |
| `adapter-aws (Django)` — one cloud-adapter container per provider (BaseCloudAdapter chassis) | `services/cloud-adapter/adapters/` |
| `normalization-service.jar (Spring Boot)` with JSON Schema Validator + Report Forwarder | `services/normalization-service/src/main/java/com/opticloud/normalization/` |
| `reports-service.jar (Spring Boot, CQRS)` with CommandService + QueryService | `services/reports-service/src/main/java/com/opticloud/reports/` |
| MongoDB (`Colección de logs de accesos no autorizados`) | `iac/databases.tf::aws_instance.datastores` (Docker) + `auth-service/src/access-log/` |
| PostgreSQL `postgresql-reports` (RDS) | `iac/databases.tf::aws_db_instance.postgres_reports` |
| PostgreSQL `postgresql-adapter` (RDS) | `iac/databases.tf::aws_db_instance.postgres_adapter` |
| Redis `caché de reportes pre-calculados` | `iac/databases.tf::aws_instance.datastores` (Docker) + `reports-service/.../config/CacheConfig.java` |

## Testing the ASRs

The three Sprint 4 ASRs are validated with a **hybrid** approach: JMeter for the
quantitative thresholds, `curl` for the functional behaviour. Files live in
`testing/`.

### JMeter — quantitative thresholds

`testing/opticloud-asr-tests.jmx` contains three thread groups plus a `setUp`
that seeds users and one report.

**1 · Install JMeter** (on your local machine — CloudShell is headless/ephemeral;
JMeter needs Java, already covered by the JDK you build the services with):

```bash
choco install jmeter      # Windows
# or: brew install jmeter            (macOS)
# or download the zip from https://jmeter.apache.org/download_jmeter.cgi
jmeter --version
```

**2 · Run headless** from the `testing/` folder (relative path to `companies.csv`),
passing the Kong IP on the CLI — no need to edit the `.jmx`:

```bash
cd testing
jmeter -n -t opticloud-asr-tests.jmx \
       -JKONG_IP=<kong-public-ip> \
       -l results.jtl -e -o reporte-html
# open reporte-html/index.html for the dashboard
```

`-n` = no-GUI (the correct mode for measuring); `-e -o reporte-html` generates
the HTML dashboard with percentiles — that is the deliverable artifact.

**3 · Ramp up.** Thread counts are JMeter variables, so you scale without editing
the plan: `-JASR1_THREADS=1000`, `-JASR2_THREADS=100`, etc.

**4 · Debug (GUI mode, never for measuring).** If a request fails, open the plan
in the GUI, add a *View Results Tree*, run small: `jmeter -t opticloud-asr-tests.jmx`.

| Thread group | ASR | What it measures | Pass criterion |
| --- | --- | --- | --- |
| `ASR1 - Latencia` | ASR 1 | `GET /reports/queries/companies/ACME/reports` under load; run twice to see the Redis cache effect (cold vs warm) | p99 ≤ 100 ms (Duration Assertion) |
| `ASR2 - Seguridad` | ASR 2 | `GET /auth/company/BETA/check` with an ACME token → CompanyGuard must block | HTTP 403 **and** block time < 500 ms |
| `ASR3 - Modificabilidad` | ASR 3 | latency baseline of the `adapter → normalization → reports` pipeline (`POST /adapters/aws/trigger`) | run before/after adding a new adapter; delta ≤ 100 ms |

> **Honest note on ASR 1.** The target is 5.000 concurrent users at ≤ 100 ms.
> The IaC deploys reports-service on a single `t3.micro` with one `db.t3.micro`
> RDS and a single-node Redis — that hardware will *not* sustain 5.000@100 ms.
> The test still produces the architecturally relevant evidence (cache-hit
> latency collapses vs cache-miss, and the materialized view removes the
> aggregation cost), and the report should state the measured ceiling and argue
> that horizontal scaling behind an ELB reaches the target. Don't fake the number.

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
3. Deploy a new **adapter-gcp** container in parallel: uncomment the
   `aws_instance.adapter_gcp` block in `iac/services.tf` (same image,
   `ADAPTER_PROVIDER=gcp`).
4. Expose it: uncomment the `adapter_gcp` service/route and `adapter_gcp_upstream`
   in `kong/kong.yaml`, and the `<ADAPTER_GCP_HOST>` sed line in `iac/kong.tf`.
   The new route is `/adapters/gcp/*`; the `/adapters/aws/*` route is untouched.
5. The existing 8 EC2 instances (including `adapter-aws`) and both RDS stay
   `Up` while the new container comes online → **downtime = 0**.
6. Re-run the `ASR3` JMeter thread group and compare the latency delta.

> **How extensibility works.** The same `cloud-adapter` image is deployed once
> per provider, each container scoped by `ADAPTER_PROVIDER` and exposed under
> its own `/adapters/<provider>/*` route in Kong. The `BaseCloudAdapter` chassis
> (logging, health, validation, dispatch) is shared; a concrete provider only
> implements `fetch_raw_lines()` and is added to `adapters/registry.py`. Adding
> a provider is therefore a **new class + a new container + a new Kong route** —
> no existing service definition, route, or instance changes, matching the
> infographic's "one container per adapter, `/adapters/<provider>/*`". The
> `Normalizer` in `normalization-service` already ships `normalizeGcp()` and
> `normalizeAzure()`, so the normalization side needs no change either.
