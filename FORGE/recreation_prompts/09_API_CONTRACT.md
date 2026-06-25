# 09 — API Contract (Layer 23)

Mivel nincs backend, az "API contract" itt **a Zustand store actions** + **a pure lib függvények** + **a gateway export formátum**. Minden ezeken keresztül történik.

## Store actions (`src/domain/store.ts`)

```ts
// Identity / impersonation
setCurrentEnvironment(name: string): void
setCurrentRole(role: Role): void
setActor(name: string): void

// Audit
pushAudit(action: string, target: string, summary?: string, before?: unknown, after?: unknown): void
togglePinAudit(id: string): void
pruneAudit(): number
pruneSnapshots(): number
pruneExecutions(): number

// Import
createImportJob(input: { filename: string; source: "file" | "url"; rawText: string }): string  // returns jobId
parseImportJob(jobId: string): void
commitImportJob(jobId: string): void

// Scenarios
upsertScenario(s: Scenario): void
deleteScenario(id: string): void

// Data packs
upsertDataPack(p: DataPack): void
deleteDataPack(id: string): void

// Errors
upsertErrorDef(e: ErrorDefinition): void
deleteErrorDef(id: string): void

// Mappings (productionLock enforced inside)
upsertMapping(m: FieldMapping): void
setMappingStatus(id: string, status: "proposed"|"approved"|"rejected"): void
deleteMapping(id: string): void

// OpenAI
upsertOpenAi(c: OpenAiConfig): void
deleteOpenAi(id: string): void

// Prevalidation
togglePrevalidation(id: string): void

// Environments
addEnvironment(env: ApiEnvironment): void
removeEnvironment(id: string): void

// Issues
setIssueStatus(id: string, status: IssueStatus): void

// Endpoint editing
upsertEndpoint(endpoint: Endpoint): void
upsertEndpointResponse(endpointId: string, response: EndpointResponse): void

// Execution
runEndpoint(params: { endpointId: string; environmentId?: string; scenarioId?: string;
  mode: "mock"|"live"; bodyOverride?: string }): ExecutionRun
runScenarioSuite(params: { apiId?: string; environmentId?: string }): ExecutionRun[]

// Snapshots
createSnapshot(input: Omit<Snapshot, "id"|"createdAt"|"createdBy">): Snapshot
deleteSnapshot(id: string): void

// Mock Forge
generateMockForEndpoint(params: { endpointId: string; statusCode: string; seed?: number }): MockArtifact
injectMockAsExample(mockId: string, label?: string): void

// Members + roles
upsertMember(m: WorkspaceMember): void
setMemberRole(id: string, role: Role): void
removeMember(id: string): void

// Schema versions
upsertSchemaVersion(v: SchemaVersion): void
deleteSchemaVersion(id: string): void

// Gateway smoke
runGatewaySmoke(params: { scenarioIds: string[]; environmentName: string }): GatewaySmokeRun

// Schedules
upsertScheduledRun(s: ScheduledRun): void
deleteScheduledRun(id: string): void
runScheduleTick(): number                  // returns # fired
runScheduleNow(scheduleId: string): ScheduledRunRecord

// Reviews
requestMappingReview(mappingId: string, reviewerRole: Role, note?: string): MappingReview
addReviewNote(reviewId: string, text: string): void
decideReview(reviewId: string, decision: MappingReviewStatus): void
setMappingLock(mappingId: string, locked: boolean, production?: boolean): void

// Settings
updateSettings(patch: Partial<WorkspaceSettings>): void
reset(): void
loadTestConnections(): { added: number; skipped: number }
```

## Permissions (`src/lib/permissions.ts`)

```ts
export type Action =
  | "scenario.write" | "scenario.run" | "scenario.delete"
  | "mapping.write" | "mapping.approve" | "mapping.lock.production"
  | "openai.write" | "env.write" | "endpoint.write" | "import.commit"
  | "snapshot.create" | "gateway.export" | "gateway.smoke"
  | "role.manage" | "mock.write" | "audit.view"
  | "schedule.write" | "schema.version.write";

const MATRIX: Record<Role, Action[]> = {
  owner:    [/* all */],
  editor:   ["scenario.write","scenario.run","scenario.delete","mapping.write","mapping.approve",
             "openai.write","env.write","endpoint.write","import.commit","snapshot.create",
             "mock.write","audit.view","schedule.write","schema.version.write","gateway.smoke"],
  operator: ["scenario.run","audit.view","mock.write","gateway.smoke"],
  viewer:   ["audit.view"],
};

export function can(role: Role, action: Action): boolean { ... }
```

## Lib utility surface

```
src/lib/
├── pdf-export.ts        buildFactsheetPdf(api), buildRunReportPdf(runs), buildSchemaDiffPdf(diff), buildSmokeReportPdf(run), buildAuditPdf(entries)
├── gateway-export.ts    exportKongConfig(apis, envs) → string (YAML)
├── schema-diff.ts       computeSchemaDiff(versionA, versionB) → { added, removed, changedTypes, breaking },
│                         computeImpact(diff, scenarios, mappings, endpointId) → SchemaDiffImpact
├── mock-forge.ts        generateMock(schema, seed, status) → { payload, valid, messages },
│                         computeCoverage(schema, payload) → MockCoverageReport
├── transform-engine.ts  evalTransform(expr, sourceValue, context) → result, supports: lowercase, uppercase, trim,
│                         concat($..., $...), iso(date), default(v), substring, regex_replace, lookup(map)
├── cron-eval.ts         parseCron("every 5 min" | "daily 09:00" | "weekly mon 08:00") → CronSpec
│                         nextRunFrom(spec, now) → Date
├── permissions.ts       can(role, action), ROLE_LABEL, ROLE_DESCRIPTION
└── utils.ts             cn(...), formatRelative(date), shortId()
```

## Gateway export format (`exportKongConfig`)

Kong-style YAML:

```yaml
_format_version: "3.0"
services:
  - name: identity-users-v1
    url: https://api.example.com
    routes:
      - name: list-users
        paths: [/users]
        methods: [GET]
        plugins:
          - name: rate-limiting
            config: { minute: 60 }
          - name: key-auth
            config: { key_names: [x-api-key] }
```

## Persistence

```ts
persist(stateCreator, {
  name: "apibox-store",
  version: 5,
  migrate: (persisted, fromVersion) => {
    // each migration appends missing collections + defaults; never destructive
    if (fromVersion < 5) {
      persisted.scheduledRunRecords ??= [];
      persisted.notificationLog ??= [];
      persisted.pinnedAuditIds ??= [];
      persisted.settings ??= { executionRetentionDays: 30, auditRetentionDays: 90,
                                snapshotRetentionDays: 60, autoTickEnabled: true, tickIntervalSec: 60 };
    }
    return persisted;
  },
})
```
