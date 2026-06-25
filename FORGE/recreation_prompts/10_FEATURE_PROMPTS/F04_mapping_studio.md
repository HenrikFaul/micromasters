# F04 — Mapping Studio + live preview

## Goal
Build cross-API field mappings (source schema field → target schema field + transform expression). Live preview shows transformed payload validated against target schema.

## Files
- `src/lib/transform-engine.ts`
- `src/routes/mappings.tsx`

## Transform engine grammar
Pure JS evaluator (no eval). Supported expressions:
- `lowercase`, `uppercase`, `trim` (identity transforms)
- `concat($.field1, ' ', $.field2)` — token interpolation
- `iso(date)` — Date → ISO 8601
- `default(value)` — fall-back when source null/undefined
- `substring(start, end)`
- `regex_replace(pattern, replacement)`
- `lookup({key:value, ...})` — value mapping table

Parse: tokenize on `(`, `)`, `,`, `$.`. Reject anything else; UI shows red.

## Store contract
```ts
upsertMapping(m: FieldMapping): void   // checks productionLock + role
setMappingStatus(id, status): void     // proposed|approved|rejected
deleteMapping(id): void                // checks productionLock
```

Auto-compute `confidence`:
- 1.0 if source/target type match exactly
- 0.7 if both are strings of similar names
- 0.5 if transform present
- 0.3 otherwise

## UI
- Left column: source API+schema tree (collapsible)
- Right column: target API+schema tree
- Center: mappings table (source field → target field, transform, status, confidence bar)
- Right preview drawer: source example JSON → transform → output JSON + schema validation result

## Acceptance
- Given source field `user.email` and target `contact.email` with `lowercase` transform, when source example is `{user:{email:"Foo@Bar"}}`, preview output shows `"foo@bar"`.
- Given mapping with productionLock=true and role=editor, save denied + audit.
- Given confidence < 0.4, ValidationIssue `mapping_low_confidence` seeded.
