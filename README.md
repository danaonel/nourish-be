# Nourish — Backend

AI Personal Nutritionist API. Spring Boot 3.3 (Java 17, Maven). Deploys independently of the frontend.

Implements the services from `APP-TECHNICAL-PLAN.md` as a single deployable for v1 (the plan explicitly favors "avoid over-fragmenting"): Profile, Meal Planning, Logging, Context, Notification, and an AI Orchestration service where all persona behavior and safety guardrails are enforced in code.

## Run locally (no container)

```bash
mvn spring-boot:run
# or
mvn -DskipTests package && java -jar target/nourish-backend-1.0.0.jar
```

Starts on `http://localhost:8080`. Uses an in-memory store by default — no database required.

### API documentation (Swagger UI)

Once running:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

The docs are auto-generated from the controllers by springdoc-openapi. (Note: the UI lives at `/swagger-ui.html`, not under `/api`.)

## Run with Podman

```bash
podman build -t nourish-backend:local .
podman run -d --name nourish-backend -p 8080:8080 \
  -e FRONTEND_ORIGINS=http://localhost:3000 \
  nourish-backend:local
```

(These are standard OCI build files; `docker build`/`docker run` work identically if you switch tools.)

## Configuration (environment variables)

| Var | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `FRONTEND_ORIGINS` | `http://localhost:3000` | Comma-separated CORS allow-list for the separately-deployed PWA |
| `ANTHROPIC_API_KEY` | _(empty)_ | If set, enables live LLM meal planning; otherwise a deterministic local planner is used |
| `ANTHROPIC_MODEL` | `claude-3-5-sonnet-latest` | Model id when the LLM is enabled |

## AI / Claude integration

`AnthropicClient` calls the [Anthropic Messages API](https://platform.claude.com/docs/en/api) (`POST /v1/messages`) using the JDK HTTP client — no extra dependency.

- **Enable it:** set `ANTHROPIC_API_KEY`. `GET /api/meta` then reports `"llmEnabled": true`.
- **Where it's used:** meal-plan curation (`AiOrchestrationService`) and the coaching flows (`CoachingService`, craving + pantry).
- **Safety boundary:** the LLM only *reorders* a candidate pool that has already passed the deterministic `ConstraintFilter`; the model cannot introduce a food the user excluded. LLM meal picks and the pantry suggestion are re-checked against hard constraints (allergies, intolerances, ethical exclusions) before being returned.
- **Graceful fallback:** if the key is absent, the request fails, times out, or the response can't be parsed, every path falls back to the deterministic local planner/templates — the app always works. Generated plans are labelled `source: AI_GENERATED` (Claude curated) or `LOCAL_PLANNER` (fallback).
- **Model:** `ANTHROPIC_MODEL`, default `claude-3-5-sonnet-latest`.

**Setting the key without putting it on the command line.** The app loads a local `.env` file at startup (via `spring-dotenv`), so you can keep the key out of your shell history and global profile:

```bash
cp .env.example .env       # then edit .env and set ANTHROPIC_API_KEY=sk-ant-...
mvn spring-boot:run        # key is picked up automatically; nothing on the command line
```

`.env` is gitignored. Real OS environment variables still take precedence over the file, so in production you set `ANTHROPIC_API_KEY` as a normal env var / secret and don't ship a `.env` at all.

Or, if you prefer a one-off inline value:

```bash
ANTHROPIC_API_KEY=sk-ant-... mvn spring-boot:run
```

## Auth seam

Requests carry the user identity in the `X-User-Id` header (defaults to `demo-user`). In production a managed OAuth2/OIDC provider validates a JWT at the gateway and forwards the subject — swap the `CurrentUser` bean for a JWT-subject resolver (`APP-TECHNICAL-PLAN.md` §3.3).

## Authentication (AWS Cognito)

The backend is an OAuth2 resource server. Every `/api/**` request (except the public paths below) must carry a valid Cognito access token: `Authorization: Bearer <jwt>`. The signature is verified against the pool's JWKS, and the user id is the token's `sub` claim — it cannot be spoofed by the client.

- **Pool:** `us-west-1_7xcWXlLGK` (region `us-west-1`). Configured via `COGNITO_ISSUER_URI` / `COGNITO_JWKS_URI` (defaults point at this pool).
- **Optional client_id check:** set `COGNITO_CLIENT_ID` to reject tokens not issued to your app client (Cognito access tokens carry `client_id`, not `aud`).
- **Public paths:** `/api/meta`, `/actuator/health`, `/actuator/info`, and the Swagger/OpenAPI endpoints.
- **Local dev without Cognito:** set `SECURITY_ENABLED=false`. JWT enforcement is bypassed and identity falls back to the `X-User-Id` header. **Never use this in a real deployment.**

| Var | Default | Purpose |
|---|---|---|
| `SECURITY_ENABLED` | `true` | `false` disables JWT auth (dev only) |
| `COGNITO_ISSUER_URI` | pool issuer | Token issuer to validate |
| `COGNITO_JWKS_URI` | pool JWKS | Signing keys |
| `COGNITO_CLIENT_ID` | _(empty)_ | Optional: required `client_id` on tokens |

## Persistence

The `Store<T>` abstraction has two implementations:

- **In-memory (default):** zero infrastructure, data lost on restart. Good for local dev and demos.
- **MongoDB (`mongo` profile):** each user's data persists, keyed by `userId`, with indexes on `userId` (and `(userId, timestamp)` for logs).

Enable Mongo:

```bash
SPRING_PROFILES_ACTIVE=mongo MONGODB_URI=mongodb://localhost:27017/nourish mvn spring-boot:run
```

Run a local MongoDB with Podman: `podman run -d --name mongo -p 27017:27017 docker.io/library/mongo:7`. Collections created: `profiles`, `recipes`, `mealPlans`, `groceryLists`, `logs`, `notificationPrefs`. Per DP-1, sensitive fields (medical/medication) should additionally be encrypted (client-side field-level encryption or app-layer with a KMS) before production.

## API surface (all under `/api`)

| Method & path | Requirement | Notes |
|---|---|---|
| `GET /meta` | — | App info, whether the LLM is wired |
| `GET/PUT /profile` | FR-1..FR-8, FR-22 | Persona-defining profile; applies recovery guardrails on save |
| `POST /meal-plan/generate`, `GET /meal-plan` | FR-9, FR-10 | Weekly plan; each meal carries a `whyItFits` rationale |
| `POST /grocery-list/generate`, `GET/PUT /grocery-list` | FR-11 | Grouped by section, respects preferred sourcing |
| `POST /logs`, `POST /logs/batch`, `GET /logs`, `GET /logs/trends` | FR-18, FR-20 | Idempotent writes via `clientId`; trend rollups; SG-3 concern nudge returned with each write |
| `GET/POST /recipes`, `POST /recipes/{id}/confirm` | FR-5, FR-27 | "What works" library |
| `POST /coach/craving`, `POST /coach/pantry`, `GET /coach/resources` | FR-17, FR-25, FR-13a, SG-3/SG-5 | Guided craving flow, pantry suggestions, support resources |
| `POST /context/menu-guidance` | FR-14/15/16 | "I'm at X" guidance; excludes hard-constraint items, suppresses trigger items |
| `GET/PUT /notifications/prefs`, `POST /notifications/subscribe` | FR-24, NFR-3, SG-2 | Per-type opt-in; calorie notifications require `?recoveryAck=true` for recovery users |
| `GET /account/export`, `DELETE /account` | DP-2 | Full data export and hard delete |

## Safety guardrails (enforced in code, not prompts)

- `ConstraintFilter` — hard exclusions (allergies, intolerances, **ethical constraints**, SG-6) are checked deterministically; any candidate mentioning an excluded term is rejected before it can be shown.
- `PersonaPrompts` — per-goal system prompts; the RECOVERY prompt avoids calorie/restriction framing (SG-1, SG-2).
- `SafetyService` — non-diagnostic pattern detection returning support resources, never more diet content (SG-3).
- `ProfileService` / `NotificationService` — RECOVERY defaults calorie & weight logging OFF; calorie notifications can't be enabled without explicit acknowledgment (SG-2).

## Build

```bash
mvn -DskipTests package
```
