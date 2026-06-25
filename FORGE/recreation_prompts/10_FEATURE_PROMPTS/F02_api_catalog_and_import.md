# F02 — API Catalog + Unified Import

## Goal
Egységes import folyamat: a Catalog oldal címe **"API Catalog \ Import"**, az „Import" gomb **modalt** nyit. Nincs külön `/import` menüpont a sidebar-ban (a `/import` route megmaradhat fallback-nek, de a navigáció a Catalog-on keresztül megy). A modal támogat fájlt, URL-t (raw + SwaggerHub / Swagger Editor + bare base URL), és Postman v2.1 collection konverziót. Sikeres import azonnal megjelenik **mind** a Catalogban (Zustand store) **mind** az API Explorer bal sávjában (`apibox.specs.v1` localStorage).

## Files
- `src/routes/catalog.tsx` — lista + Import modal trigger
- `src/lib/spec-loader.ts` — `parseSpecString(text)`, `normalizeSpecUrl(url)`, `loadSpecFromUrl(url)`, `extractAuthDefaults(spec, op?)`
- `src/domain/openapi-parser.ts` — közös YAML-aware parser, weakness detection
- `src/routes/import.tsx` — fallback / haladó nézet (megmarad, de elsődleges flow a Catalog modal)

## Source támogatás
| Forrás | Logika |
|---|---|
| `.json` fájl | `JSON.parse` |
| `.yaml` / `.yml` fájl | `js-yaml` `load` |
| Postman collection | item-walk → `{method, url, body}` → szintetikus OpenAPI 3.0 operations |
| Raw spec URL | `fetch` → content-type alapján JSON/YAML parse |
| Swagger Editor link (`editor.swagger.io/?url=...`) | `normalizeSpecUrl` kibontja a `?url=` paramot |
| SwaggerHub app link | `normalizeSpecUrl` átírja `api.swaggerhub.com/apis/.../swagger.json` raw URL-re |
| Bare API base URL | `loadSpecFromUrl` próba sorrend: `/openapi.json`, `/openapi.yaml`, `/v3/api-docs`, `/swagger.json`, `/swagger.yaml`, `/docs/openapi.json`, `/api-docs` |

## Parser logika (`openapi-parser.ts`)
1. Próbál `JSON.parse`-t, fallback `yaml.load` (js-yaml).
2. Paths bejárás → ParsedEndpoint (method, summary, response codes, parameterCount, hasRequestBody).
3. `components.schemas` bejárás → ParsedSchema (fieldCount, hasExample).
4. Weakness detekció (no_example, vague_description, missing_enum, weak_error_response, sparse_response_coverage, no_auth_detail, unresolved_ref).
5. `extractAuthDefaults` — `securitySchemes` parse → `{type, headerName, prefix, in, queryName}` (Bearer / Basic / apiKey header / apiKey query / oauth2).

## Store contract
```ts
createImportJob({ filename, source, rawText }): string
parseImportJob(jobId): void
commitImportJob(jobId): void   // → ApiAsset + Endpoints + SchemaObjects + ValidationIssues
                               // + push to apibox.specs.v1 localStorage (Explorer látja)
```

## UI — Catalog modal
- Trigger: **„Import"** gomb a Catalog header-ben.
- Modal tabs: **File** | **URL** | **Postman**.
- Preview szekció parse után: Endpoints / Schemas / Servers / Auth lista weakness chip-ekkel.
- **„Commit import"** (role-gated: `import.commit`) — egy lépésben két helyre ír (store + localStorage).

## `/catalog` lista
- Kereső + filter (domain dropdown, lifecycle multi, tag chips).
- Kártyák: API név · version chip · lifecycle badge · readiness progress · tag lista.
- Sor-action: **„Open in Explorer"** → `/api-explorer?specId=...`.

## `/api/$apiId`
- Tabs: Endpoints / Schemas / Environments / Snapshots.
- Endpoint sor: method badge · path mono · summary · weakness chip → designer link.

## Acceptance
- Új YAML import a Catalog modal-ból azonnal (≤ 1s) megjelenik a Catalog listában ÉS az API Explorer bal sávjában.
- SwaggerHub link beragasztva is működik (auto-normalize).
- Bare base URL pl. `https://example.com/api` esetén a probe végigmegy a 7 jelölt útvonalon, és az első sikeres JSON/YAML-t használja.
- Postman collection import-ból kreált op-ok megjelennek tag = collection-folder név alatt.
- `securitySchemes`-ben definiált Bearer token / X-API-Key auto-megjelenik az Explorer Auth panelben, érték per-spec `apibox.apikeys.v1`-ben perzisztens.
- Role=operator esetén „Commit" disabled tooltippel.
