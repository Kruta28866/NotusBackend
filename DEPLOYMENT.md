# Deploying Notus Backend

## Render

Use the `NotusBackend` directory as the Render project root.

The included `render.yaml` deploys the Spring Boot app from the Dockerfile and checks `/api/test`.

Set these Render environment variables:

```text
DB_PASSWORD=your-supabase-db-password
GEMINI_API_KEY=your-gemini-api-key
QR_HMAC_SECRET=replace-with-a-long-random-secret
CORS_ALLOWED_ORIGIN_PATTERNS=https://your-vercel-app.vercel.app
```

Render injects `PORT`; `application.properties` maps it to `server.port` with a local default of `8080`.

After the backend is live, copy its Render URL into the frontend's Vercel `VITE_API_URL`.
