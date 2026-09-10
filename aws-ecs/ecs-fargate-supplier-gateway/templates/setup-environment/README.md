# Stack: setup-environment

Creates the VPC, public and private subnets across two AZs, NAT Gateway, and
the **VPC endpoints** for ECR, CloudWatch Logs, Secrets Manager, and S3.

This is the step that materializes the article's network architecture:
all tasks run in **private subnets**, the only entry point is the ALB
that we'll add in the `setup-cluster-ecs` step, and the VPC endpoints prevent
every image pull or log push from going through the NAT gateway.

## Resources created

- **VPC** — `10.0.0.0/16` with DNS hostnames enabled
- **Subnets** — 2 public (10.0.1.0/24, 10.0.2.0/24) + 2 private
  (10.0.3.0/24, 10.0.4.0/24) in two different AZs
- **Internet Gateway** + public route table
- **NAT Gateway** with Elastic IP in the first public subnet
- **Interface VPC endpoints** for:
  - `ecr.api` — authentication to ECR
  - `ecr.dkr` — Docker image pulls
  - `logs` — push to CloudWatch Logs from tasks
  - `secretsmanager` — reading supplier credentials
- **Gateway VPC endpoint** for `s3` (route table-based, no hourly cost)
- **Security group** shared for the Interface-type VPC endpoints
  (accepts HTTPS from the VPC CIDR)

## Parameters

| Name | Description |
|---|---|
| `TemplateBucket` | S3 bucket with CF templates (output from `setup-buckets`) |
| `SuffissoName` | Prefix for resource names |
| `EnviromentTag` | `dev` / `stage` / `prod` |
| `ApplicationTag` | Application tag |

## Prerequisites

The `setup-buckets` stack must have been deployed and the output
`TemplatesBucketName` must have been copied into the `parameters.json`
file of this step (field `TemplateBucket`).

## Deploy

```bash
make deploy STEP_NAME=setup-environment
```

## Outputs

- `VpcId`, `VpcCidr`
- `PublicSubnet1`, `PublicSubnet2`
- `PrivateSubnet1`, `PrivateSubnet2`

These outputs are exported by name and consumed by subsequent steps
via `Fn::ImportValue`.

## Cost

| Resource | Monthly cost (eu-west-1) |
|---|---|
| NAT Gateway (1×) | ~$32 (hourly) + $0.045/GB processed |
| VPC endpoint Interface (4×) | ~$28 ($0.01/h × 730h × 4) |
| VPC endpoint Gateway (S3) | free |
| **Total network** | **~$60/month** |

Without the VPC endpoints, the same traffic would go through the NAT and the
same VPC would cost ~$85/month just for network. The savings are the lesson
"the real cost of ECS is not ECS, it's the network" from the article.
