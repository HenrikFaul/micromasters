# F01 — Workspace + RBAC + Audit

## Goal
Single workspace, 4 roles (owner/editor/operator/viewer), every mutating action is audited.

## Files
- `src/lib/permissions.ts` — Action enum + MATRIX + `can()`
- `src/domain/store.ts` — `currentRole`, `actor`, `pushAudit`, role-gated actions
- `src/routes/settings.tsx` — role impersonation, actor name input

## Store contract
```ts
currentWorkspaceId: ID;
currentRole: Role;     // default "owner"
actor: string;         // default "Demo User"
audit: AuditEntry[];
pinnedAuditIds: string[];

setCurrentRole(role: Role): void   // audit "role.impersonate"
setActor(name: string): void
pushAudit(action, target, summary?, before?, after?): void
togglePinAudit(id: string): void
pruneAudit(): number
```

## RBAC enforcement
Inside every mutating action, first check `can(get().currentRole, "scenario.write")`. If false: `pushAudit("scenario.write.denied", ...)` and `return`.

## Acceptance
- Given role=viewer, when `upsertScenario` called, then no state change + audit `scenario.write.denied`.
- Given role=editor, when `upsertScenario` called, then state updated + audit `scenario.upsert`.
- Given owner-only action like `setMappingLock(production=true)`, when role=editor calls, denied + audited.

## UI
- Topbar role badge (color per role).
- Settings → role select (owner|editor|operator|viewer) + actor input.
- Toast on denied actions ("Denied — your role (Editor) cannot perform: mapping.lock.production").
