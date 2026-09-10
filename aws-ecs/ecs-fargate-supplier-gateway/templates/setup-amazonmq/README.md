# Stack: setup-amazonmq

Creates the **AmazonMQ for RabbitMQ** broker that hosts the fan-out of the
Supplier Gateway. The article chooses AmazonMQ managed over a containerized
RabbitMQ for three reasons:

1. **Durability** — replication, backup, failover managed by AWS.
2. **Operational burden** — no manual monitoring of disk, memory, connections.
3. **Cost** — `mq.m5.large` ~$130/month, justified in production for the
   search deadline constraint.

For the tutorial we start with `mq.t3.micro` (~$15/month), which is enough
to validate the architecture and test the fan-out end-to-end.

## Resources created

- **`AWS::AmazonMQ::Broker`** — RabbitMQ single-instance in
  `SINGLE_INSTANCE` deployment (for the tutorial). For production switch to
  `CLUSTER_MULTI_AZ` with 3 nodes.
- **`AWS::EC2::SecurityGroup`** — opens AMQP (5671) and management console
  (15671) from the VPC CIDR. No internet-facing.

## Parameters

| Name | Description |
|---|---|
| `VpcId` | Output from `setup-environment` |
| `PrivateSubnet1` / `PrivateSubnet2` | Output from `setup-environment` |
| `BrokerInstanceType` | `mq.t3.micro` (tutorial) or `mq.m5.large` (production) |
| `BrokerEngineVersion` | RabbitMQ version (default `3.13`) |
| `AdminUsername` | `supplieradmin` — admin username (not a secret). Password is generated automatically and stored in Secrets Manager. |

> **ℹ️ Broker password** — the template **does not accept** a password parameter.
> A Secrets Manager secret is created with `GenerateSecretString` (24 chars,
> at least one character of each type) and the password is passed to the broker
> via dynamic reference `{{resolve:secretsmanager:...}}`. To retrieve it:
>
> ```bash
> aws secretsmanager get-secret-value \
>   --secret-id gt/supplier-gateway/mq/admin \
>   --query SecretString --output text
> ```

## Prerequisites

The `setup-environment` stack must have been deployed. Its outputs
`VpcId`, `PrivateSubnet1`, `PrivateSubnet2` go into the `parameters.json`
file of this step.

## Deploy

```bash
make deploy STEP_NAME=setup-amazonmq
```

## Outputs

- `BrokerArn` — ARN of the broker.
- `BrokerAmqpEndpoint` — AMQP URL for consumers/producers.
- `BrokerSecretArn` — ARN of the Secrets Manager secret containing
  `{username, password}`. **This is the only value to pass to subsequent
  steps** — the password never leaves Secrets Manager.
- `BrokerSecurityGroupId` — security group to reference in ECS tasks to
  allow outbound traffic to the broker.

## Cost

| Type | Monthly cost (eu-west-1) |
|---|---|
| `mq.t3.micro` (tutorial) | ~$15 |
| `mq.m5.large` (production single) | ~$130 |
| `mq.m5.large` 3-AZ cluster | ~$400 |

## Notes

The broker accepts connections only from the VPC CIDR (`10.0.0.0/16`). For
ECS tasks to reach it, the tasks must have a security group that allows
outbound traffic to `BrokerSecurityGroupId` (configured automatically by the
`setup-cluster-ecs` step).
