# Stack: setup-secrets

Creates the secrets in **AWS Secrets Manager** for the **three suppliers**
integrated in the `hotel-supplier-gateway` (POC): Booking.com, Expedia,
HotelBeds. The Medium article also mentions Amadeus and Simple Booking, but
the reference gateway only has the three providers above; those remain
excluded from the tutorial.

The values are retrieved by ECS tasks at startup via the `secrets` key
of the task definition. The article explicitly states: *"Never bake
secrets into Docker images"*.

## Resources created

- `AWS::SecretsManager::Secret` × 3, one per supplier, with JSON keys:
  - `supplier/booking-com` → `{"api_key":"..."}`
  - `supplier/expedia` → `{"api_key":"...","bearer_token":"..."}`
  - `supplier/hotelbeds` → `{"api_key":"...","shared_secret":"..."}`

The task definitions in `setup-cluster-ecs` read these secrets with the
ARN + JSON key syntax, for example:

```yaml
Secrets:
  - Name: BOOKING_COM_API_KEY
    ValueFrom: !Sub '${BookingComSecretArn}::api_key'
```

## Parameters

| Name | Description |
|---|---|
| `SuffissoName` | Prefix for secret names |
| `EnviromentTag` | `dev` / `stage` / `prod` |
| `ApplicationTag` | Application tag |
| `BookingComApiKeySecret` | Booking.com Demand API key |
| `ExpediaApiKeySecret` | Expedia Rapid API key |
| `ExpediaBearerTokenSecret` | Expedia Rapid API bearer token |
| `HotelBedsApiKeySecret` | HotelBeds Booking API key |
| `HotelBedsSharedSecretSecret` | HotelBeds shared secret |

All supplier secrets have the default `change-me-before-deploy` as a
placeholder. To be updated after deployment.

## Updating a secret after deployment

The template parameters are only the initial value. To put in the real
credentials, update via the CLI:

```bash
aws secretsmanager put-secret-value \
  --secret-id gt/supplier-gateway/supplier/booking-com \
  --secret-string '{"api_key":"your-real-api-key"}'
```

To rotate a key: same command, new value. ECS picks up the new version
on the next task restart (or immediately if the task definition has
a `:staging-label`).

## Outputs

- `SecretsPrefix` — common prefix to build ARNs on the ECS side.
- `BookingComSecretArn`, `ExpediaSecretArn`, `HotelBedsSecretArn` — one
  per supplier, to pass to the `setup-cluster-ecs` step.

## Cost

~$0.40/secret/month + $0.05 per 10,000 API calls. For 3 secrets and low
traffic it's a figure that stays under a dollar per month.

## Security notes

- The parameters are `NoEcho: true` and `Type: String` with placeholder
  defaults. Real credentials **must never** go in the committed parameter
  files.
- The `<Supplier>SecretArn` output is exported to be consumed by subsequent
  steps without exposing the secret value.
- The task roles in `setup-cluster-ecs` receive the permission
  `secretsmanager:GetSecretValue` with `Resource` scoped to the 3 specific
  ARNs.
