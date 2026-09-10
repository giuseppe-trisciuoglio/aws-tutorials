# Architecture: ECS Fargate for the Supplier Gateway

In-depth document for the tutorial
[`ecs-fargate-supplier-gateway`](../README.md). Discusses the design
decisions from the article and how they translate into CloudFormation templates.

## Overview

<p align="center">
  <img src="architecture.png" alt="Supplier Gateway architecture on AWS: ALB in the public subnets, four Fargate services in the private subnets, AmazonMQ RabbitMQ and RDS PostgreSQL, with VPC endpoints for ECR, CloudWatch Logs, Secrets Manager and S3." width="100%">
</p>

Vector source: [`architecture.svg`](architecture.svg). Both files are produced
by `docs/build-architecture-svg.py` from the official AWS icons — see
[`icons/README.md`](icons/README.md).

## Design decisions

### 1. ECS Fargate instead of EKS or Lambda

**Why not EKS.** Control plane at ~$75/month, steep learning curve,
Kubernetes operations not justified for 4 Spring Boot services that
talk to 5 HTTP suppliers. Helm/ArgoCD/custom operators are overkill.

**Why not Lambda.** Lambdas have cold starts and a stateless nature.
The Supplier Gateway has state (`PENDING → PARTIAL → COMPLETED`) and workers
that call suppliers with latencies up to 20-30s during peak hours. The
event-based model doesn't fit.

**Why ECS Fargate.** Serverless without node management, versioned task
definitions, native ALB, secrets injection from Secrets Manager, free
control plane. Always-on workload with a time constraint → right trade-off.

### 2. PostgreSQL via RDS, not via container

The `hotel-supplier-gateway` persists search, booking, and hotel state
in PostgreSQL (Spring Data JPA with
`spring.jpa.hibernate.ddl-auto=update`). Three options evaluated:

- **(a) Containerized PostgreSQL on ECS** — zero additional RDS cost,
  but you manage replication, backup, and point-in-time recovery yourself,
  and if the container dies the data is lost without EFS + replica.
- **(b) RDS PostgreSQL managed** — managed by AWS, automatic backups,
  optional Multi-AZ failover, parameter groups, logs exported to
  CloudWatch. This is the choice adopted by the tutorial.
- **(c) Aurora Serverless** — auto-scaling down to zero, but ~30s cold start
  on first use and strong AWS lock-in.

RDS is in **private subnets**, with a security group that opens 5432 from
the VPC CIDR. The Spring Boot JDBC driver enforces SSL by default,
consistent with the `rds.force_ssl=1` parameter group set by the stack.

The original article does not mention RDS — the tutorial adds it because
the real gateway implementation requires a database.

### 3. Four task definitions from the same Docker image

The current gateway code is a single Spring Boot jar. The article
proposes splitting it into 4 logical services:

| Service | Spring Profile | HTTP Port | RabbitMQ Consumer |
|---|---|---|---|
| `gateway` | `gateway` | 8080 | no |
| `worker` | `worker` | no | yes (calls suppliers) |
| `aggregator` | `aggregator` | no | yes (collects partial results) |
| `event-consumer` | `event-consumer` | no | yes (async events) |

Differentiation is via `SPRING_PROFILES_ACTIVE`. **This requires code
changes** (conditional beans per profile), out of scope for the tutorial.

### 4. Networking: private subnets + VPC endpoints

**All tasks in private subnets.** No public IP, no inbound port open from
outside except the ALB.

**ALB in public subnets** is the only entry point.

**Interface VPC endpoints** for ECR, CloudWatch Logs, Secrets Manager.
Without them, every image pull or log push would go through the
NAT gateway: ~$3/day + $0.045/GB. With 4 services doing image pulls
on every deploy and continuous log pushes, the savings are ~$76/month.

**Gateway VPC endpoint for S3** is free (route table-based, no hourly cost).

| Endpoint | Type | Cost/hr | Cost/month (730h) |
|---|---|---|---|
| `ecr.api` | Interface | $0.01 | $7.30 |
| `ecr.dkr` | Interface | $0.01 | $7.30 |
| `logs` | Interface | $0.01 | $7.30 |
| `secretsmanager` | Interface | $0.01 | $7.30 |
| `s3` | Gateway | $0 | $0 |
| **Total** | | | **$29.20** |

### 5. AmazonMQ managed instead of containerized RabbitMQ

Three reasons in the article:

1. **Durability** — replication, backup, failover managed by AWS. A RabbitMQ
   on Fargate is a single task with EFS, but you have to build the
   resilience yourself.
2. **Operational burden** — containerized broker = monitoring disk,
   memory, connections, quorum cluster. AmazonMQ does it for you.
3. **Cost** — `mq.m5.large` ~$130/month, justified for the search deadline.

The broker is in **private subnets**, with a security group that opens AMQP
only from the VPC CIDR. No internet-facing.

### 6. Secrets Manager end-to-end (DB + broker + suppliers)

The article insists: *"Never bake secrets into Docker images"*.
The tutorial goes further and applies the same principle to **all**
credentials, including those of the DB and the broker:

| Credential | Where it's created | How it reaches the container |
|---|---|---|
| Supplier API key | `setup-secrets` (placeholder) → updated via `aws secretsmanager put-secret-value` | `secrets:` key in task definition |
| **DB master password** | `setup-database` with `ManageMasterUserPassword: true` (RDS generates, stores in SM, rotates) | `secrets:` key: `DBSecretArn::username/password` |
| **Broker admin password** | `setup-amazonmq` with `GenerateSecretString` (24 char random) | `secrets:` key: `BrokerSecretArn::username/password` |

Task definition pattern:

```yaml
Environment:
  - Name: SPRING_DATASOURCE_URL
    Value: !Sub "jdbc:postgresql://${DBEndpoint}:${DBPort}/${DBName}"
  # No password here.
Secrets:
  - Name: SPRING_DATASOURCE_USERNAME
    ValueFrom: !Sub "${DBSecretArn}::username"
  - Name: SPRING_DATASOURCE_PASSWORD
    ValueFrom: !Sub "${DBSecretArn}::password"
  - Name: RABBITMQ_USER
    ValueFrom: !Sub "${BrokerSecretArn}::username"
  - Name: RABBITMQ_PASSWORD
    ValueFrom: !Sub "${BrokerSecretArn}::password"
```

Credentials are retrieved via `secrets:` by the container at startup, **never
present in plaintext** in the template or in the parameter files. To retrieve
a value (for debugging or rotation):

```bash
aws secretsmanager get-secret-value \
  --secret-id $(aws cloudformation describe-stacks \
    --stack-name supplier-gateway-setup-database-dev \
    --query 'Stacks[0].Outputs[?OutputKey==`DBSecretArn`].OutputValue' \
    --output text)
```

Task roles have `secretsmanager:GetSecretValue` with `Resource` scoped
to the specific ARNs.

### 7. IAM: one task role per service

The article: *"each task has its own task role"*. Even though in this
design all 4 have similar policies (Secrets + SSM), the separation
is ready to be extended with different permissions.

| Role | Policies |
|---|---|
| `GatewayTaskRole` | `SupplierSecretsAccess` + `InfrastructureSecretsAccess` (DB + broker secrets) + `SSMSessionManager` |
| `WorkerTaskRole` | same |
| `AggregatorTaskRole` | same |
| `EventConsumerTaskRole` | same |
| `ExecutionRole` | `AmazonECSTaskExecutionRolePolicy` (managed, ECR pull + logs) |

Each task role has two `secretsmanager:GetSecretValue` policies:

- `SupplierSecretsAccess` — scopes `Resource` to the ARNs of the 3 POC
  suppliers (Booking, Expedia, HotelBeds).
- `InfrastructureSecretsAccess` — scopes `Resource` to the ARNs of
  infrastructure secrets (`DBSecretArn` + `BrokerSecretArn`).

All also have `ssmmessages:*` for ECS Exec.

### 8. Container Insights + ECS Exec

**Container Insights** enabled on the cluster. Metrics per task, per
container, per service: CPU, memory, network. For the Supplier Gateway
the key metric is *task count per service*: if workers drop below
`DesiredCount`, something is wrong.

**ECS Exec** (`EnableExecuteCommand: true`) allows `docker exec` on
a Fargate task without SSH or inbound ports. Uses SSM under the hood.

```bash
aws ecs execute-command \
  --cluster gt-supplier-gateway-cluster-dev \
  --task <task-arn> \
  --container gateway \
  --interactive \
  --command "/bin/sh"
```

### 9. Rolling update with circuit breaker

The article: *"blue/green would be overkill — four small services,
no need for gradual traffic shifting"*. Configuration:

```yaml
DeploymentConfiguration:
  MaximumPercent: 200
  MinimumHealthyPercent: 100
  DeploymentCircuitBreaker:
    Enable: true
    Rollback: true
```

If new tasks fail the health check, the deployment stops and rolls back
automatically.

## Cost (article + tutorial)

Values for eu-west-1, monthly. The `...` rows are added by the tutorial
compared to the original article.

| Item | Article (~$340) | Tutorial (~$264) | Production (~$425) |
|---|---|---|---|
| Fargate (4 services) | ~$70 | ~$144 | ~$144 |
| ALB | ~$22 | ~$22 | ~$22 |
| AmazonMQ (mq.m5.large / mq.t3.micro / mq.m5.large) | ~$130 | ~$15 | ~$130 |
| CloudWatch Logs | ~$5-10 | ~$8 | ~$15 |
| NAT Gateway | ~$90 | ~$32 | ~$32 |
| VPC Endpoints | ~$14 | ~$28 | ~$28 |
| **RDS PostgreSQL** (added by tutorial) | — | ~$13 | ~$52 |
| Secrets Manager | — | ~$2 | ~$2 |
| **Total** | **~$340** | **~$264** | **~$425** |

The tutorial costs are **higher** than the article's (~+$70 for
Fargate + RDS) because the tutorial uses more generous parameters:
- Fargate: 1 vCPU + 2 GB for workers (article: 0.5 vCPU + 1 GB for all)
- Added RDS, not present in the article
- All 4 services run simultaneously (article: only desired count)

The article has a more "aspirational" estimate. The tutorial has a more
realistic estimate for those who want to try the full architecture.

## Key takeaway

> The real cost of ECS is not ECS, it's the network.

Interface VPC endpoints are the single optimization with the highest ROI
in the entire architecture: ~$28/month of spend to avoid ~$80/month
of NAT traffic. It's the change you can make without touching the
application and that pays for itself in the first month.
