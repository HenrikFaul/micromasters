# 11 — Technical Implementation

## Dependencies (`package.json`)

```json
{
  "dependencies": {
    "@hookform/resolvers": "^5.2.2",
    "@radix-ui/react-accordion": "^1.2.12",
    "@radix-ui/react-alert-dialog": "^1.1.15",
    "@radix-ui/react-aspect-ratio": "^1.1.8",
    "@radix-ui/react-avatar": "^1.1.11",
    "@radix-ui/react-checkbox": "^1.3.3",
    "@radix-ui/react-collapsible": "^1.1.12",
    "@radix-ui/react-context-menu": "^2.2.16",
    "@radix-ui/react-dialog": "^1.1.15",
    "@radix-ui/react-dropdown-menu": "^2.1.16",
    "@radix-ui/react-hover-card": "^1.1.15",
    "@radix-ui/react-label": "^2.1.8",
    "@radix-ui/react-menubar": "^1.1.16",
    "@radix-ui/react-navigation-menu": "^1.2.14",
    "@radix-ui/react-popover": "^1.1.15",
    "@radix-ui/react-progress": "^1.1.8",
    "@radix-ui/react-radio-group": "^1.3.8",
    "@radix-ui/react-scroll-area": "^1.2.10",
    "@radix-ui/react-select": "^2.2.6",
    "@radix-ui/react-separator": "^1.1.8",
    "@radix-ui/react-slider": "^1.3.6",
    "@radix-ui/react-slot": "^1.2.4",
    "@radix-ui/react-switch": "^1.2.6",
    "@radix-ui/react-tabs": "^1.1.13",
    "@radix-ui/react-toggle": "^1.1.10",
    "@radix-ui/react-toggle-group": "^1.1.11",
    "@radix-ui/react-tooltip": "^1.2.8",
    "@tailwindcss/vite": "^4.2.1",
    "@tanstack/react-query": "^5.83.0",
    "@tanstack/react-router": "^1.168.25",
    "@tanstack/react-start": "^1.167.50",
    "@tanstack/router-plugin": "^1.167.28",
    "class-variance-authority": "^0.7.1",
    "clsx": "^2.1.1",
    "cmdk": "^1.1.1",
    "date-fns": "^4.1.0",
    "embla-carousel-react": "^8.6.0",
    "input-otp": "^1.4.2",
    "js-yaml": "^4.2.0",
    "jspdf": "^4.2.1",
    "lucide-react": "^0.575.0",
    "react": "^19.2.0",
    "react-day-picker": "^9.14.0",
    "react-dom": "^19.2.0",
    "react-hook-form": "^7.71.2",
    "react-resizable-panels": "^4.6.5",
    "recharts": "^2.15.4",
    "sonner": "^2.0.7",
    "tailwind-merge": "^3.5.0",
    "tailwindcss": "^4.2.1",
    "tw-animate-css": "^1.3.4",
    "vaul": "^1.1.2",
    "vite-tsconfig-paths": "^6.0.2",
    "zod": "^3.24.2",
    "zustand": "^5.0.14"
  }
}
```

## Project tree

```
src/
├── assets/apibox-logo.svg
├── components/
│   ├── shell/
│   │   ├── app-shell.tsx       wraps useScheduleTick() + AppSidebar + AppTopBar + <main>
│   │   ├── app-sidebar.tsx     5 nav sections (Workspace/Modeling/Runtime/Intelligence/Governance)
│   │   └── app-topbar.tsx      breadcrumb, env switcher, role badge, search
│   ├── common/primitives.tsx   Tag, StatusDot, Empty, SectionTitle, MetaList
│   └── ui/                     shadcn primitives (50+ files, generated)
├── domain/
│   ├── types.ts                see 05_DATA_MODEL.md
│   ├── store.ts                Zustand store, 1000+ lines
│   ├── seed.ts                 minimal demo data
│   ├── test-connections.ts     JSONPlaceholder, PokeAPI, Open-Meteo, REST Countries
│   └── openapi-parser.ts       js-yaml + JSON, weakness detection
├── hooks/
│   ├── use-mobile.tsx
│   └── useScheduleTick.ts      setInterval(settings.tickIntervalSec * 1000) → runScheduleTick()
├── lib/                        see 09_API_CONTRACT.md "Lib utility surface"
│   ├── pdf-export.ts
│   ├── gateway-export.ts
│   ├── schema-diff.ts           # internal endpoint schema diff (governance)
│   ├── spec-diff.ts             # OpenAPI spec-level diff for watch (F15)
│   ├── spec-loader.ts           # parseSpecString, normalizeSpecUrl,
│   │                            # loadSpecFromUrl (probe), extractAuthDefaults
│   ├── mock-forge.ts
│   ├── transform-engine.ts
│   ├── cron-eval.ts
│   ├── permissions.ts
│   └── utils.ts
├── routes/
│   ├── __root.tsx              <html><body><AppShell><Outlet /></AppShell></body></html>
│   ├── index.tsx                /
│   ├── catalog.tsx              /catalog   (label: "API Catalog \ Import", modal-driven import)
│   ├── import.tsx               /import    (fallback advanced view)
│   ├── api-explorer.tsx         /api-explorer  (F15 Postman-szerű konzol)
│   ├── api.$apiId.tsx           /api/$apiId
│   ├── api.$apiId.endpoint.$endpointId.tsx   /api/$apiId/endpoint/$endpointId
│   ├── data-packs.tsx           /data-packs
│   ├── scenarios.tsx            /scenarios
│   ├── mappings.tsx             /mappings
│   ├── mapping-reviews.tsx      /mapping-reviews
│   ├── errors.tsx               /errors
│   ├── schema-diff.tsx          /schema-diff
│   ├── environments.tsx         /environments
│   ├── prevalidation.tsx        /prevalidation
│   ├── execution.tsx            /execution
│   ├── gateway-smoke.tsx        /gateway-smoke
│   ├── schedules.tsx            /schedules
│   ├── mock-forge.tsx           /mock-forge
│   ├── openai.tsx               /openai
│   ├── explorer.tsx             /explorer
│   ├── audit.tsx                /audit
│   └── settings.tsx             /settings
├── routeTree.gen.ts            AUTO-GENERATED, do not edit
├── router.tsx                  createRouter({ routeTree, defaultPreload: 'intent' })
├── start.ts                    TanStack Start entry
├── server.ts                   SSR handler (auto)
└── styles.css                  Tailwind v4 @theme tokens
```

## Route → component map

Every file's `createFileRoute(...)` string MUST equal the URL above (dots in filename → slashes in route id; `$param` for dynamic segments).

## Build order

See `12_BUILD_ORDER.md`.

---

## ⚠️ Gotchas (build közben tanult buktatók — KRITIKUS)

### 1. Zustand v5 `useShallow` — kötelező collection selectorokra
Zustand v5 szigorú `Object.is` egyenlőséget használ a `useSyncExternalStore` alatt. Ha egy selector új array/object referenciát ad vissza render-enként (pl. inline `.filter`), végtelen re-render loopba esik az egész app és a UI lefagy ("Maximum update depth exceeded"). **Megoldás:**

```ts
// ❌ ROSSZ — minden renderben új array referencia
const apis = useApiBoxStore((s) => s.apis.filter((a) => a.workspaceId === wsId));

// ✅ HELYES
import { useShallow } from "zustand/react/shallow";
const apis = useApiBoxStore(useShallow((s) =>
  s.apis.filter((a) => a.workspaceId === wsId)
));
```

Vagy még jobb: derived state-et **számold ki a komponensben** primitív vagy referencia-stabil selector eredményéből.

### 2. `createFileRoute` path string
A fájlnévben dot, a `createFileRoute("...")` stringben **slash**. Pl. `api.$apiId.endpoint.$endpointId.tsx` → `createFileRoute("/api/$apiId/endpoint/$endpointId")`. Eltérés esetén build break / üres oldal.

### 3. Layout route `<Outlet />`
Ha egy route gyermek route-okat fogad (pl. `/api/$apiId` → `/api/$apiId/endpoint/$endpointId`), a layout `component`-nek render-elnie kell `<Outlet />`-et, különben a gyermek mountol de nem jelenik meg.

### 4. `persist` migration
A v5 schema új mezőket vezet be (`scheduledRunRecords`, `notificationLog`, `pinnedAuditIds`, `settings`). A `migrate` callback **nem destruktív** — csak hiányzó mezőket ad hozzá default értékkel. Production-ben létező localStorage-t ne dobjuk el.

### 5. Production lock store-szinten
A UI csak elrejti a "Save" gombot — biztonsági szempontból (és a demo helyesség kedvéért) a `upsertMapping` action **maga** ellenőrzi a productionLock-ot és role-t, audit bejegyzéssel utasít el. Lásd `09_API_CONTRACT.md`.

### 6. `jspdf` és sync writes
A `jspdf` szinkron, böngészőben fut. PDF buildelés alatt ne lépj `await`-re; ha nagy doksi, használj `setTimeout(0)` chunkolást, hogy a UI ne fagyjon.

### 7. `js-yaml` `load` típushiány
`yaml.load(text)` `unknown` típust ad — Zod sémával validáld parse után, vagy explicit `as` cast. Ne támaszkodj `any`-re.

### 8. `useScheduleTick` re-mount loop
A hook az `AppShell`-ben fut, ami minden route-on render-el. A `setInterval`-t **csak egyszer** indítsd (`useEffect` üres deps), és a tick callback Zustand-ot olvasson `getState()`-tel, ne hookkal — különben tick triggerelheti újra magát.

```ts
useEffect(() => {
  const { settings } = useApiBoxStore.getState();
  if (!settings.autoTickEnabled) return;
  const id = setInterval(() => useApiBoxStore.getState().runScheduleTick(),
                          settings.tickIntervalSec * 1000);
  return () => clearInterval(id);
}, []);
```

### 9. Cron parser scope
`cron-eval.ts` nem implementál teljes Unix cront. Csak: `every N min`, `every N hour`, `daily HH:MM`, `weekly DOW HH:MM` (DOW = mon..sun). Hibás kifejezést `parseCron` `null`-lal jelez, a UI tiltja a mentést.

### 10. localStorage quota
~5MB limit. A `pruneExecutions` / `pruneAudit` / `pruneSnapshots` minden tick-en lefut. Manuális reset: Settings → "Reset workspace to seed".

### 11. `Tailwind v4` — nincs `tailwind.config.js`
Minden token a `src/styles.css`-ben `@theme` blokkban. Új színt felvenni: hozzáadod a `:root` és `.dark` blokkba + `@theme inline` blokkba `--color-X: var(--X)` mappinget.

### 12. Toast import
`sonner` package — `import { toast } from "sonner"`. Felépítés a `src/routes/__root.tsx`-ben: `<Toaster richColors position="top-right" />`.

### 13. `react-router-dom` TILOS
Ne importálj sehonnan. Minden link/navigáció `@tanstack/react-router`-ből.

### 14. localStorage kulcsok (F15 — Explorer)
A Zustand `persist` middleware kulcsán **felül** az API Explorer önálló localStorage kulcsokat használ, hogy az Explorer reload után is azonnal hozzáférjen a specekhez és credentialökhöz a teljes store hidratálódása előtt:

| Kulcs | Tartalom |
|---|---|
| `apibox.specs.v1` | `{ id, name, spec, importedAt }[]` — importált specek |
| `apibox.apikeys.v1` | `{ [specId]: { headerName, prefix, value, in, queryName } }` |
| `apibox.favorites.v1` | `Favorite[]` — Data Pack mentett requestek |
| `apibox.watches.v1` | `{ [specId]: { fingerprint, snapshot, updatedAt } }` — spec verzió-figyelés |

A Catalog `commitImportJob` ÉS a Catalog Import modal egyaránt **két helyre ír** sikeres importnál (store + `apibox.specs.v1`), különben az Explorer nem látja az új specet.

### 15. `spec-loader` URL probe sorrend
`loadSpecFromUrl(url)` először `normalizeSpecUrl`-rel kibontja a SwaggerHub / Swagger Editor wrapper URL-eket, majd ha az URL nem tartalmaz spec fájl-utótagot, próba sorrendben hív: `/openapi.json`, `/openapi.yaml`, `/v3/api-docs`, `/swagger.json`, `/swagger.yaml`, `/docs/openapi.json`, `/api-docs`. Az első sikeres parse nyer. CORS hiba esetén a UI tájékoztat, hogy a böngészőből nem érhető el — proxy / másolás-beillesztés ajánlott.

### 16. `spec-diff` modul
`diffSpecs(prev, next): SpecDiffReport` — összehasonlít operation-szinten: added, removed, paraméter typed/required változás, response code változás, schema field add/remove/type change. A `breaking` flag igaz, ha required mező hozzáadódik vagy típus változik / response kód eltűnik. `specFingerprint(spec)` egy lightweight string (paths + method + status codes hash) a watch tick gyors összevetéséhez.

### 17. Mock validation gate
A `/api-explorer` Send Mock módban először `built.missing` (path/query/header required hiány) és `built.bodyIssues` (séma-eltérés a JSON body-ban) listát épít. Bármelyik nem üres → `400 validation_failed` response a `{ error, issues[] }` body-val. Csak tisztán átment request kap a `responses` map-ból választott / generált példát. Ez gátolja a "rossz adatra is 200" hibát.
