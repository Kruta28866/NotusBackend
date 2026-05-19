# Deploying Notus Backend

## Render

Recommended backend host: Render Web Service with Docker.

Use the `NotusBackend` directory as the Render project root. The included `render.yaml` deploys the Spring Boot app from the Dockerfile and checks `/api/health`.

Set these Render environment variables:

```text
DB_PASSWORD=your-supabase-db-password
GEMINI_API_KEY=your-gemini-api-key
AUTH_JWT_SECRET=replace-with-a-long-random-secret
QR_HMAC_SECRET=replace-with-a-long-random-secret
CORS_ALLOWED_ORIGIN_PATTERNS=https://your-vercel-app.vercel.app
APP_FRONTEND_BASE_URL=https://your-vercel-app.vercel.app
INITIAL_TEACHER_CODE=optional-first-teacher-code
JPA_SHOW_SQL=false
SENTRY_DSN=your-backend-sentry-dsn
SENTRY_ENVIRONMENT=production
SENTRY_TRACES_SAMPLE_RATE=0.1
```

Set SMTP variables only when real invitation emails should be sent:

```text
SMTP_HOST=
SMTP_PORT=587
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_AUTH=true
SMTP_STARTTLS_ENABLE=true
SMTP_STARTTLS_REQUIRED=true
MAIL_FROM=
MAIL_FROM_NAME=Notus
```

Render injects `PORT`; `application.properties` maps it to `server.port` with a local default of `8080`.

After the backend is live:

1. Copy its Render URL.
2. Put it into Vercel as `VITE_API_URL`.
3. Replace `CORS_ALLOWED_ORIGIN_PATTERNS` and `APP_FRONTEND_BASE_URL` with the final Vercel URL.
4. Redeploy both services.
