# F12 — Dashboard Widgets

## Goal
Replace placeholder index with 4 governance widgets on `/`.

## File
`src/routes/index.tsx`

## Layout
Grid `md:grid-cols-2` gap-4, 4 cards:

### 1. Latest Smoke
- Heading: "Latest gateway smoke"
- Show most recent GatewaySmokeRun:
  - When (relative) · creator · profile
  - Pass/fail summary: `X passed / Y failed`
  - Link: "Open run" → `/gateway-smoke`
- Empty: "No smoke runs yet" + CTA

### 2. Pending Mapping Reviews
- Top 5 MappingReview where status='pending'
- Each row: mapping label · reviewer role required · requested by · "Open"
- Link: "All reviews" → `/mapping-reviews`

### 3. Upcoming Runs
- Next 5 enabled ScheduledRuns sorted by `nextRunAt`
- Each row: name · cron · next run (relative) · "Run now" button
- Link: "All schedules" → `/schedules`

### 4. Audit Feed
- Last 10 AuditEntry (pinned first)
- Compact rows: time · actor · action · target
- Link: "Full log" → `/audit`

## Selectors
**ALL** use `useShallow` for filtered/sliced arrays:
```ts
const latestSmoke = useApiBoxStore((s) =>
  s.gatewaySmokeRuns[s.gatewaySmokeRuns.length - 1] ?? null);  // primitive ok
const pendingReviews = useApiBoxStore(useShallow((s) =>
  s.mappingReviews.filter((r) => r.status === "pending").slice(0, 5)));
const upcoming = useApiBoxStore(useShallow((s) =>
  [...s.scheduledRuns].filter((r) => r.enabled)
    .sort((a, b) => +new Date(a.nextRunAt) - +new Date(b.nextRunAt)).slice(0, 5)));
const recentAudit = useApiBoxStore(useShallow((s) => {
  const pinned = s.audit.filter((a) => s.pinnedAuditIds.includes(a.id));
  const rest = s.audit.filter((a) => !s.pinnedAuditIds.includes(a.id)).slice(-10).reverse();
  return [...pinned, ...rest].slice(0, 10);
}));
```

## Acceptance
- Given seed/test connections, all 4 widgets render non-empty.
- Empty state on each widget when collection empty (no blank divs).
