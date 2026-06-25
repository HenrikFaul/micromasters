# 07 — UI/UX Design (Layer 22)

## Shell layout

```
+----------------------------------------------------------+
| Sidebar 244px | Topbar 56px                              |
|---------------+------------------------------------------|
|  Workspace    |  breadcrumb · search · env · role · ⋯   |
|  - Overview   +------------------------------------------+
|  - Catalog    |                                          |
|  - Import     |                  Main                    |
|               |             (overflow-auto)              |
|  Modeling     |                                          |
|  - Data Packs |                                          |
|  - Scenarios  |                                          |
|  - Mapping…   |                                          |
|  - Errors     |                                          |
|  - Schema Diff|                                          |
|               |                                          |
|  Runtime      |                                          |
|  - Environs   |                                          |
|  - Prevalid…  |                                          |
|  - Execution  |                                          |
|  - Gateway Sm.|                                          |
|  - Schedules  |                                          |
|  - Mock Forge |                                          |
|               |                                          |
|  Intelligence |                                          |
|  - OpenAI     |                                          |
|  - Explorer   |                                          |
|---------------|                                          |
|  Governance   |                                          |
|  - Audit Log  |                                          |
|  - Settings   |                                          |
+----------------------------------------------------------+
```

- Hidden on `md:` below — use a slide-over (already shadcn `sheet`).
- Sidebar sections: Workspace / Modeling / Runtime / Intelligence / Governance — divider lines between.
- Section header: 10.5px uppercase, tracking 0.12em, muted.
- Active nav item: `bg-sidebar-accent text-sidebar-accent-foreground`.
- Footer: status dot "All modules online" + version `v0.7.0`.

## Topbar
- Left: breadcrumb (`Catalog / Identity / Users`).
- Center: `cmd+k` palette stub (search input, opens shadcn `command`).
- Right: environment dropdown · role badge (Owner/Editor/Operator/Viewer with color) · avatar.

## Page templates

### Index / Dashboard `/`
Grid 2x2:
1. **Latest Smoke** — last GatewaySmokeRun summary + pass/fail
2. **Pending Mapping Reviews** — top 5 pending
3. **Upcoming Runs** — next 5 scheduled (sorted by nextRunAt)
4. **Audit Feed** — last 10 audit entries (pinned first)

### Catalog `/catalog`
- Search + filter (domain, lifecycle, tag)
- Card grid: API name, version chip, lifecycle badge, readiness bar, tag list, "Open" button

### API detail `/api/$apiId`
- Tabs: Endpoints / Schemas / Environments / Snapshots
- Endpoint table: method badge · path · summary · tags · weaknesses chip · open

### Endpoint designer `/api/$apiId/endpoint/$endpointId`
- Tabs: Overview / Request / Responses / Scenarios / Mocks / Versions
- Right rail: linked scenarios, run button, environment picker

### Mapping Studio `/mappings`
- Two-column source/target schema tree + center mapping list
- Right preview panel with source example → transform → output

### Schema Diff `/schema-diff`
- Endpoint + version A/B selectors
- Side-by-side schema text + colored gutter
- Impact card stack at bottom + export buttons

### Execution Console `/execution`
- Run table (newest first) with mode badge, status, duration
- Drawer on row click: request, response, trace timeline, prevalidation list

### Gateway Smoke `/gateway-smoke`
- Top: scenario picker + env picker + "Run smoke"
- Run history table; drawer with per-scenario detail; "Export PDF"

### Schedules `/schedules`
- Table: name, cron, next run, last run, status
- Row expand: last 10 ScheduledRunRecord + NotificationLogEntry

### Mapping Reviews `/mapping-reviews`
- Tabs: Pending / Approved / Rejected / Changes requested
- Card per review: mapping summary, requested by, notes timeline, action buttons (role-gated), lock toggle

### Audit `/audit`
- Filter bar (actor, action, date range)
- Pinned section, then chronological table
- Export CSV / PDF

### Settings `/settings`
- Workspace info
- Members + role manage (owner only)
- Retention policy sliders
- Impersonation (role select + actor name)
- "Load public test APIs" button
- "Reset workspace to seed" (confirm dialog)

## Component inventory (shadcn primitives used)

`button`, `card`, `tabs`, `table`, `dialog`, `drawer`, `sheet`, `dropdown-menu`, `select`, `input`, `textarea`, `label`, `badge`, `tooltip`, `popover`, `command`, `scroll-area`, `separator`, `switch`, `checkbox`, `radio-group`, `progress`, `toast` (sonner), `alert`, `alert-dialog`, `breadcrumb`, `accordion`, `collapsible`, `resizable` (mapping studio split).

## Empty states
Every list shows a CTA card with one-line description and primary action button. Never a blank `<div>`.

## Microcopy register
- Action verbs at top of pages ("Design endpoint", "Run smoke", "Approve mapping").
- Toasts: short and result-focused ("Scenario saved", "Production lock applied").
- Denied: explicit reason ("Denied — production locked. Ask an owner to unlock.").

## Accessibility
- Keyboard: nav (G H / G C ...), `/` focus search, `Cmd+K` palette, all Radix primitives keyboard-correct.
- Color contrast: AA on text against tokens; never use only color for status (badge text + icon).
- Focus rings on all interactive (`focus-visible:ring-2 ring-ring`).
