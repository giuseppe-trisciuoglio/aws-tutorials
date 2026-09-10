# Stack: setup-database

Creates the **RDS PostgreSQL 17** instance that hosts the Supplier Gateway
state (search, booking, hotel content). The gateway uses `jakarta.persistence`
with `spring.jpa.hibernate.ddl-auto=update`, so the first connection
from the container creates the schema automatically — this stack simply
creates the empty database.

## Resources created

- `AWS::RDS::DBParameterGroup` (family `postgres17`) — forces SSL on
  connections (`rds.force_ssl=1`) and logs queries > 1s.
- `AWS::RDS::DBSubnetGroup` — DB in the two private subnets on different AZs.
- `AWS::EC2::SecurityGroup` — opens 5432 from the VPC CIDR.
- `AWS::RDS::DBInstance` — PostgreSQL 17, single-AZ for the tutorial
  (`MultiAZ=false`), `DeletionProtection=true`, `DeletionPolicy=Retain`,
  7-day backup retention, logs exported to CloudWatch
  (`postgresql` log group).

## Parameters

| Name | Default | Notes |
|---|---|---|
| `VpcId`, `PrivateSubnet1`, `PrivateSubnet2`, `VpcCidr` | — | output from `setup-environment` |
| `DBName` | `hotelsearch` | database name (RDS doesn't accept underscores, using a valid name) |
| `DBUsername` | — | master user (not a secret: only the password is generated automatically) |
| ~~`DBPassword`~~ | — | ~~removed~~ The password is generated automatically by RDS and stored in Secrets Manager (`ManageMasterUserPassword: true`) |
| `DBInstanceClass` | `db.t4g.micro` | for tutorial; production: `db.t3.small` or `db.r6g.large` |
| `DBEngineVersion` | `17.2` | PostgreSQL version (gateway uses `postgres:17`) |
| `DBAllocatedStorage` | `20` | initial GB |
| `DBMaxAllocatedStorage` | `100` | max GB with autoscaling |
| `MultiAZ` | `false` | `true` for production (doubles the cost, but automatic failover) |

> **ℹ️ DB password** — the template **does not accept** a password parameter.
> RDS generates a random password with `ManageMasterUserPassword: true`,
> stores it in a Secrets Manager secret (`rds!db-<id>-<random>`) and maintains
> automatic rotation. To retrieve it:
>
> ```bash
> aws secretsmanager get-secret-value \
>   --secret-id $(aws cloudformation describe-stacks \
>     --stack-name supplier-gateway-setup-database-dev \
>     --query 'Stacks[0].Outputs[?OutputKey==`DBSecretArn`].OutputValue' \
>     --output text)
> ```

## Prerequisites

- `setup-environment` deployed (VPC, subnets, CIDR outputs).

## Deploy

```bash
make deploy STEP_NAME=setup-database
```

> **Note**: the first RDS instance creation takes ~5-10 minutes
> for storage provisioning and service startup.

## Outputs

- `DBEndpoint` — hostname (e.g. `gt-supplier-gateway-postgres-dev.xxx.eu-west-1.rds.amazonaws.com`).
- `DBPort` — port (default `5432`).
- `DBName` — `hotelsearch`.
- `DBUsername` — master user (username only, the password is in Secrets Manager).
- `DBSecretArn` — ARN of the Secrets Manager secret containing
  `{username, password, engine, host, port, dbname}`. **This is the only
  value to pass to subsequent steps** — the password never leaves
  Secrets Manager.
- `DBInstanceIdentifier` — ARN-style identifier.
- `DBSecurityGroupId` — security group, to reference if you want
  stricter ingress rules (e.g. only from the task SG).

## Cost

| Class | Monthly cost (eu-west-1) | Notes |
|---|---|---|
| `db.t4g.micro` (tutorial, single-AZ) | ~$13 | burst-only, OK for dev |
| `db.t4g.small` (tutorial, single-AZ) | ~$26 | |
| `db.t3.small` Multi-AZ | ~$52 | production small |
| `db.r6g.large` Multi-AZ | ~$320 | production with real workloads |

Plus storage (~$0.115/GB/month for gp3) and backups (free up to
allocated size).

## Security notes

- `PubliclyAccessible: false` — the DB accepts connections only from inside
  the VPC.
- `rds.force_ssl=1` — every JDBC connection must use SSL. The Spring Boot
  PostgreSQL driver does this by default, but it's an extra protection
  against misconfiguration.
- `DeletionProtection: true` — blocks accidental `DELETE` via console
  or CLI. To disable it requires an explicit stack update.
- `ManageMasterUserPassword: true` — the password is never passed
  as a CloudFormation parameter nor written to any file. RDS generates it,
  stores it in Secrets Manager, and maintains automatic rotation.
- **Current tutorial limitation**: the DB SG opens 5432 from the VPC
  CIDR (`10.0.0.0/16`), not only from the task SG. For production,
  change the rule to:
  ```yaml
  SecurityGroupIngress:
    - IpProtocol: tcp
      FromPort: 5432
      ToPort: 5432
      SourceSecurityGroupId: !Ref TaskSecurityGroupId
  ```
  where `TaskSecurityGroupId` comes from an export of `setup-cluster-ecs`
  (requires reordering the deployments or a shared export).

## Database schema

The gateway manages the schema via JPA (`ddl-auto=update`). On the first
deployment of the ECS tasks, Hibernate creates the tables automatically.
The main entities are:

- `searches` — search state (`PENDING → PARTIAL → COMPLETED`)
- `bookings` — booking state (`PENDING → CONFIRMED | REJECTED | FAILED | UNKNOWN`)
- `hotels` — hotel content with GIATA cross-reference
- `supplier_calls` — audit of supplier calls

Flyway scripts are not used (the project is a POC, not production). For
production, introducing Flyway for versioned migrations is recommended.
