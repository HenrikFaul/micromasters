# 01 — Business Brief (Layer 10 input)

## Termék

**APIBox — Integration Workbench**: egyetlen, offline-first frontend webapp, ami az API integrációk **teljes életciklusát** lefedi: import → tervezés → mockolás → mapping → futtatás → governance → publikálás.

## Probléma

Egy integrációs csapat ma 5–8 különálló eszközt használ (Postman, Stoplight, Kong manager, Apigee, Excel mapping táblák, JIRA review-k, cron runner). A tudás szétszakad, a változások hatása láthatatlan, a production mappingek "véletlenül" módosulnak, és a smoke teszt manuális.

## Cél (objective)

Egy egységes, demózható workbench, ahol az **API tervező / integrátor / ops mérnök** egy felületen képes:
- importálni OpenAPI specet, azonnal látva a gyengeségeket
- endpoint-onként szerkeszteni request/response schemát és scenariokat
- mock választ generálni, ami a schemával validálódik
- cross-API field mappinget tervezni live preview-val
- scenario suite-ot futtatni mock vagy live módban, trace-szel
- gateway konfigot exportálni és smoke teszttel ellenőrizni
- schedule szerint futtatni és notifikációt küldeni
- mapping változást review-zni, production lock-kal védeni
- minden műveletről audit-ot vezetni, exportálni

## Célközönség (personas)

| Persona | Kontextus | Primary JTBD |
|---|---|---|
| **API Designer** (editor role) | OpenAPI specet ír, schemát tervez | "When I import a spec, I want to see weaknesses and fix them inline so my contract is review-ready." |
| **Integration Engineer** (editor role) | Két API közötti mappinget épít | "When source and target API meet, I want a mapping with live preview so I can ship without writing test code." |
| **Ops / SRE** (operator role) | Pre-prod smoke + ütemezett futások | "When a release goes out, I want a scheduled smoke test with notifications so regressions are caught before customers." |
| **Tech Lead / Owner** (owner role) | Governance, lockok, review | "When a mapping is in production, I want a lock + review trail so accidental changes can't ship." |
| **Auditor / Viewer** (viewer role) | Read-only governance | "I want to see what changed, when, by whom, exportable." |

## Sikerkritériumok

1. Egy frissen elindított példányba importálható egy publikus OpenAPI (vagy a 4 beépített test connection), és **5 percen belül** lefuttatható egy scenario suite trace-szel.
2. Két API mappingje **drag-drop-szerűen** építhető, live preview frissül beíráskor.
3. Schema verzióváltás **diff + impact report** PDF/JSON exportot ad scenariokra és mappingekre.
4. Production-lockolt mapping mentési kísérlete **denied** auditbejegyzést hagy editor szereppel.
5. Egy scheduled run a háttérben "ketyeg" (in-memory tick), futtatja a suite-ot, notification-t logol.

## Non-goals

- Nincs valódi backend (no Supabase, no DB) — minden state localStorage-persisted Zustand store.
- Nincs valódi SMTP / webhook — szimulált notification log.
- Nincs auth flow — szerep-impersonation a Settings-ben.
- Nincs élő HTTP hívás upstream API-kra a runtime-ból — a "live" mode a payloadot szimulálja a stored example alapján.

## Constraints

- Tech stack: TanStack Start v1 + Vite 7 + React 19 + Tailwind v4 + shadcn/ui + Zustand v5
- Egyetlen tab / desktop-class UI (sidebar 244px, topbar 56px)
- Offline-first: localStorage-only persistence, semmi network dependency a működéshez
- Nincs új npm dep a felsorolt stacken kívül (kivéve `jspdf`, `js-yaml`, `zod` ami már bent van)

## Domain szótár

- **Workspace**: a teljes adat namespace-e (jelenleg 1 fix, "Default workspace").
- **Business domain**: az API-k logikai csoportja (pl. "Identity", "Billing").
- **API asset**: egy importált vagy kézzel létrehozott API (név, slug, version, lifecycle).
- **Endpoint**: path + method + parameters + request schema + responses.
- **Scenario**: egy endpoint válaszsablonja (kind: happy_path / validation_error / rate_limit / ...).
- **Data pack**: parametrizált payload sablon, scenariohoz vagy schemához kötve.
- **Field mapping**: source endpoint mező → target endpoint mező + transform.
- **Mock artifact**: generált válasz (seed + payload + valid flag).
- **Schema version**: endpoint request/response schema egy adott verziója.
- **Gateway smoke run**: scenario subset gateway konfig elleni futása.
- **Scheduled run**: cron-szerű ütemezés + retention + notification.
- **Mapping review**: pending → approved/rejected workflow, reviewer szereppel + production lockkal.
- **Audit entry**: actor + role + action + target + before/after.
