# F07 — Schema Diff + PDF/JSON export + Impact

## Goal
Compare two SchemaVersions for an endpoint. Classify breaking vs non-breaking. Compute impact on scenarios and mappings. Export as PDF (jspdf) and JSON.

## Files
- `src/lib/schema-diff.ts`
- `src/lib/pdf-export.ts` (`buildSchemaDiffPdf`)
- `src/routes/schema-diff.tsx`

## `computeSchemaDiff(versionA, versionB) → diff`
- Walk both schemas (request + each response by status).
- Compute: `addedFields[]`, `removedFields[]`, `changedTypes[{field, before, after}]`.
- `breaking = removedFields.length > 0 || changedTypes.some(...)`.

## `computeImpact(diff, scenarios, mappings, endpointId) → SchemaDiffImpact`
- For each scenario of the endpoint: check if its `responseBody` references any removed field → `scenariosAffected`.
- For each mapping where `sourceApiId/sourceField` or `targetApiId/targetField` matches a removed/changed field → `mappingsAffected`.

## PDF layout (`buildSchemaDiffPdf`)
- Header: endpoint path + method + "vA → vB"
- Summary table: added/removed/changed counts + breaking flag
- Section: Added fields (green dot)
- Section: Removed fields (red dot)
- Section: Changed types (orange dot)
- Section: Impact — affected scenarios + mappings list
- Footer: generated at + actor

## UI `/schema-diff`
- Endpoint picker + version A picker + version B picker
- Side-by-side schema text (mono) with colored gutter
- Bottom impact card stack
- Buttons: "Export PDF" (download .pdf) + "Export JSON" (download .json)

## Acceptance
- Given versionA has `email: string` and versionB removes it, then diff.removedFields contains `email` + breaking=true.
- Given a mapping references the removed field, then impact.mappingsAffected lists it with reason.
- PDF download contains all sections.
