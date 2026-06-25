# F13 — Test Connections (4 public APIs)

## Goal
One-click load of 4 free no-auth public APIs into the current workspace so every feature is immediately testable with real specs and base URLs. Non-destructive merge.

## File
`src/domain/test-connections.ts` — exports `buildTestConnections(workspaceId, actor)` returning:
```ts
{
  domains: BusinessDomain[];
  apis: ApiAsset[];
  endpoints: Endpoint[];
  scenarios: Scenario[];
  mappings: FieldMapping[];
  dataPacks: DataPack[];
  scheduledRuns: ScheduledRun[];
  environments: ApiEnvironment[];
}
```

## APIs

### 1. JSONPlaceholder (REST CRUD baseline)
- baseUrl: `https://jsonplaceholder.typicode.com`
- Endpoints: `GET /posts`, `GET /posts/{id}`, `POST /posts`, `GET /comments`
- Schemas: Post(id, userId, title, body), Comment(id, postId, name, email, body)
- 4+ scenarios (happy, 404, validation_error)

### 2. PokeAPI (large schema, mocks)
- baseUrl: `https://pokeapi.co/api/v2`
- Endpoints: `GET /pokemon/{name}`, `GET /pokemon?limit=&offset=`
- Schema covers nested arrays (types[], stats[]) → good Mock Forge target

### 3. Open-Meteo (query params heavy)
- baseUrl: `https://api.open-meteo.com/v1`
- Endpoint: `GET /forecast?latitude=&longitude=&hourly=`
- Many query parameters → query param validation showcase

### 4. REST Countries (mapping showcase)
- baseUrl: `https://restcountries.com/v3.1`
- Endpoints: `GET /all`, `GET /alpha/{code}`
- Cross-API mapping: REST Countries.cca2 → JSONPlaceholder Post.userId lookup (demo only, contrived)

## Store action
```ts
loadTestConnections(): { added: number; skipped: number } {
  const bundle = buildTestConnections(get().currentWorkspaceId, get().actor);
  const mergeById = (existing, incoming) => {
    const adds = incoming.filter(x => !existing.some(e => e.id === x.id));
    return { merged: [...existing, ...adds], added: adds.length };
  };
  // For each collection: merge, count added, count skipped
  // pushAudit("test-connections.load", `workspace:${wsId}`, `added X skipped Y`)
}
```

## UI (`src/routes/settings.tsx`)
- Section "Test data"
- Button: "Load public test APIs"
- On click: toast with `added X, skipped Y` per collection
- Description: list the 4 APIs with their purpose

## Acceptance
- Given empty workspace, click → 4 APIs visible in `/catalog`, scenarios in `/scenarios`, mappings in `/mappings`, schedules in `/schedules`.
- Second click → skipped count > 0, no duplicates.
