# F11 — Audit Log

## Goal
View, filter (actor / action substring / date range), pin important entries, export CSV and PDF.

## Files
- `src/routes/audit.tsx`
- `src/lib/pdf-export.ts` — `buildAuditPdf(entries)`
- store: `togglePinAudit`, `pruneAudit`

## UI
- Filter bar: actor select · action substring input · date range (from/to date pickers)
- Pinned section at top (cards with star filled)
- Main table: time · actor · role badge · action · target · summary · pin star · before/after viewer (collapsible JSON)
- Buttons: "Export CSV" · "Export PDF" (apply current filter)

## CSV format
`at,actor,actorRole,action,target,summary`

## PDF (`buildAuditPdf`)
- Title "Audit Log Export" + filter summary
- Table: time / actor / action / target / summary
- Footer: total count + generated at

## Acceptance
- Given 100 entries, when filter `action="mapping"` + last 7 days, only matching render.
- Pin/unpin toggles `pinnedAuditIds` and reorders.
- CSV download = current filtered set.
- `pruneAudit()` removes entries older than `settings.auditRetentionDays` (but NEVER pinned).
