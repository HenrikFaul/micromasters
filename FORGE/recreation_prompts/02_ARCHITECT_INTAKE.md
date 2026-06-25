# 02 — Architect Intake (Layer 10 output)

```json
{
  "objectiveConfirmed": "APIBox is a desktop-class, offline-first integration workbench covering the full API lifecycle (import, design, mock, map, run, govern, schedule) as a single-page TanStack Start app with Zustand persistence and no live backend.",
  "buildProfile": "standard",
  "stack": {
    "frontend": "TanStack Start v1 (Vite 7) + React 19 + TypeScript strict + Tailwind v4 (oklch design tokens via src/styles.css) + shadcn/ui (Radix primitives)",
    "state": "Zustand v5 with persist middleware (localStorage key 'apibox-store') + useShallow for collection selectors",
    "routing": "File-based TanStack Router under src/routes; createFileRoute strings must match generated route ids",
    "forms": "react-hook-form + @hookform/resolvers + zod",
    "data-viz": "recharts (light usage, dashboard widgets)",
    "icons": "lucide-react",
    "pdf": "jspdf (synchronous, client-only)",
    "yaml": "js-yaml (OpenAPI parser)",
    "backend": "NONE (no Supabase, no edge functions, no server fns in v1) — domain is pure TS in src/domain/, all logic client-side",
    "rationale": "Offline-first demo workbench; goal is fidelity of API governance UX, not multi-tenant SaaS. Removing the backend cuts complexity and unblocks every workflow in <1s. Cloud can be added later if multi-user collaboration is requested."
  },
  "systemBoundaries": [
    "IN: catalog, import, endpoint design, scenarios, data packs, errors, mapping, mock forge, prevalidation, execution (mock/simulated live), environments, openai config, explorer, schema diff, gateway export+smoke, schedules, mapping reviews, audit, settings, dashboard, test connections",
    "OUT: real HTTP upstream calls, real SMTP/webhook delivery, real auth (Supabase), multi-tenant workspaces, billing"
  ],
  "architectureStyle": "client-only SPA, file-based routing, single Zustand store with audit middleware, lib/ utilities for pure logic (pdf-export, gateway-export, schema-diff, mock-forge, transform-engine, cron-eval), background hook (useScheduleTick) for scheduled run ticks",
  "keyDecisions": [
    { "decision": "No backend in v1", "options": ["Supabase from day 1", "client-only Zustand"], "chosen": "client-only Zustand", "tradeoff": "Cannot share state across users; fast to demo and zero ops.", "reversible": true },
    { "decision": "Zustand v5 + useShallow", "options": ["Redux Toolkit", "Jotai", "Zustand v5"], "chosen": "Zustand v5", "tradeoff": "v5 strict equality requires useShallow on every collection selector — see Gotchas.", "reversible": false },
    { "decision": "TanStack Start file-based routing", "options": ["React Router v6 SPA", "TanStack Start"], "chosen": "TanStack Start", "tradeoff": "Strict createFileRoute path matching; rewards typed Link.", "reversible": false },
    { "decision": "Tailwind v4 with @theme tokens", "options": ["Tailwind v3 + config", "Tailwind v4 + CSS @theme"], "chosen": "Tailwind v4", "tradeoff": "No tailwind.config.js; all tokens in src/styles.css.", "reversible": true },
    { "decision": "jspdf for export", "options": ["pdf-lib", "jspdf", "server-side render"], "chosen": "jspdf", "tradeoff": "Smaller bundle, sync API; limited typography.", "reversible": true },
    { "decision": "audit middleware via pushAudit() inside actions", "options": ["Zustand middleware proxy", "explicit pushAudit calls"], "chosen": "explicit pushAudit", "tradeoff": "Verbose but transparent; reviewer can grep every audited action.", "reversible": true },
    { "decision": "production lock enforcement in store actions", "options": ["UI-only disable", "store-level gating"], "chosen": "store-level + UI disable", "tradeoff": "Cannot be bypassed by direct store call.", "reversible": false }
  ],
  "nonFunctionalTargets": {
    "reliability": "No crashes on empty state; every list renders with empty placeholders.",
    "security": "No secrets in client; API keys only logged as redactedHeaders metadata; impersonation is a demo affordance (not a real auth boundary).",
    "performance": "Initial route mount <300ms cold, navigation <100ms warm; lists virtualized only above 200 rows (none currently).",
    "scale": "Single-tab demo; up to ~1000 entities per collection in localStorage."
  },
  "constraints": [
    "No new npm dep beyond the locked package.json unless strictly necessary",
    "No Node-only modules in routes/lib (Worker runtime constraint for future SSR)",
    "Hungarian + English mixed UI labels allowed (current state)"
  ],
  "riskRegister": [
    { "risk": "Zustand v5 selector returning new array → infinite re-render", "likelihood": "high", "impact": "high", "mitigation": "wrap collection selectors with useShallow from zustand/react/shallow; never inline .filter in selector" },
    { "risk": "localStorage quota (~5MB)", "likelihood": "low", "impact": "med", "mitigation": "retention policies (executionRetentionDays, auditRetentionDays); pruneExecutions/pruneAudit/pruneSnapshots run on tick" },
    { "risk": "createFileRoute path drift after rename", "likelihood": "med", "impact": "high", "mitigation": "always match filename dots→slashes; let routeTree.gen.ts regenerate" },
    { "risk": "production-locked mapping silently fails edit", "likelihood": "med", "impact": "med", "mitigation": "UI badge + tooltip + toast on denial; audit entry mapping.write.denied" }
  ],
  "openAssumptions": [
    { "assumption": "Single workspace 'default-ws' is enough for demo", "ifWrongThen": "Add workspace switcher + namespacing on every entity" },
    { "assumption": "User accepts mock+simulated 'live' (not real HTTP)", "ifWrongThen": "Add fetch executor with CORS proxy" }
  ]
}
```
