# F05 — Scenarios + Execution Console

## Goal
Scenarios are pre-canned responses per endpoint. Run a single scenario or the whole suite against an environment in mock or simulated-live mode. Capture trace.

## Files
- `src/routes/scenarios.tsx`
- `src/routes/environments.tsx`
- `src/routes/prevalidation.tsx`
- `src/routes/execution.tsx`
- store: `runEndpoint`, `runScenarioSuite`

## Scenario kinds
`happy_path | partial_success | validation_error | auth_error | rate_limit | timeout | downstream_failure | ai_invalid_json | ai_moderation_block`

## `runEndpoint(params)` algorithm
1. Resolve endpoint + env + scenario.
2. Build request: method, baseUrl + path, fixedHeaders + redacted markers, body from scenario or override.
3. Trace step: "Request built".
4. Prevalidation: for each enabled rule, evaluate → pass/fail + message.
5. Trace step: "Prevalidation passed/failed".
6. Mock mode: response = scenario.responseBody + statusCode + latencyMs (simulate with `await new Promise(setTimeout)`).
7. Live mode: same as mock (no real HTTP); mark `mode="live"` for UI.
8. Trace step: "Response composed".
9. `passed = prevalidation all pass && statusCode < 500`.
10. Append ExecutionRun.

## `runScenarioSuite(params)`
Iterate all scenarios in scope (workspace or apiId), call `runEndpoint` for each. Return ExecutionRun[].

## UI
### `/scenarios`
- Table grouped by API/endpoint
- Inline create dialog: kind, statusCode, latencyMs, responseBody (JSON textarea), headerOverrides

### `/environments`
- Card per env: baseUrl, authMode, fixedHeaders, timeout
- Add/remove

### `/prevalidation`
- Toggle list (schema, auth, headers, rate, moderation, custom)

### `/execution`
- Run table: time · API · endpoint · mode badge · status · duration · pass icon
- Row click → drawer: request (curl-ish) · response (status, headers, body) · trace timeline · prevalidation list

## Acceptance
- Given scenario `happy_path` with statusCode=200, when Run, then ExecutionRun.passed=true and trace has 3+ steps.
- Given disabled env, run blocked with toast.
- Given role=viewer, "Run" hidden.
