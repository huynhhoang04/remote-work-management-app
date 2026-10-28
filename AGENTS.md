# AGENTS.md

Instructions for AI coding agents (and humans) working in this repository. Read this file first,
then the relevant docs in `docs/` before writing code.

## 1. What this project is

**Topic 21 — Remote work management & tracking app** (Mobile App course capstone):
an Android app for individuals/small teams: project + task management (Kanban, timeline),
deadline reminders, productivity stats, per-project chat, and push notifications. The backend is a
**Spring Boot modular monolith** serving the mobile app (other clients may be added later).

The scope has been **reviewed and trimmed** per the instructor's feedback: keep features that
"belong in a mobile app", cut features that are "desktop-oriented". The concrete keep/cut list is
in `docs/04-thiet-ke-backend.md` section 1 — **do not** reintroduce cut features.

## 2. Hard constraints (do not violate)

| Constraint | Value |
|---|---|
| Backend | **Java 21 LTS + Spring Boot 3.3.x** |
| Build | **Maven** (use `./mvnw`) |
| Architecture | **Modular monolith** — one app, one PostgreSQL. NO microservices, no message broker, no API gateway |
| Database | **PostgreSQL 16**, schema managed by **Flyway** (`ddl-auto: validate`) |
| Auth | JWT (15' access + rotating refresh), RBAC with 4 project roles |
| Files | **Object storage** (Supabase Storage via REST) with local-disk fallback for dev/test; metadata in PostgreSQL |
| Push | Firebase Cloud Messaging via `firebase-admin`; **no-op** when `FCM_ENABLED=false` |
| Mobile | **React Native — Expo (managed) + TypeScript** (no Kotlin/Compose) — see `docs/plan-app/01-phase-1.md` |

**Deliberately excluded:** Redis, message broker, workspaces/organizations, WebRTC
video calls, chat typing/presence, two-way offline sync, import/export, advanced audit features.
Do not reintroduce them without a decision recorded in `docs/`. MinIO/S3 may replace Supabase
only through the `StorageService` interface (see `docs/plan-deploy/02-dich-vu-ngoai.md`).

## 3. Repository map

```
backend/                     Spring Boot modular monolith (Maven)
├─ src/main/java/com/example/remoteapp/
│   ├─ config/               Security, WebSocket, OpenAPI
│   ├─ common/               BaseEntity, ErrorCode, AppException, GlobalExceptionHandler, SecurityUtils
│   ├─ auth/                 JWT, refresh token, register/login/forgot password
│   ├─ user/                 profile, FCM devices
│   ├─ project/              projects, members, RBAC (ProjectPermissionService), invite links
│   ├─ task/                 task, assignee, label, checklist, comment, attachment
│   ├─ schedule/             personal schedule events
│   ├─ stats/                today, personal stats, project dashboard (aggregate SQL)
│   ├─ notification/         notification center, FCM push, deadline reminder scheduler
│   ├─ chat/                 GROUP/DIRECT conversations, messages, read receipts, STOMP
│   ├─ file/                 StorageService (local/Supabase), file metadata
│   ├─ audit/                audit log (leaf module)
├─ src/main/resources/db/migration/   Flyway V1–V6
└─ src/test/java/            unit tests (Mockito) + integration tests (Testcontainers)
bruno/                       Bruno workspace (OpenCollection YAML) — 103 requests, 65 endpoints
docs/                        project documentation (see section 4)
infra/                       dev docker-compose (PostgreSQL, mailpit)
render.yaml                  Render Blueprint — deploy phase 1 (Docker, free)
.github/workflows/           backend CI (./mvnw test) + deploy job (Render hook)
.agents/skills/              mobile app design skills
mobile/                      React Native app (Expo + TypeScript) — builds an Android APK
```

## 4. Documentation — what to read first

| Priority | Document | Use for |
|---|---|---|
| 1 | `docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md` | Feature specs + implementation (FR codes, business rules) |
| 2 | `docs/plan-backend/02-danh-sach-api.md` | API contract + error codes |
| 3 | `docs/plan-backend/01-kien-truc-va-stack.md` | Architecture, stack, dependency rules, conventions |
| 4 | `docs/04-thiet-ke-backend.md` | Keep/cut scope, ERD, RBAC, roadmap |
| — | `docs/plan-backend/04-tong-ket-trien-khai-backend.md` | **Handover**: implemented features, file map, 65 endpoints, tests, env |
| — | `docs/plan-backend/05-schema-co-so-du-lieu.md` | **DB schema**: ERD 19 tables + column/index/FK catalog + query examples |
| — | `docs/plan-deploy/01-ci-cd-phase-1.md` | Deploy: Render (Docker) + Neon, deploy hook sau CI |
| — | `docs/plan-deploy/02-dich-vu-ngoai.md` | Supabase Storage + Firebase FCM setup, alternatives, troubleshooting |
| — | `docs/plan-deploy/03-deploy-phase-2.md` | Deploy runbook phase 2: env mới, thứ tự, verify, rollback |
| — | `docs/plan-app/01-phase-1.md` | Mobile phase 1: Expo + React Native + TypeScript, screens/design system |
| — | `docs/01–03`, `docs/baocao-*` | Context/original drafts — partially superseded by `docs/04` |

Docs are written in Vietnamese (project requirement). When an API/entity/table changes, update the
matching doc **in the same commit**.

## 5. Backend conventions

- **Layering:** `controller → service → repository → entity`. No business logic in controllers;
  **never** return entities — always DTOs (`record`).
- **Module dependency rules:** never access another module's repository (exceptions: `stats` and
  `notification` reminder reads use aggregate queries). Call other modules via their public services.
  Details: `docs/plan-backend/01` §4.2.
- **Validation:** Jakarta annotations on DTOs (`@NotBlank`, `@Size`, `@Email`...); services re-check
  business rules (uniqueness, permissions, state transitions...).
- **Errors:** throw `AppException(ErrorCode, message)`; `GlobalExceptionHandler` renders the standard
  format (`timestamp/status/code/message/path`). Services never throw other exception types.
- **Authorization:** every project-scoped operation must go through
  `ProjectPermissionService.requireRole/requireMember` (enforced on the backend — never trust the UI).
- **Current user:** controllers read `SecurityUtils.currentUserId()` and pass `actorId`
  **explicitly** into services (services never touch SecurityContext → easy to unit test).
- **Audit:** important actions call `AuditService.record(...)` inside the transaction.
- **Entities:** extend `BaseEntity` (UUID id, createdAt, updatedAt), `@Enumerated(EnumType.STRING)`,
  soft delete via `deletedAt`; Lombok `@Getter/@Setter`; manual entity ↔ DTO mapping (no MapStruct).
- **Constructor injection:** write **explicit constructors**; `@RequiredArgsConstructor` is banned in
  this repo. Reason: the VSCode Java extension ships its own Lombok agent (1.18.39) that mis-compiles
  Lombok-generated constructors, reports one error per dependency in the editor and writes broken
  class files into `target/classes`, which then makes `./mvnw spring-boot:run` die with
  `java.lang.Error: Unresolved compilation problem`. Lombok is still fine for entity accessors.
- **Javadoc:** every public method has Javadoc plus a `@reference` line pointing to the matching
  section of `docs/plan-backend/03` (or `docs/04`) — the codebase currently follows this.
- **Language:** English identifiers; Vietnamese comments/Javadoc/error messages.
- **Config:** environment variables (`DB_URL`, `JWT_SECRET`, `MAIL_*`, `STORAGE_*`, `FCM_*`,
  `APP_TIMEZONE`, `REMINDER_*`...); never commit secrets.

## 5b. Mobile conventions (Expo + React Native + TypeScript)

- **TypeScript strict**; API types live in `mobile/src/types`, zod schemas in `mobile/src/schemas`
  mirror the backend rules (email, password 8–72, project name 1–100, invite code 12 chars).
- **Server state only through TanStack Query** (queries/mutations); no ad-hoc `useEffect` fetching.
- **HTTP only through `mobile/src/api/client.ts`** (axios + auth/refresh interceptors). Never call
  axios/fetch directly from screens.
- **UI**: React Native Paper (M3) + tokens in `mobile/src/theme`; no hardcoded colors/spacing.
- **Tokens** in `expo-secure-store` (never AsyncStorage, never in source).
- **Screens**: Expo Router files under `mobile/src/app`; every data screen renders the shared
  `StateView` (loading/empty/error+retry). All user-facing text in Vietnamese, in
  `mobile/src/utils/constants.ts`.
- **Navigation**: route guards in `(auth)`/`(app)` layouts; 401/refresh-failure → replace to login.

## 6. Database & migrations

- A new table means a new Flyway file (`V3__*.sql`, `V4__*.sql`...). **Never** edit a migration that
  is already committed.
- Entity ↔ schema must match exactly (`ddl-auto: validate`): column types, names, NOT NULL.
- Add indexes for heavily queried columns (`project_id`, `deadline`, `user_id`...). Foreign keys
  reference `users(id)`/`projects(id)` in the same database.

## 7. Testing (mandatory)

- Unit test services with **Mockito** for every business rule (authorization, validation,
  idempotency...).
- Integration tests use `@SpringBootTest` + MockMvc + **Testcontainers PostgreSQL**:
  `@Testcontainers(disabledWithoutDocker = true)` — self-skips without Docker; CI still runs them.
- Before you are done: `cd backend && ./mvnw test` must be **green**. API changes require
  updated/new tests.
- Mobile: `cd mobile && npx tsc --noEmit && npm test` must be **green** (Jest + RNTL; zod schemas,
  error mapping, refresh interceptor, key components).

## 8. Main commands

```bash
cd backend
./mvnw clean verify        # build + all tests
./mvnw test                # tests only
./mvnw spring-boot:run     # run locally (needs PostgreSQL)

# local infrastructure (PostgreSQL + mailpit)
docker compose -f infra/docker-compose.yml up -d

# Bruno API suite (103 requests, 65 routes) — local backend on :8080
cd bruno
./run.sh --bail                 # full flow
./run.sh --tags tasks           # one module

cd ../mobile
npm ci
cp .env.example .env           # EXPO_PUBLIC_API_URL (mặc định URL Render)
npx expo start                 # Expo Go / emulator
npx tsc --noEmit && npm test   # typecheck + unit/component tests
npx expo run:android           # build APK local (cần Android Studio/SDK)
```

CI: `.github/workflows/backend-ci.yml` runs `./mvnw -B test` on JDK 21 for every push/PR;
`.github/workflows/mobile-ci.yml` runs `npx tsc --noEmit` + `npm test` for `mobile/**` changes.

Troubleshooting: if the app fails with `java.lang.Error: Unresolved compilation problem` while
`./mvnw test` is green, shut down the running app, fix the error the IDE reports, then
`./mvnw clean` before starting again — the IDE build wrote broken classes into `target/classes`.

## 9. How to work here

1. **Read before writing.** Read the docs from section 4 and the files you are about to change.
2. **Follow the plan.** Backend phases 1–6 are implemented (handover in
   `docs/plan-backend/04-tong-ket-trien-khai-backend.md`); next is the mobile app
   (`docs/plan-app/01-phase-1.md`). Requests that contradict the plan → ask or state the reason.
3. **Keep scope tight.** Only do what was asked — no speculative abstractions, no out-of-scope
   refactors, no new infrastructure/config without a need.
4. **Validate before declaring done.** Build + tests must pass; fix what you broke.
5. **Keep `main` green.** Never commit a red build or failing tests.
6. **Commits:** Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`...); never commit `target/`,
   `uploads/`, or secrets.

## 10. Anti-patterns

- Bypassing `ProjectPermissionService` and querying `project_members` directly.
- Validating only in the UI/DTO without checking business rules in the service.
- Returning entities from controllers, or using `ddl-auto: update` instead of Flyway.
- Editing/deleting an already-committed migration file.
- Splitting services into microservices, adding Redis/Kafka "to be professional".
- Bypassing `StorageService` to call Supabase/MinIO/S3 directly from services.
- Reintroducing features cut in `docs/04` §1.2 without a recorded decision.
- Public methods without Javadoc/`@reference` (breaks the project standard).
- Committing `.env`, a real `JWT_SECRET`, or real user data.
- Mobile: storing tokens in AsyncStorage, calling `axios` directly in screens, hardcoding colors/spacing
  instead of `src/theme`, or skipping the `StateView` error/empty states.
