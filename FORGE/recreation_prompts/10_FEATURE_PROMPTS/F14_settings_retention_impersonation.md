# F14 — Settings (retention, impersonation, reset)

## Goal
Workspace-level controls: retention policy, schedule tick interval, role impersonation, "Reset to seed", "Load public test APIs".

## File
`src/routes/settings.tsx`
- store: `updateSettings`, `reset`, `setCurrentRole`, `setActor`

## Sections

### 1. Workspace info
- Name, slug (read-only for v1), created at, member count.

### 2. Members + roles (owner only)
- Table: name · email · role select · remove button
- Add member form: name + email + role

### 3. Retention
- Slider/number inputs:
  - executionRetentionDays (default 30, range 1..365)
  - auditRetentionDays (default 90)
  - snapshotRetentionDays (default 60)
- Toggle: autoTickEnabled (default true)
- Number: tickIntervalSec (default 60, min 10, max 600)
- Save → `updateSettings(patch)` + toast

### 4. Impersonation (demo affordance)
- Role select: owner | editor | operator | viewer (with description from ROLE_DESCRIPTION)
- Actor name input
- "Apply" → setCurrentRole + setActor + audit `role.impersonate`

### 5. Test data
- "Load public test APIs" → F13
- Toast result

### 6. Danger zone
- "Reset workspace to seed" button → alert-dialog confirm → `reset()` → reload state from `seed.ts`
- Push audit `workspace.reset`

## Acceptance
- Setting tickIntervalSec=10 → `useScheduleTick` polls every 10s after next mount (or settings change watched via effect re-run).
- Role impersonate viewer → all write buttons disabled site-wide.
- Reset clears all collections except WorkspaceSettings defaults.
