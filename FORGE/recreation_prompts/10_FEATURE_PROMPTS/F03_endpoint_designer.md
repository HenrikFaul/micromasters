# F03 — Endpoint Designer

## Goal
Per-endpoint editor for path, parameters, headers, auth, request schema, responses (status+schema+examples), behavior, linked scenarios, mocks, schema versions.

## File
`src/routes/api.$apiId.endpoint.$endpointId.tsx`

## Tabs
1. **Overview** — summary, description, tags, authRequired, authProfile, behavior (default latency, default scenario, retryable)
2. **Request** — parameters table (CRUD: name, in, required, type, example, enum), requestBodySchema (JSON Schema textarea), requiredHeaders, fixedHeaders, redactedHeaders
3. **Responses** — list of EndpointResponse rows; each: statusCode, description, schemaText (textarea), examples[]
4. **Scenarios** — linked Scenario list with run buttons
5. **Mocks** — recent MockArtifact list + "Generate" CTA → opens Mock Forge
6. **Versions** — SchemaVersion list + "Snapshot current schema" button

## Validation
- JSON Schema text parsed live; red banner if invalid; Save disabled until valid.
- Parameter `in=path` required true enforced.

## Store contract
```ts
upsertEndpoint(endpoint: Endpoint): void
upsertEndpointResponse(endpointId, response: EndpointResponse): void
upsertSchemaVersion(version: SchemaVersion): void
```

## Acceptance
- Given valid response schema text, when save, then state updated and audit `endpoint.response.upsert`.
- Given invalid JSON in schema, save button disabled + red banner.
- Given role=viewer, all inputs read-only.
