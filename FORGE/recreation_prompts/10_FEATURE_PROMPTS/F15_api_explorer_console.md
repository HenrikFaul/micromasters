# F15 — API Explorer Console (Postman-szerű)

## Cél
Önálló oldal a `/api-explorer` útvonalon, ahol a felhasználó a betöltött OpenAPI / Swagger specifikációkból bármely végpontot ki tud próbálni Mock vagy Live módban. Helyettesíti a régi „bedömpingelt endpoint dropdown" élményt egy strukturált, Postman-szerű konzollal.

## Belépési pontok
- Sidebar → **Design → API Explorer**.
- API Catalog → egy konkrét API sorából „Open in Explorer" gomb.
- Audit / Execution sorokból mély-link `?specId=...&op=METHOD path`.

## Layout (3 panel)
1. **Bal sáv — Spec & Endpoint browser**
   - Felül: **Source select** (a localStorage `apibox.specs.v1` kulcsban tárolt specek + a `public/specs/tmf/manifest.json` alapján auto-betöltött TMF csomag).
   - **Category filter chip-sor**: a spec `info.x-category` vagy heurisztika alapján (TMF / IoT / GTFS / Meteo / 622 / Custom). Egyszerre csak az adott szolgáltatás endpoint-jai látszanak — sosem ömlik bele az összes.
   - Endpoint fa **tag → method+path** csoportosítással. Method-pill színek: GET zöld, POST kék, PUT narancs, DELETE piros, PATCH lila.
   - Keresőmező (path / summary / tag).
   - **Watch** csillag: spec verzió-figyelés be/ki (lásd Watch & Diff).

2. **Közép — Request Builder**
   - Method + URL preview (path-param interpoláció után).
   - **Base URL override** input + env-választó (a spec `servers[]`-ből).
   - **Auth panel** — auto-detektált a `securitySchemes`-ből (`extractAuthDefaults`). Mezők: header név, prefix (Bearer/Basic/—), in (header/query), API key. Az érték per-spec localStorage-ben (`apibox.apikeys.v1`).
   - **Params** szekciók: Path / Query / Header — `ParamList` + `ParamRow` komponens, oszlopok: név, in, type/format, required (piros pötty), deprecated (áthúzás), description, enum (Select dropdown), constraints (min/max/pattern/minLength/maxLength), példa.
   - **Body editor** (POST/PUT/PATCH): JSON editor + élő séma-validáció (`bodyValidation`). Megjeleníti a required mezőket, típusokat, enumokat (`SchemaHints` collapsible). Hiba lista pirosan, „Body matches schema" zölden.
   - **Request Preview** kártya: a végleges method + abszolút URL + headerek (api-key érték maszkolva).
   - **Mock/Live toggle** + Mock módban **response code select** (a `responses` map összes 2xx/4xx/5xx kódja, jelzi melyikhez van example).
   - „Send" gomb. „Save as Data Pack" (favorit) — `apibox.favorites.v1` localStorage.

3. **Jobb — Response panel**
   - Status pill + latency + size.
   - Tabs: **Body** | **Headers** | **Schema** (a végpont válasz-sémája fa-nézetben) | **Trace** (mock módban a prevalidáció lépései).
   - Body view: pretty JSON, kereshető, másolható, letölthető `.json`.
   - „Compare with schema" toggle: a body mezőit összeveti a deklarált sémával.

## Spec betöltés (Import gomb / Catalog modal)
- Támogatott források:
  - **Fájl feltöltés**: `.json`, `.yaml`, `.yml` (js-yaml), Postman v2.1 collection (alap konvertálás operation-ökké).
  - **Direct URL**: bármely raw spec URL.
  - **Swagger Editor / SwaggerHub link**: `normalizeSpecUrl` kibontja a `?url=` paramétert ill. a SwaggerHub app linkeket raw API download URL-re alakítja.
  - **Bare API base URL**: `loadSpecFromUrl` próbálkozási sorrendben hív: `/openapi.json`, `/openapi.yaml`, `/v3/api-docs`, `/swagger.json`, `/swagger.yaml`, `/docs/openapi.json`, `/api-docs`.
- Sikeres betöltés után a spec **két helyre kerül**: globális Zustand store (Catalog) ÉS `apibox.specs.v1` localStorage (Explorer). Ezért az új import azonnal megjelenik mindkét felületen.

## Validáció — Mock mód szigorúság
A „Send" gomb Mock módban először `built.missing` (hiányzó required path/query/header) és `built.bodyIssues` (séma-eltérések) listát épít. Ha bármi van, a response **NEM 200**, hanem:
```
status: 400
body: { "error": "validation_failed", "issues": [...] }
```
Csak tisztán átment request kap a deklarált `responses`-ből választott példát / generált payload-ot.

## Watch & Diff
- Watch be: aktuális spec fingerprint (`specFingerprint`) + snapshot tárolása `apibox.watches.v1` localStorage-ben.
- Mount-kor és „Check Now" gombra a loader újra letölti a spec-et és `diffSpecs(prev, next)` -et futtat → `SpecDiffReport`:
  - hozzáadott / törölt operation-ök
  - paraméter típus / required / enum változás (breaking flag)
  - response code változás
- Modal mutatja a diffet; jóváhagyás után az új snapshot lesz az új baseline.

## Favorites / Data Pack
- `Favorite` rekord: `{id, name, specId, method, path, mode, baseUrlOverride, apiKeyHeader, apiKeyPrefix, pathParams, queryParams, headers, body, mockStatus, createdAt}`.
- UI: header „Data Pack" dropdown — listáz, betölt, töröl. „Save as..." input + gomb.

## Adatkulcsok (localStorage)
- `apibox.specs.v1` — importált specek `{id, name, spec, importedAt}[]`.
- `apibox.apikeys.v1` — `{ [specId]: { headerName, prefix, value, in, queryName } }`.
- `apibox.favorites.v1` — `Favorite[]`.
- `apibox.watches.v1` — `{ [specId]: { fingerprint, snapshot, updatedAt } }`.

## Acceptance criteria
- Új YAML / JSON / SwaggerHub URL import után 1 másodpercen belül megjelenik a Catalogban ÉS az Explorer bal sávjában a megfelelő kategória alatt.
- Kötelező mező törlése után a Send Mock módban 400-as `validation_failed` választ ad.
- Bearer / API key auto-detektálás a spec `securitySchemes`-ből — első használat után az érték a per-spec kulcs alatt megmarad reload után is.
- A response panel Schema fülön a deklarált válasz-séma fa-nézete megjelenik akkor is, ha nincs example.
- Watch módban a spec külső változása után a Check Now diff modalt nyit hozzáadott/törölt/breaking változások listájával.

## Érintett fájlok
- `src/routes/api-explorer.tsx` — fő oldal.
- `src/routes/catalog.tsx` — „API Catalog \\ Import" modal (file/URL/Postman, YAML).
- `src/lib/spec-loader.ts` — `parseSpecString`, `normalizeSpecUrl`, `loadSpecFromUrl`, `extractAuthDefaults`.
- `src/lib/spec-diff.ts` — `diffSpecs`, `specFingerprint`, `SpecDiffReport`.
- `src/domain/openapi-parser.ts` — közös YAML-aware parser az Import flow-hoz.
- `public/specs/tmf/manifest.json` + TMF swagger JSON-ok — induló katalógus.
- `src/components/shell/app-sidebar.tsx` — „API Catalog \\ Import" + „API Explorer" linkek.

## Külső függőségek
- `js-yaml` (YAML parse)
- shadcn: Dialog, Select, Tabs, Textarea, Collapsible, Tooltip, Badge
