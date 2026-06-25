# 03 — Product Spec (Layer 11)

## Entities (lifecycle + key fields)

Lásd `05_DATA_MODEL.md` minden TypeScript interfészért. Itt csak az üzleti lényeg.

| Entity | Owner | Key fields | Lifecycle |
|---|---|---|---|
| Workspace | system | id, name, slug | persist |
| BusinessDomain | workspace | id, name, color | persist |
| ApiAsset | workspace | name, slug, version, lifecycle, readiness | draft → review → active → deprecated |
| Endpoint | api | path, method, parameters, requestBodySchema, responses[] | persist |
| Parameter | endpoint | name, in (path/query/header/cookie), required | persist |
| EndpointResponse | endpoint | statusCode, schemaText, examples[] | persist |
| Scenario | endpoint | kind, statusCode, latencyMs, responseBody | persist |
| DataPack | endpoint/schema/global | scope, payload, version | persist |
| ErrorDefinition | workspace | code, httpStatus, category, payloadTemplate | persist |
| FieldMapping | workspace | source/target apiId+schemaName+field, transform, status | proposed → approved/rejected |
| MockArtifact | endpoint | seed, payload, valid | persist |
| SchemaVersion | endpoint | version, requestSchemaText, responsesText | append-only |
| ExecutionRun | endpoint | mode, request, response, trace[], passed | append-only, pruned by retention |
| GatewaySmokeRun | workspace | gatewayProfile, results[], passed | append-only |
| ScheduledRun | workspace | cron, scenarioIds, notify, retentionDays | enabled/disabled |
| ScheduledRunRecord | schedule | at, durationMs, passed/failed counts | append-only |
| NotificationLogEntry | schedule | channel, status, detail | append-only |
| MappingReview | mapping | status (pending/approved/rejected/changes_requested), notes[], productionLock | append-only |
| Snapshot | api | kind (factsheet/run_report/gateway_export), dataUrl | append-only |
| AuditEntry | workspace | actor, actorRole, action, target, before/after | append-only, pruned |
| WorkspaceMember | workspace | name, email, role | persist |
| WorkspaceSettings | workspace | retention days, autoTickEnabled, tickIntervalSec | persist |

## Features (F1..F14)

Minden feature külön részletes prompt: `10_FEATURE_PROMPTS/F{NN}_*.md`.

| ID | Name | MVP | User story | Acceptance (Given/When/Then) |
|---|---|---|---|---|
| **F01** | Workspace + RBAC + Audit | ✓ | As any user, I want my role to gate destructive actions so accidental changes are blocked. | Given role=viewer, when I try to upsert a scenario, then the action is no-op and audit logs `*.write.denied`. |
| **F02** | API Catalog + Import Studio | ✓ | As a designer, I want to import OpenAPI YAML/JSON and see weaknesses listed. | Given a valid OpenAPI 3.0 file, when I commit import, then ApiAsset+Endpoints+SchemaObjects exist and ValidationIssues are seeded. |
| **F03** | Endpoint Designer | ✓ | As a designer, I want to edit request/response JSON Schema per endpoint with live JSON Schema validation. | Given an endpoint, when I edit responses[200].schemaText to invalid JSON, then a banner shows parse error and save is blocked. |
| **F04** | Mapping Studio + live preview | ✓ | As an integrator, I want drag-style field mapping with transform expressions and a live preview of the transformed payload. | Given source schema with field `user.email` and target with `contact.address`, when I bind them with `lowercase` transform, then preview shows the source example transformed. |
| **F05** | Scenarios + Execution Console | ✓ | As an ops user, I want to run a scenario or full suite against an env and see trace. | Given a scenario, when I "Run", then an ExecutionRun appears with trace steps and prevalidation results. |
| **F06** | Mock Forge | ✓ | As a designer, I want deterministic mock payloads per endpoint+status with schema validation and coverage report. | Given an endpoint with response schema, when I generate seed=42 status=200, then MockArtifact.valid=true and uncoveredPaths is listed. |
| **F07** | Schema Diff + PDF/JSON export | ✓ | As an owner, I want a diff between two SchemaVersions with an impact report on scenarios and mappings, exportable as PDF/JSON. | Given v1.0 and v1.1, when I open diff, then breaking/non-breaking is classified and PDF download contains the impact summary. |
| **F08** | Gateway Export + Smoke | ✓ | As an ops, I want to export a Kong-style config and run a smoke test against it. | Given selected scenarios, when I run smoke, then per-scenario pass/fail + plugin checks appear in GatewaySmokeRun. |
| **F09** | Scheduled Runs + notifications | ✓ | As an ops, I want cron-scheduled suite runs with email/webhook notifications and retention. | Given enabled schedule with cron "every 5 min", when tick fires, then ScheduledRunRecord + NotificationLogEntry are appended. |
| **F10** | Mapping Reviews + production lock | ✓ | As an owner, I want a review workflow with notes and an optional production lock. | Given a mapping with productionLock=true, when an editor calls upsertMapping, then the action is denied and audited. |
| **F11** | Audit Log (filter/pin/export) | ✓ | As an auditor, I want to filter by date/actor/action, pin important entries, export CSV/PDF. | Given 100 audit entries, when I filter date range last-7-days, then only matching rows render; pinned rows stay at top. |
| **F12** | Dashboard widgets | ✓ | As any role, I want to see latest smoke, pending reviews, upcoming schedules, audit feed on landing. | Given seed data, when I open `/`, then 4 widgets render with non-empty content. |
| **F13** | Test Connections | ✓ | As any user, I want a one-click load of 4 public test APIs so I can exercise every feature. | Given empty workspace, when I click "Load public test APIs", then 4 APIs + endpoints + scenarios + mappings + schedules are merged in. |
| **F14** | Settings (retention/impersonation/reset) | ✓ | As an owner, I want to tune retention, impersonate a role, and reset to seed. | Given autoTickEnabled=true, when tickIntervalSec=10, then useScheduleTick polls every 10s. |

## User Flows (összefoglaló — részletes lépések `06_UX_FLOWS.md`)

UF1 Import → Endpoint review → Scenario create → Run  
UF2 Two-API mapping → live preview → request review → approve → lock production  
UF3 Schema version bump → diff → impact report → export PDF  
UF4 Gateway export → smoke test → fix failing scenario → re-run  
UF5 Schedule create → wait for tick → see record → notification log  
UF6 Mock generate → coverage report → inject as response example  
UF7 Audit filter → pin → CSV export

## Permissions matrix

Lásd `src/lib/permissions.ts` (idézve a `09_API_CONTRACT.md`-ben). Összefoglaló:

| Action | owner | editor | operator | viewer |
|---|:-:|:-:|:-:|:-:|
| scenario.write/delete | ✓ | ✓ | – | – |
| scenario.run | ✓ | ✓ | ✓ | – |
| mapping.write/approve | ✓ | ✓ | – | – |
| mapping.lock.production | ✓ | – | – | – |
| endpoint.write | ✓ | ✓ | – | – |
| import.commit | ✓ | ✓ | – | – |
| gateway.export | ✓ | – | – | – |
| gateway.smoke | ✓ | ✓ | ✓ | – |
| schedule.write | ✓ | ✓ | – | – |
| schema.version.write | ✓ | ✓ | – | – |
| role.manage | ✓ | – | – | – |
| audit.view | ✓ | ✓ | ✓ | ✓ |

## Build priority order

F01 → F02 → F03 → F05 (alap loop) → F04 → F06 → F07 → F08 → F09 → F10 → F11 → F12 → F13 → F14
