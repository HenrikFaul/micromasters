# 00 — Master Rebuild Prompt (one-shot)

Másold be ezt egy új AI session-be (Lovable / Claude / GPT-5) változatlanul. A kontextusként hivatkozott fájlok (`01_BUSINESS_BRIEF.md` … `12_BUILD_ORDER.md` + `10_FEATURE_PROMPTS/F01..F15`) együtt képezik a teljes csomagot — add át mindet, vagy egy archívumban (zip), vagy egyenként.

---

## Prompt

> You are an autonomous senior fullstack engineer. **Rebuild the APIBox Integration Workbench webapp** from the attached prompt pack. Do not stop until every feature in `03_PRODUCT_SPEC.md` is functional and matches the acceptance criteria.
>
> **Mandatory reading order:**
> 1. `01_BUSINESS_BRIEF.md` — understand the product, personas, JTBD, non-goals.
> 2. `02_ARCHITECT_INTAKE.md` — confirm the stack: TanStack Start v1 (Vite 7) + React 19 + TS strict + Tailwind v4 + shadcn/ui + Zustand v5 + jspdf + js-yaml + zod. **No backend.** All state lives in a single Zustand store, persisted to localStorage.
> 3. `03_PRODUCT_SPEC.md` — F1..F15 features, RBAC matrix, build priority order.
> 4. `04_ROLE_ACTIVATION.md` — embody the lead role per phase.
> 5. `05_DATA_MODEL.md` — copy `types.ts` verbatim. Do not change field names or types.
> 6. `06_UX_FLOWS.md` — every page must support these flows end-to-end.
> 7. `07_UIUX_DESIGN.md` — shell layout, page templates, component inventory.
> 8. `08_ART_DIRECTION.md` — copy the oklch tokens into `src/styles.css` verbatim. No Tailwind config file (v4).
> 9. `09_API_CONTRACT.md` — Zustand action surface, lib utilities, gateway export format.
> 10. `10_FEATURE_PROMPTS/F01..F15` — implement each feature in priority order.
> 11. `11_TECH_IMPLEMENTATION.md` — exact deps, route file table. **READ THE "Gotchas" SECTION FIRST** — especially `useShallow` for every Zustand collection selector, or the UI will lock up with infinite re-renders.
> 12. `12_BUILD_ORDER.md` — execute step 0 → 18 in order. Run a build after each step; fix createFileRoute path mismatches before continuing.
>
> **Hard rules:**
> - Do NOT introduce a backend, Supabase, edge functions, or fetch calls for app state. All state is client-only Zustand.
> - Do NOT use `react-router-dom`. Use `@tanstack/react-router`.
> - Every Zustand selector that returns an array/object MUST be wrapped with `useShallow` from `zustand/react/shallow`.
> - Every mutating action must call `pushAudit(...)` so the audit log is complete.
> - Production-locked mappings: `upsertMapping` / `deleteMapping` must check `productionLock` and the caller's role; if denied, push `mapping.write.denied` audit and return silently.
> - Empty list states show a CTA card. Never render a blank `<div>`.
> - Toast (sonner) on every mutation.
> - Every route file has `head()` with a unique title.
>
> **Done definition:**
> - All 15 features (F01..F15) implementable end-to-end with the seed data.
> - "Load public test APIs" button populates 4 real APIs (JSONPlaceholder, PokeAPI, Open-Meteo, REST Countries) + scenarios + mappings + schedules.
> - Manual smoke walks: import → endpoint designer → scenario → run → mapping → mock → schema diff → gateway smoke → schedule tick → mapping review + production lock → audit export.
> - `npm run build` succeeds with zero TS errors.
>
> Start with Step 0 in `12_BUILD_ORDER.md`. Work in parallel where independent (writing route files, lib files). After every milestone, sanity-check the UI via the preview.

---

## Mit kapsz vissza

Az AI a `12_BUILD_ORDER.md` lépéseit követve felépíti:
- a teljes `src/` fát a `11_TECH_IMPLEMENTATION.md` szerint,
- a Zustand store-t az actions surface-szel,
- az összes 23 route-ot,
- a 4 publikus test API integrációt,
- a PDF/CSV/JSON exportokat,
- a production-lock + audit + schedule tick teljes governance loop-ot.

A végeredmény vizuálisan és funkcionálisan ekvivalens az eredeti APIBox v0.7.0 buildtel.
