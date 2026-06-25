# 12 — Build Order

Lépésről lépésre rebuild sorrend. Minden lépés végén futtass `npm run build`-et és nézd meg a routes/ alatt minden createFileRoute string egyezik a fájlnévvel.

## Step 0 — Foundation (scaffold)
1. `npm create` TanStack Start template (Vite 7 + React 19 + TS strict)
2. `bun add` a teljes dependency listára (lásd `11_TECH_IMPLEMENTATION.md`)
3. shadcn/ui inicializálás (`components.json` + `src/components/ui/` 50+ primitive)
4. `src/styles.css` — `08_ART_DIRECTION.md` tokens 1:1
5. `src/router.tsx`, `src/start.ts`, `src/server.ts` — TanStack Start standard
6. `src/routes/__root.tsx` — `<html><head><Meta /></head><body><AppShell><Outlet /></AppShell><Toaster /></body></html>`

## Step 1 — Shell
1. `src/assets/apibox-logo.svg`
2. `src/components/shell/app-sidebar.tsx` — 5 nav csoport, `07_UIUX_DESIGN.md` szerint
3. `src/components/shell/app-topbar.tsx` — breadcrumb + env + role + search
4. `src/components/shell/app-shell.tsx` — wrapper + `useScheduleTick()` (placeholder no-op egyelőre)
5. `src/components/common/primitives.tsx` — `Tag`, `StatusDot`, `Empty`, `SectionTitle`

## Step 2 — Domain types + permissions
1. `src/domain/types.ts` — másold 1:1 a `05_DATA_MODEL.md`-ből
2. `src/lib/permissions.ts` — `09_API_CONTRACT.md` szerint
3. `src/lib/utils.ts` — `cn`, `formatRelative`, `shortId`

## Step 3 — Store (skeleton)
1. `src/domain/store.ts` — Zustand + persist + minden collection üres array
2. Action stubok: minden public function neve és signature-e a `09_API_CONTRACT.md`-ből
3. `pushAudit` implementálva
4. `useShallow` szabály bevezetve (memo a komponens-szintekre)

## Step 4 — Seed + first routes (F01)
1. `src/domain/seed.ts` — default workspace + 1 owner member + 1 demo API + 1 endpoint
2. `src/routes/index.tsx` — egyszerű "Welcome" + KPI lista
3. `src/routes/catalog.tsx` — API card grid
4. `src/routes/settings.tsx` — workspace info + role impersonation + reset

## Step 5 — Import + Catalog (F02)
1. `src/domain/openapi-parser.ts` — js-yaml + JSON parse + weakness detector
2. `src/routes/import.tsx` — file/url upload, preview, commit
3. `src/routes/api.$apiId.tsx` — tabs (Endpoints/Schemas/Envs/Snapshots), Outlet for endpoint detail

## Step 6 — Endpoint Designer (F03)
1. `src/routes/api.$apiId.endpoint.$endpointId.tsx` — tabs Overview/Request/Responses/Scenarios/Mocks/Versions
2. JSON Schema text editor (controlled `<textarea>` + parse error banner)
3. `upsertEndpoint`, `upsertEndpointResponse` actions

## Step 7 — Scenarios + Execution (F05)
1. `src/routes/scenarios.tsx` — list + modal create/edit
2. `src/routes/environments.tsx` — env CRUD
3. `src/routes/prevalidation.tsx` — toggle list
4. `runEndpoint` + `runScenarioSuite` actions
5. `src/routes/execution.tsx` — run table + drawer with trace

## Step 8 — Mapping + Mock + Errors (F04 / F06)
1. `src/lib/transform-engine.ts`
2. `src/routes/mappings.tsx` — source/target picker + transform + live preview
3. `src/lib/mock-forge.ts` — schema → deterministic payload + coverage
4. `src/routes/mock-forge.tsx`
5. `src/routes/errors.tsx`
6. `src/routes/data-packs.tsx`

## Step 9 — Schema Diff + PDF (F07)
1. `src/lib/schema-diff.ts` — compute diff + impact
2. `src/lib/pdf-export.ts` — buildSchemaDiffPdf, buildFactsheetPdf
3. `src/routes/schema-diff.tsx` — pickers + side-by-side + impact + export buttons
4. Endpoint designer "Versions" tab → snapshot SchemaVersion

## Step 10 — Gateway Export + Smoke (F08)
1. `src/lib/gateway-export.ts` — Kong YAML
2. `runGatewaySmoke` action
3. `src/routes/gateway-smoke.tsx` — picker + history + drawer + PDF export
4. `src/routes/explorer.tsx` — graph stub + export button

## Step 11 — Schedules + Notifications (F09)
1. `src/lib/cron-eval.ts` — parseCron + nextRunFrom
2. `src/hooks/useScheduleTick.ts` — setInterval + getState
3. `runScheduleTick`, `runScheduleNow` actions + NotificationLogEntry
4. `src/routes/schedules.tsx` — list + create modal + drill-down

## Step 12 — Mapping Reviews + Lock (F10)
1. `requestMappingReview`, `addReviewNote`, `decideReview`, `setMappingLock`
2. Production lock enforcement in `upsertMapping`, `deleteMapping`
3. `src/routes/mapping-reviews.tsx`

## Step 13 — Audit (F11)
1. `togglePinAudit`, `pruneAudit`
2. `buildAuditPdf` + CSV builder inline
3. `src/routes/audit.tsx` — filter + pin + export

## Step 14 — Dashboard widgets (F12)
1. Replace `/` with 4 governance widgets

## Step 15 — OpenAI Control
1. `src/routes/openai.tsx` — CRUD list

## Step 16 — Test Connections (F13)
1. `src/domain/test-connections.ts` — JSONPlaceholder, PokeAPI, Open-Meteo, REST Countries
2. `loadTestConnections` action (merge-by-id, non-destructive)
3. Settings button + toast

## Step 17 — Settings polish (F14)
1. Retention sliders, autoTickEnabled toggle, tickIntervalSec slider
2. Impersonation: role select + actor input
3. "Reset workspace to seed" confirm dialog

## Step 18 — Polish + QA
1. Empty states everywhere
2. Toast on every mutation
3. Keyboard shortcuts (`G H`, `G C`, ...)
4. Tab title + meta in each route `head()`
5. Manual smoke: import → endpoint → scenario → mapping → mock → diff → gateway → smoke → schedule → tick → review → lock → audit → export

## Step 18 — API Explorer Console (F15)
1. `bun add js-yaml` (ha még nincs) és típusok.
2. `src/lib/spec-loader.ts` — `parseSpecString`, `normalizeSpecUrl`, `loadSpecFromUrl` (probe), `extractAuthDefaults`.
3. `src/lib/spec-diff.ts` — `diffSpecs`, `specFingerprint`, `SpecDiffReport`.
4. `src/routes/api-explorer.tsx` — 3-paneles layout: spec/endpoint browser (kategória chipek + tag+method fa) | Request Builder (params, body editor, schema hints, request preview, mock multi-status, Data Pack favorites) | Response panel (Body/Headers/Schema/Trace).
5. Auth panel auto-feltöltése `extractAuthDefaults` alapján; per-spec `apibox.apikeys.v1`.
6. Mock validation gate: `built.missing` + `built.bodyIssues` → `400 validation_failed`.
7. Watch & Diff: `apibox.watches.v1`, „Check Now" modal a `SpecDiffReport` megjelenítésével.
8. Catalog Import modal frissítés: `parseSpecString` (YAML + JSON + Postman), `normalizeSpecUrl`, `loadSpecFromUrl` használata; commit után `apibox.specs.v1`-be is ír.
9. Sidebar: „API Catalog \ Import" + „API Explorer" linkek.
10. Smoke: TMF manifest betölt → kategória „TMF" → egy GET végpont Mock 200 / Mock 400 (kötelező query törlésével) / Bearer key mentés / reload után key megmarad.

Részletek: `10_FEATURE_PROMPTS/F15_api_explorer_console.md`.
