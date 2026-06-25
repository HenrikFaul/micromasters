# APIBox — Full Rebuild Prompt Pack (v2)

Ez a csomag elegendő ahhoz, hogy egy AI ügynök **nulláról újraépítse** az APIBox Integration Workbench webappot az aktuális állapot szerint. A FORGE intake módszertant követi (10→11→12→13 layerek), majd feature-szintű és technikai bontásban folytatja.

> **v2 frissítés (2026-06-12)**: hozzáadva az `F15 — API Explorer Console` (Postman-szerű konzol), a Catalog egységes Import modal (file / URL / SwaggerHub / Postman / YAML), `spec-loader` + `spec-diff` modulok, séma-alapú request validáció, Mock multi-status example, Data Pack favorites, spec watch + diff report, auto auth-detect (`securitySchemes`). Lásd `10_FEATURE_PROMPTS/F15_api_explorer_console.md`.

## Hogyan használd

### A) One-shot rebuild
Másold be `00_MASTER_REBUILD_PROMPT.md` teljes tartalmát egy új AI session-be (Lovable, Claude, GPT-5).

### B) Phased rebuild (ajánlott)
A `12_BUILD_ORDER.md` szerinti sorrendben add át a fájlokat:
1. `01_BUSINESS_BRIEF.md`
2. `02_ARCHITECT_INTAKE.md`
3. `03_PRODUCT_SPEC.md`
4. `04_ROLE_ACTIVATION.md`
5. `05_DATA_MODEL.md`
6. `06_UX_FLOWS.md` + `07_UIUX_DESIGN.md` + `08_ART_DIRECTION.md`
7. `09_API_CONTRACT.md`
8. `10_FEATURE_PROMPTS/F01..F15` — feature-ről feature-re kódolás
9. `11_TECH_IMPLEMENTATION.md`
10. Build & smoke teszt

## Mit épít fel

APIBox = **API tervezés, mockolás, mapping, futtatás és governance** workbench, frontend-first (Zustand persist + localStorage) appként:

- API Catalog & egységes **Import modal** (file JSON/YAML, URL, SwaggerHub/Swagger Editor link, bare API base URL, Postman collection)
- **API Explorer Console** (Postman-szerű, kategória-szűrővel, auto auth-detect, séma validáció, mock multi-status, request preview, Data Pack favorites, spec watch + diff) — F15
- Endpoint Designer
- Scenarios + Data Packs + Error Library
- Mapping Studio
- Mock Forge
- Prevalidation + Execution Console
- Environments
- OpenAI Control
- Architecture Explorer
- Schema Diff + PDF/JSON export
- Gateway Export + Gateway Smoke
- Scheduled Runs + notification log
- Mapping Reviews + production lock
- Audit Log
- Settings (retention, impersonation, reset)
- Dashboard widgetek
- Test Connections (JSONPlaceholder, PokeAPI, Open-Meteo, REST Countries)
- TMF Open API csomag (`public/specs/tmf/manifest.json` + 50+ swagger JSON) induló katalógusként

## Kritikus tanulságok
Lásd `11_TECH_IMPLEMENTATION.md` "Gotchas" — kiemelten a Zustand v5 `useShallow` szabály.

## Fájllista
```
00_MASTER_REBUILD_PROMPT.md
01_BUSINESS_BRIEF.md
02_ARCHITECT_INTAKE.md
03_PRODUCT_SPEC.md
04_ROLE_ACTIVATION.md
05_DATA_MODEL.md
06_UX_FLOWS.md
07_UIUX_DESIGN.md
08_ART_DIRECTION.md
09_API_CONTRACT.md
10_FEATURE_PROMPTS/F01..F15
11_TECH_IMPLEMENTATION.md
12_BUILD_ORDER.md
```
