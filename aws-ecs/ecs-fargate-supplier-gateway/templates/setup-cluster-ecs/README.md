# Stack: setup-cluster-ecs

The main piece. Creates the **ECS cluster**, the **Application Load Balancer**
in public subnets, **4 task definitions** (gateway, worker, aggregator,
event-consumer) and **4 Fargate services** in private subnets, with
**Container Insights**, **ECS Exec**, and **deployment circuit breaker**.

## Resources created

### Cluster
- `AWS::ECS::Cluster` with `containerInsights: enabled` and capacity providers
  `FARGATE` / `FARGATE_SPOT`.

### IAM Roles (5 total)
- `ExecutionRole` — the standard role for ECR pull + CloudWatch Logs push.
- `GatewayTaskRole` — Secrets Manager for suppliers, SSM for ECS Exec.
- `WorkerTaskRole` — Secrets Manager + SSM.
- `AggregatorTaskRole` — Secrets Manager + SSM.
- `EventConsumerTaskRole` — Secrets Manager + SSM.

> **Role differentiation** — the article insists on *"each task has its own
> task role"*. Even though in this design all 4 have similar policies
> (Secrets + SSM), the separation is ready to be extended with different
> permissions (e.g. the aggregator could have `elasticache:*` if the
> article evolved toward managed Redis).

### Task Definitions (4)
- **`gateway`** — 512 CPU, 1 GB RAM, port 8080, exposed via ALB. Spring
  profile `gateway`. Receives all supplier secrets via `secrets`.
- **`worker`** — 1024 CPU, 2 GB RAM, no port. Profile `worker`.
  RabbitMQ consumer that calls suppliers.
- **`aggregator`** — 512 CPU, 1 GB RAM. Profile `aggregator`. RabbitMQ
  consumer that aggregates partial results and updates state.
- **`event-consumer`** — 512 CPU, 1 GB RAM. Profile `event-consumer`.
  Consumer for async events from the events queue.

All 4:
- Same Docker image (`ContainerImage` as parameter).
- Same execution role, different task role (with two policies: supplier secrets + infrastructure secrets).
- Same `RABBITMQ_HOST` / `RABBITMQ_PORT` injected as env vars.
- **RabbitMQ username/password** retrieved via `secrets:` key from `BrokerSecretArn`.
- **PostgreSQL username/password** retrieved via `secrets:` key from `DBSecretArn`.
- PostgreSQL connection via `SPRING_DATASOURCE_URL` (override of the default
  `localhost:5432` in `application.properties`).
- Separate CloudWatch log group.

> **Secrets Manager pattern** — every credential (supplier API key, DB
> master password, broker admin password) is retrieved via `secrets:`
> by the container at startup. **No password is in plaintext** in the
> template, in the parameter files, or in the task definition env vars.

### ECS Services (4)
- `GatewayService` — desired count 2, behind ALB target group.
- `WorkerService` — desired count 2, no load balancer.
- `AggregatorService` — desired count 1.
- `EventConsumerService` — desired count 1.

All with:
- `AssignPublicIp: DISABLED` (private subnet).
- `EnableExecuteCommand: true` (for ECS Exec).
- `DeploymentCircuitBreaker: { Enable: true, Rollback: true }`.

### Load Balancer
- Internet-facing ALB in public subnets.
- HTTP listener on port 80 forwarding to the gateway target group.
- Configurable health check (`HealthCheckPath`, default `/actuator/health`).

### Security Groups
- `ALBSecurityGroup` — accepts 80/443 from internet.
- `TaskSecurityGroup` — accepts gateway traffic from ALB, AMQP from broker,
  and all internal VPC traffic.

### CloudWatch Log Groups
- One per service, 14-day retention, prefix `/ecs/<suffix>-<app>-<env>`.

## Parameters

| Name | Default | Notes |
|---|---|---|
| `TemplateBucket` | — | output from `setup-buckets` |
| `VpcId`, `VpcCidr` | — | output from `setup-environment` |
| `PublicSubnet1`, `PublicSubnet2` | — | output from `setup-environment` |
| `PrivateSubnet1`, `PrivateSubnet2` | — | output from `setup-environment` |
| `BrokerSecurityGroupId` | — | output from `setup-amazonmq` |
| `BrokerAmqpEndpoint` | — | output from `setup-amazonmq` (`amqps://host:port`) |
| `BrokerSecretArn` | — | output from `setup-amazonmq` (broker Secrets Manager secret) |
| `BookingComSecretArn`, `ExpediaSecretArn`, `HotelBedsSecretArn` | — | output from `setup-secrets` (3 POC suppliers) |
| `DBEndpoint`, `DBPort`, `DBName` | — | output from `setup-database` |
| `DBSecretArn` | — | output from `setup-database` (DB Secrets Manager secret) |
| `ContainerImage` | — | full ECR URI with `latest` or digest |
| `ContainerPort` | `8080` | container HTTP port |
| `GatewayCpu` / `GatewayMemory` | `512` / `1024` | |
| `WorkerCpu` / `WorkerMemory` | `1024` / `2048` | |
| `AggregatorCpu` / `AggregatorMemory` | `512` / `1024` | |
| `EventConsumerCpu` / `EventConsumerMemory` | `512` / `1024` | |
| `<Service>DesiredCount` | `2` / `2` / `1` / `1` | desired tasks per service |
| `HealthCheckPath` | `/actuator/health` | path for ALB health check |

## Prerequisites

- `setup-buckets` deployed.
- `setup-environment` deployed (outputs `VpcId`, `VpcCidr`, subnets, etc).
- `setup-amazonmq` deployed (outputs `BrokerAmqpEndpoint`, `BrokerSecurityGroupId`, `BrokerSecretArn`).
- `setup-database` deployed (outputs `DBEndpoint`, `DBPort`, `DBName`, `DBSecretArn`).
- `setup-secrets` deployed (outputs `<Supplier>SecretArn` × 5).
- Docker image pushed to ECR with `ContainerImage` configured.
- IAM permissions: the deployer must be able to create IAM roles with
  `CAPABILITY_NAMED_IAM`.

## Deploy

```bash
make deploy STEP_NAME=setup-cluster-ecs
```

## Outputs

- `ECSClusterName`, `ECSClusterArn`
- `LoadBalancerDNS` — public DNS of the ALB, hit `http://<dns>/api/v1/searches`.
- `<Service>ServiceName` × 4 — one per ECS service.
- `GatewayTaskDefinitionArn`
- `TaskSecurityGroupId`

## Design notes

### Differentiation of the 4 services

The article says *"all with the same Dockerfile"*. Differentiation here
is via `SPRING_PROFILES_ACTIVE`: `gateway`, `worker`, `aggregator`,
`event-consumer`. **This requires modifications to the gateway code** —
the current implementation (a single `default` profile) does not yet support
selective bean activation per profile. The template is a *proposed design*
from the article, not an immediate deployment of the current code.

### Rolling update with circuit breaker

The article explains why rolling update is the right choice for this
workload: *"four small services, no need for gradual traffic shifting"*.
The configuration applied:

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

### ECS Exec

All 4 services have `EnableExecuteCommand: true`. To `docker exec` on a task:

```bash
aws ecs execute-command \
  --cluster gt-supplier-gateway-cluster-dev \
  --task <task-arn> \
  --container gateway \
  --interactive \
  --command "/bin/sh"
```

### Redis vs PostgreSQL difference

The article mentions Redis as the cache for partial results. The current
gateway implementation uses PostgreSQL as the search store. The template
respects the article in resource names and IAM policies, but **the
caching layer is independent from compute** — the aggregator can talk to
Redis or PostgreSQL without infrastructure changes.

## Cost

| Resource | Monthly cost (eu-west-1, article values) |
|---|---|
| 2× gateway tasks (0.5 vCPU/1GB) | ~$36 |
| 2× worker tasks (1 vCPU/2GB) | ~$72 |
| 1× aggregator task (0.5 vCPU/1GB) | ~$18 |
| 1× event consumer task (0.5 vCPU/1GB) | ~$18 |
| **Fargate total** | **~$144** |
| ALB | ~$22 + LCU |
| Container Insights | ~$5 |
| CloudWatch Logs (4 groups, 14-day retention) | ~$3-5 |

Add to these:
- VPC endpoints (~$28/month) and NAT gateway (~$32) from `setup-environment`
- AmazonMQ broker from `setup-amazonmq` (`mq.t3.micro` ~$15 or
  `mq.m5.large` ~$130)
- RDS PostgreSQL from `setup-database` (`db.t4g.micro` ~$13 or
  `db.t3.small` Multi-AZ ~$52)

Estimated total:
- **~$270/month** for the tutorial (all on db.t4g.micro / mq.t3.micro)
- **~$440/month** for production (db.t3.small Multi-AZ / mq.m5.large)
