# F06 — Mock Forge

## Goal
Deterministic mock payload generation per endpoint + status, schema-validated, with coverage report and "Inject as response example" affordance.

## Files
- `src/lib/mock-forge.ts`
- `src/routes/mock-forge.tsx`

## `generateMock(schema, seed, status)`
1. Seeded PRNG (mulberry32(seed)).
2. Walk JSON Schema:
   - `string` with `enum` → pick deterministically
   - `string` with `format=email|uuid|date-time|uri` → realistic mock
   - `number/integer` → seeded between `minimum..maximum`
   - `boolean` → seeded
   - `object` → recurse properties, respect `required`
   - `array` → length 1..3, recurse `items`
   - `oneOf/anyOf` → pick first by seed
3. Return `{ payload, valid, validationMessages }`.

## `computeCoverage(schema, payload) → MockCoverageReport`
- Walk schema, collect all JSON paths.
- Walk payload, mark covered.
- `percentCovered = covered / total`.

## Store contract
```ts
generateMockForEndpoint({ endpointId, statusCode, seed }): MockArtifact
generateMockBulk({ endpointId, statusCode, count, baseSeed }): MockArtifact[]   // ≤25
injectMockAsExample(mockId, label?): void   // appends to endpoint.responses[status].examples
```

## UI
- Endpoint + status picker
- Seed input + "Generate"
- Payload viewer (monospace) + valid/invalid badge + validation messages
- Coverage panel: percent bar + uncoveredPaths list
- "Bulk generate N" → table of N artifacts
- "Inject as example" → toast + endpoint updated

## Acceptance
- Given schema with required `email`, when generate, then payload.email is non-empty + valid=true.
- Same seed twice → identical payload.
- Inject creates example with `source="generated"` and label `mock-{seed}`.
