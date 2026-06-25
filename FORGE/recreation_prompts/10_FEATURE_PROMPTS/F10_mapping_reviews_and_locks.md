# F10 — Mapping Reviews + Production Lock

## Goal
Approval workflow for FieldMapping changes with reviewer RBAC, notes, soft lock, and a production lock that only owners can set/remove and that gates editor mutations.

## Files
- `src/routes/mapping-reviews.tsx`
- store: `requestMappingReview`, `addReviewNote`, `decideReview`, `setMappingLock`
- store: `upsertMapping`/`deleteMapping` ENFORCE productionLock

## Workflow
1. Editor creates/edits mapping → calls `requestMappingReview(mappingId, reviewerRole)` → creates MappingReview(status="pending", reviewerRole).
2. Reviewer (must have a role >= reviewerRole) opens review:
   - Add note (free text + author + at)
   - Decide: approve / reject / changes_requested
3. Owner can set `setMappingLock(mappingId, locked=true, production=true)` → review.productionLock=true.
4. Any `upsertMapping(m)` checks:
   ```ts
   const lock = get().isMappingLocked(m.id);
   if (lock.production && get().currentRole !== "owner") {
     get().pushAudit("mapping.write.denied", `mapping:${m.id}`, "production-locked");
     return;
   }
   if (lock.locked && !can(get().currentRole, "mapping.approve")) {
     get().pushAudit("mapping.write.denied", `mapping:${m.id}`, "locked");
     return;
   }
   ```

## Helper
```ts
isMappingLocked(mappingId): { locked: boolean; production: boolean }
```

## UI `/mapping-reviews`
- Tabs: Pending / Approved / Rejected / Changes Requested
- Card per review:
  - Mapping summary (source → target field, transform)
  - Requested by + at, reviewer role required
  - Notes timeline (author + at + text)
  - Add note textarea
  - Action buttons (role-gated): Approve / Request changes / Reject
  - Lock toggles: "Soft lock" (editor+) / "Production lock" (owner only)
  - Production lock badge shown when active

## Acceptance
- Given productionLock=true and role=editor, when upsertMapping called, then no-op + audit `mapping.write.denied` with summary "production-locked".
- Given role=owner, can set productionLock=true.
- Given reviewerRole=owner, editor cannot decide.
- Add note → review.notes appended.
