# 05 — Data Model (Layer 20)

Az alábbi a `src/domain/types.ts` **teljes**, 1:1 másolata. Bármelyik AI ezt változatlanul beilleszti és felhasználja.

```ts
// ApiBox domain entities. Pure TS, no React. Desktop-ready.

export type ID = string;

export type LifecycleStatus = "draft" | "review" | "active" | "deprecated";
export type Severity = "info" | "warning" | "error" | "critical";
export type IssueStatus = "open" | "acknowledged" | "resolved" | "ignored";
export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE" | "HEAD" | "OPTIONS";

export interface Workspace { id: ID; name: string; slug: string; description?: string; createdAt: string; }

export interface BusinessDomain { id: ID; workspaceId: ID; name: string; color?: string; }

export interface ApiEnvironment {
  id: ID; apiId: ID; name: string; baseUrl: string;
  integrationUrl?: string;
  authMode: "none" | "apiKey" | "bearer" | "basic" | "oauth2" | "custom";
  apiKeyRequired: boolean; apiKeyHeaderName?: string;
  fixedHeaders: Record<string, string>;
  timeoutMs?: number; retries?: number; notes?: string;
}

export interface ApiAsset {
  id: ID; workspaceId: ID; domainId?: ID; name: string; slug: string; version: string;
  description?: string; owner?: string; tags: string[]; lifecycle: LifecycleStatus;
  readiness: number; sourceType: "openapi" | "manual" | "imported";
  sourceFilename?: string; importedAt?: string; updatedAt: string;
}

export interface Endpoint {
  id: ID; apiId: ID; path: string; method: HttpMethod;
  summary?: string; description?: string; tags: string[]; authRequired: boolean;
  parameters: Parameter[]; requestBodySchemaId?: ID; responses: EndpointResponse[];
  scenarioIds: ID[]; dataPackIds: ID[]; issueIds: ID[];
  requestBodySchema?: string;       // JSON Schema text (Phase 5)
  requiredHeaders?: string[];
  fixedHeaders?: Record<string, string>;
  redactedHeaders?: string[];
  authProfile?: "inherit" | "none" | "bearer" | "apiKey" | "oauth2";
  behavior?: EndpointBehavior;
}

export interface EndpointBehavior {
  defaultLatencyMs?: number;
  defaultScenarioId?: ID;
  retryable?: boolean;
  perStatus?: Record<string, { latencyMs?: number; retryable?: boolean; errorDefinitionId?: ID }>;
}

export interface Parameter {
  id: ID; name: string; in: "path" | "query" | "header" | "cookie";
  required: boolean; schemaRef?: string; type?: string; example?: unknown;
  description?: string; enumValues?: string[];
}

export interface EndpointResponse {
  statusCode: string; description?: string; schemaId?: ID; examples: ResponseExample[];
  schemaText?: string;
}

export interface ResponseExample {
  id: ID; label: string; payload: unknown; source: "openapi" | "generated" | "manual";
}

export interface SchemaObject {
  id: ID; apiId: ID; name: string; jsonSchema: unknown;
  fieldCount: number; hasExamples: boolean; weaknesses: SchemaWeakness[];
}

export type SchemaWeakness =
  | "no_example" | "vague_description" | "missing_enum" | "weak_error_response"
  | "unresolved_ref" | "nullable_ambiguity" | "polymorphism_complexity"
  | "sparse_response_coverage" | "no_auth_detail";

export interface ImportJob {
  id: ID; workspaceId: ID; filename: string; source: "file" | "url"; rawText: string;
  detectedVersion?: string; status: "uploaded" | "parsing" | "ready" | "imported" | "failed";
  startedAt: string; completedAt?: string; summary?: ImportSummary; error?: string;
}

export interface ImportSummary {
  apiName: string; apiVersion: string; serverCount: number; endpointCount: number;
  schemaCount: number; enumCount: number; weaknesses: WeaknessBreakdown;
  endpoints: ParsedEndpoint[]; schemas: ParsedSchema[];
  servers: { url: string; description?: string }[];
  authSchemes: { name: string; type: string }[];
}

export interface WeaknessBreakdown {
  noExample: number; vagueDescription: number; missingEnum: number;
  weakErrorResponse: number; sparseResponseCoverage: number; noAuthDetail: number;
  unresolvedRef: number;
}

export interface ParsedEndpoint {
  path: string; method: HttpMethod; summary?: string; responseCodes: string[];
  hasRequestBody: boolean; parameterCount: number; weaknesses: SchemaWeakness[];
}

export interface ParsedSchema {
  name: string; fieldCount: number; hasExample: boolean; weaknesses: SchemaWeakness[];
}

export interface ValidationIssue {
  id: ID; workspaceId: ID; apiId?: ID; endpointId?: ID; schemaId?: ID; mappingId?: ID;
  severity: Severity;
  type: SchemaWeakness | "auth_missing" | "env_invalid" | "scenario_invalid"
        | "mapping_type_mismatch" | "mapping_missing_required" | "mapping_low_confidence"
        | "mapping_unresolved_transform" | "mock_invalid";
  message: string; guidance?: string; status: IssueStatus; createdAt: string;
}

export interface RecentActivity {
  id: ID; kind: "import" | "execution" | "scenario_created" | "edit" | "issue" | "export" | "audit";
  title: string; subtitle?: string; at: string; href?: string;
}

// ====== Phase 2 — Modeling ======
export type ScenarioKind =
  | "happy_path" | "partial_success" | "validation_error" | "auth_error"
  | "rate_limit" | "timeout" | "downstream_failure" | "ai_invalid_json" | "ai_moderation_block";

export interface Scenario {
  id: ID; workspaceId: ID; apiId: ID; endpointId: ID;
  name: string; description?: string; kind: ScenarioKind;
  statusCode: string; latencyMs: number; responseBody: string;
  headerOverrides: Record<string, string>;
  errorDefinitionId?: ID; dataPackId?: ID; createdAt: string;
}

export interface DataPack {
  id: ID; workspaceId: ID; apiId?: ID; endpointId?: ID; schemaId?: ID;
  name: string; description?: string;
  scope: "endpoint" | "schema" | "global";
  payload: string; version: string; createdAt: string;
}

export interface ErrorDefinition {
  id: ID; workspaceId: ID;
  code: string; httpStatus: number;
  category: "business" | "technical" | "validation" | "auth" | "rate_limit" | "dependency" | "ai" | "gateway";
  severity: Severity;
  message: string; retryable: boolean; retryAfterSec?: number;
  payloadTemplate: string; guidance?: string;
}

// ====== Phase 3 — Runtime & AI ======
export interface PrevalidationRule {
  id: ID; name: string; kind: "schema" | "auth" | "headers" | "rate" | "moderation" | "custom";
  description: string; enabled: boolean;
}

export interface TraceStep { ts: number; label: string; detail?: string; ok: boolean; }

export interface ExecutionRun {
  id: ID; workspaceId: ID; apiId: ID; endpointId: ID;
  environmentId?: ID; scenarioId?: ID;
  mode: "mock" | "live";
  request: { method: string; path: string; headers: Record<string, string>; body?: string };
  response: { statusCode: number; durationMs: number; body: string; headers: Record<string, string> };
  prevalidation: { ruleId: ID; passed: boolean; message?: string }[];
  passed: boolean; at: string;
  trace?: TraceStep[]; traceId?: string; bodyHash?: string; actor?: string;
}

export interface OpenAiConfig {
  id: ID; workspaceId: ID; apiId?: ID; endpointId?: ID;
  name: string; model: string; systemPrompt: string;
  temperature: number; maxTokens: number;
  structuredOutputSchemaId?: ID;
  tools: { name: string; description: string }[];
  moderationEnabled: boolean; fallbackModel?: string;
  createdAt: string;
}

// ====== Phase 4 — Architecture intelligence ======
export interface FieldMapping {
  id: ID; workspaceId: ID;
  sourceApiId: ID; sourceSchemaName: string; sourceField: string;
  targetApiId: ID; targetSchemaName: string; targetField: string;
  transform?: string;          // expression evaluated by transform-engine
  confidence: number;          // 0..1
  status: "proposed" | "approved" | "rejected";
  notes?: string;
  sourceType?: string; targetType?: string; required?: boolean;
}

// ====== Phase 5 — Enterprise ======
export type Role = "owner" | "editor" | "operator" | "viewer";

export interface WorkspaceMember {
  id: ID; workspaceId: ID; name: string; email: string; role: Role; addedAt: string;
}

export interface AuditEntry {
  id: ID; workspaceId: ID; at: string;
  actor: string; actorRole: Role;
  action: string; target: string; summary?: string;
  before?: unknown; after?: unknown;
}

export interface Snapshot {
  id: ID; workspaceId: ID; apiId?: ID;
  kind: "factsheet" | "run_report" | "gateway_export";
  label: string; version: string;
  createdAt: string; createdBy: string;
  dataUrl?: string; metaJson: string; size: number;
}

export interface MockArtifact {
  id: ID; workspaceId: ID; endpointId: ID; statusCode: string;
  seed: number; payload: string;
  valid: boolean; validationMessages: string[];
  createdAt: string;
}

// ====== Phase 6 — Governance & Operations ======
export interface SchemaVersion {
  id: ID; workspaceId: ID; endpointId: ID;
  version: string; label?: string;
  requestSchemaText?: string;
  responsesText: Record<string, string>;
  createdAt: string; createdBy: string; note?: string;
}

export interface SchemaDiffImpact {
  scenariosAffected: { id: ID; name: string; reason: string }[];
  mappingsAffected: { id: ID; field: string; reason: string }[];
  addedFields: string[]; removedFields: string[];
  changedTypes: { field: string; before: string; after: string }[];
  breaking: boolean;
}

export interface GatewaySmokeRun {
  id: ID; workspaceId: ID;
  at: string; createdBy: string;
  gatewayProfile: string;
  scenarioIds: ID[];
  results: {
    scenarioId: ID; scenarioName: string;
    endpointPath: string; method: string;
    passed: boolean; statusCode: number; durationMs: number; message?: string;
    pluginChecks: { name: string; passed: boolean; detail?: string }[];
  }[];
  passed: boolean;
}

export interface ScheduledRun {
  id: ID; workspaceId: ID;
  name: string; enabled: boolean;
  apiId?: ID; environmentId?: ID;
  scenarioIds: ID[];
  cron: string;                       // see cron-eval.ts for grammar
  nextRunAt: string; lastRunAt?: string;
  retentionDays: number;
  notify: { email?: string; webhookUrl?: string; onSuccess: boolean; onFailure: boolean; };
  createdAt: string; createdBy: string;
}

export type MappingReviewStatus = "pending" | "approved" | "rejected" | "changes_requested";

export interface MappingReview {
  id: ID; workspaceId: ID; mappingId: ID;
  requestedBy: string; requestedAt: string;
  reviewerRole: Role;
  status: MappingReviewStatus;
  decidedBy?: string; decidedAt?: string;
  notes: { id: ID; author: string; at: string; text: string }[];
  locked: boolean;          // soft lock
  productionLock: boolean;  // owner-only edit gate
}

// ====== Phase 7 — Operations close-out ======
export interface ScheduledRunRecord {
  id: ID; workspaceId: ID; scheduleId: ID;
  at: string; durationMs: number;
  scenariosRun: number; passed: number; failed: number;
  status: "passed" | "failed" | "partial";
  executionIds: ID[];
  trigger: "manual" | "auto";
}

export interface NotificationLogEntry {
  id: ID; workspaceId: ID;
  at: string; channel: "email" | "webhook";
  target: string; scheduleId?: ID; runRecordId?: ID;
  status: "delivered" | "failed" | "skipped";
  detail: string; payloadJson?: string;
}

export interface WorkspaceSettings {
  executionRetentionDays: number;
  auditRetentionDays: number;
  snapshotRetentionDays: number;
  autoTickEnabled: boolean;
  tickIntervalSec: number;
}

export interface MockCoverageReport {
  totalFields: number;
  coveredFields: number;
  uncoveredPaths: string[];
  percentCovered: number;
}
```

## Zustand store sémája (`src/domain/store.ts`)

```ts
interface ApiBoxState {
  // identity
  currentWorkspaceId: ID;
  currentEnvironment: string;
  currentRole: Role;
  actor: string;

  // collections
  workspaces: Workspace[];
  domains: BusinessDomain[];
  apis: ApiAsset[];
  endpoints: Endpoint[];
  schemas: SchemaObject[];
  parameters: Parameter[];
  environments: ApiEnvironment[];
  scenarios: Scenario[];
  dataPacks: DataPack[];
  errorDefs: ErrorDefinition[];
  mappings: FieldMapping[];
  openAi: OpenAiConfig[];
  prevalidationRules: PrevalidationRule[];
  importJobs: ImportJob[];
  validationIssues: ValidationIssue[];
  runs: ExecutionRun[];
  snapshots: Snapshot[];
  members: WorkspaceMember[];
  audit: AuditEntry[];
  mockArtifacts: MockArtifact[];

  // Phase 6/7
  schemaVersions: SchemaVersion[];
  gatewaySmokeRuns: GatewaySmokeRun[];
  scheduledRuns: ScheduledRun[];
  mappingReviews: MappingReview[];
  scheduledRunRecords: ScheduledRunRecord[];
  notificationLog: NotificationLogEntry[];
  pinnedAuditIds: string[];
  settings: WorkspaceSettings;

  // actions — see 09_API_CONTRACT.md
}

// persist({ name: 'apibox-store', version: 5, migrate: ... })
```

Selectorok használata: **mindig** `useShallow` wrap a kollekciókra. Lásd `11_TECH_IMPLEMENTATION.md` "Gotchas".
