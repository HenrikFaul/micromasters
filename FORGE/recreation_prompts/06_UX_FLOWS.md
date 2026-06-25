# 06 — UX Flows (Layer 21)

## UF1 — Import → Endpoint review → Scenario → Run
1. `/import` → upload OpenAPI YAML/JSON (or paste URL → fetched text)
2. ImportJob parsed → preview table of endpoints + weakness counts
3. "Commit import" → ApiAsset + Endpoints + SchemaObjects + ValidationIssues seeded
4. Toast → "Open API" → `/api/$apiId`
5. Endpoint list → click endpoint → `/api/$apiId/endpoint/$endpointId` (designer)
6. Designer tabs: Overview / Request / Responses / Scenarios / Mocks / Versions
7. "New scenario" → modal (kind, status, latency, body) → save
8. "Run" toolbar button → ExecutionRun appears in `/execution`
9. Trace drawer with steps + prevalidation results

## UF2 — Two-API mapping → live preview → review → lock
1. `/mappings` → "New mapping"
2. Pick source API+schema+field, target API+schema+field
3. Transform expression (`lowercase`, `concat($.first, ' ', $.last)`, `iso(date)`)
4. Live preview panel: source example → transform → target shape (validated)
5. Save (status=proposed, confidence auto-scored on type match)
6. `/mapping-reviews` → request review → reviewerRole select
7. Reviewer (impersonate via Settings) → add note → Approve
8. Owner → "Lock production" → `productionLock=true`
9. Editor tries to edit → toast "Denied (production locked)", audit `mapping.write.denied`

## UF3 — Schema version bump → diff → impact → PDF
1. Endpoint designer → "Versions" tab → "Snapshot current schema" (creates SchemaVersion)
2. Edit response schema → "Snapshot" again (v1.1)
3. `/schema-diff` → pick endpoint + v1.0 vs v1.1
4. Added / removed / changed-type fields shown
5. Impact: scenariosAffected + mappingsAffected listed
6. Buttons: "Export PDF" (jspdf), "Export JSON"

## UF4 — Gateway export + smoke
1. `/explorer` or `/catalog` → "Export gateway config" (Kong-style YAML)
2. Snapshot recorded
3. `/gateway-smoke` → pick environment + scenario subset → "Run smoke"
4. GatewaySmokeRun appended with per-scenario pass/fail + plugin checks
5. Click run → drawer with results, "Export smoke report" (PDF)
6. Smoke also schedulable (`/schedules` kind = gateway-smoke)

## UF5 — Schedule + tick + notify
1. `/schedules` → "New schedule" → name, cron ("every 5 min" | "daily 09:00" | "weekly mon 08:00"), scenarioIds, env, retentionDays, email/webhook + onSuccess/onFailure
2. Enable → nextRunAt computed
3. `useScheduleTick` runs every `settings.tickIntervalSec` (default 60s)
4. At due time → `runScenarioSuite` invoked → ScheduledRunRecord appended → NotificationLogEntry per channel
5. Drill-down: schedule row → last 10 records → click record → linked ExecutionRuns
6. "Run now" button → trigger="manual" record

## UF6 — Mock generate → coverage → inject
1. `/mock-forge` → pick endpoint + status → "Generate" (seed input)
2. MockArtifact appears with payload + valid/invalid + validation messages
3. Coverage report panel: `% covered`, uncoveredPaths list
4. "Bulk generate N" → batch up to 25
5. "Inject as response example" → adds to endpoint.responses[status].examples[] with label "mock-{seed}"

## UF7 — Audit filter / pin / export
1. `/audit` → table with date / actor / role / action / target / summary
2. Filter bar: actor select, action substring, date range (from/to)
3. Star icon per row → toggles `pinnedAuditIds` (pinned section at top)
4. "Export CSV" → current filter
5. "Export PDF" → current filter (jspdf)

## UF8 — Test connections one-click
1. `/settings` → "Load public test APIs" → merge 4 APIs (JSONPlaceholder, PokeAPI, Open-Meteo, REST Countries) + endpoints + scenarios + mappings + schedules
2. Toast: "Added X APIs, skipped Y already present"
3. Navigate to `/catalog` → see new APIs

## Edge cases (system-wide)
- Empty collections → "Get started" CTA cards, never blank pages
- Invalid JSON in schema editor → red banner, save disabled
- Unsupported OpenAPI feature (oneOf polymorphism deep) → flagged as `polymorphism_complexity` weakness, import continues
- Production lock + editor attempts edit → action returns silently, audit + toast
- Tick fires while another tick still running → skip, log notification "skipped"
- localStorage quota approach → run prune* immediately, toast warning
