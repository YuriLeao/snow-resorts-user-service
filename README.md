# snow-resorts-user-service

User microservice for Snow Resorts: profiles, **avatar upload via S3 presigned URLs**
(MinIO locally), friendships and privacy settings.

- **Port:** 8082
- **DB schema:** `users`
- **Shared libs:** `com.snowresorts:security-lib` (from GitHub Packages)
- **Object storage:** S3 (prod) / MinIO (local) via an `ObjectStorage` port

## Build & test

Requires a `github` server credential in `~/.m2/settings.xml` (see
[`settings.xml.example`](settings.xml.example)) to resolve the shared libraries.

```bash
./mvnw clean verify
./mvnw spring-boot:run    # `local` profile against the local Docker stack
```

Bring up Postgres/Redis/MinIO from [`snow-resorts-infra`](https://github.com/yurileao/snow-resorts-infra) (`make dev`).

## CI/CD

See [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml). Requires repo secret
`AWS_DEPLOY_ROLE_ARN`.
