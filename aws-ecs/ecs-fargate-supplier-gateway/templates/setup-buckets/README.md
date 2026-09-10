# Stack: setup-buckets

Creates the S3 bucket that hosts the CloudFormation templates referenced by
other stacks via `TemplateURL`. It is the first step because all subsequent
ones (except `setup-environment`) upload their nested templates to this bucket
before deployment.

## Resources created

- `AWS::S3::Bucket` — `cf-templates-<env>` with versioning, AES-256 encryption,
  full public access block, and TLS-only policy.
- `AWS::S3::BucketPolicy` — denies any non-HTTPS request.

## Parameters

| Name | Description |
|---|---|
| `SuffissoName` | Prefix for the bucket name (e.g. `gt`) |
| `EnviromentTag` | `dev` / `stage` / `prod` |
| `ApplicationTag` | Application tag |

## Deploy

```bash
# From CloudShell, after cloning the repo
make deploy STEP_NAME=setup-buckets
```

## Outputs

- `TemplatesBucketName` — bucket name to use in subsequent steps.

## Cost

Empty S3 bucket: ~$0. Storage and requests are only billed starting from the
first template upload (`make upload`). Negligible for a tutorial.
