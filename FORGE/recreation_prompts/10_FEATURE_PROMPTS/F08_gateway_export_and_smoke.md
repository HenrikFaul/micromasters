# F08 — Gateway Export + Smoke Test

## Goal
Export a Kong-style gateway config and run a smoke test (scenario subset) against the generated config; record per-scenario pass/fail with plugin checks; export PDF report.

## Files
- `src/lib/gateway-export.ts` — `exportKongConfig(apis, envs) → string` (YAML)
- `src/lib/pdf-export.ts` — `buildSmokeReportPdf(run)`
- store: `runGatewaySmoke`
- `src/routes/gateway-smoke.tsx`
- `src/routes/explorer.tsx` (has "Export gateway" button → Snapshot)

## Kong YAML shape
```yaml
_format_version: "3.0"
services:
  - name: <api-slug>-<version>
    url: <env.baseUrl>
    routes:
      - name: <method>-<path-sanitized>
        paths: [<endpoint.path>]
        methods: [<METHOD>]
        plugins:
          - name: rate-limiting        # if env.retries or behavior.retryable
          - name: key-auth             # if endpoint.authRequired && authProfile=apiKey
          - name: jwt                  # if authProfile=bearer
          - name: request-validator    # always, with schema
```

## `runGatewaySmoke({ scenarioIds, environmentName }) → GatewaySmokeRun`
1. For each scenarioId, build a simulated request from scenario+endpoint+env.
2. Plugin checks (simulated):
   - `rate-limiting`: pass if `latencyMs < env.timeoutMs`
   - `key-auth`: pass if scenario has `x-api-key` in headers OR authProfile=none
   - `request-validator`: pass if scenario.responseBody parses + matches schema
3. Per result: `passed`, `statusCode`, `durationMs`, `message`, `pluginChecks[]`.
4. Overall `passed = results.every(r => r.passed)`.

## UI `/gateway-smoke`
- Top bar: env picker + scenario multi-select + "Run smoke" button (role-gated)
- Smoke run history table (newest first): time · scenarios · pass/fail · creator
- Row click → drawer: per-scenario detail table + plugin checks + "Export PDF"

## Acceptance
- Given 3 scenarios with happy_path 200s, when Run, then run.passed=true and 3 result rows.
- Given a scenario with statusCode=429, plugin `rate-limiting` check fails.
- Export PDF contains all results + plugin checks.
