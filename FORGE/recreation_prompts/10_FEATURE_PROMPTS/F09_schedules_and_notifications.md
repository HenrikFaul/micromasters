# F09 — Scheduled Runs + Notifications + Retention

## Goal
Cron-like scheduled scenario suite runs with simulated email/webhook notifications, retention policies, run history drill-down, manual "Run now".

## Files
- `src/lib/cron-eval.ts`
- `src/hooks/useScheduleTick.ts`
- `src/routes/schedules.tsx`
- store: `upsertScheduledRun`, `deleteScheduledRun`, `runScheduleTick`, `runScheduleNow`

## Cron grammar (`parseCron`)
Supported strings:
- `every N min` (1..1440)
- `every N hour` (1..24)
- `daily HH:MM` (24h)
- `weekly DOW HH:MM` where DOW ∈ {mon,tue,wed,thu,fri,sat,sun}

Returns `{ kind, n?, hour?, minute?, dow? }` or `null`. `nextRunFrom(spec, now)` computes Date.

## `useScheduleTick()`
```ts
useEffect(() => {
  const settings = useApiBoxStore.getState().settings;
  if (!settings.autoTickEnabled) return;
  const id = setInterval(() => useApiBoxStore.getState().runScheduleTick(),
                          settings.tickIntervalSec * 1000);
  return () => clearInterval(id);
}, []);
```

Mounted once in `AppShell`. Use `getState()` inside callback, never selectors.

## `runScheduleTick()`
1. Now = Date.now().
2. For each `scheduledRuns` where `enabled && nextRunAt <= now`:
   a. Call `runScenarioSuite({ scenarioIds, env })` → ExecutionRun[].
   b. Append ScheduledRunRecord with counts.
   c. For each notify channel (email/webhook): if `onSuccess && record.status='passed'` OR `onFailure && record.status!='passed'` → append NotificationLogEntry (status='delivered' for simulated; if webhookUrl invalid → 'failed').
   d. Update schedule.lastRunAt + nextRunAt = nextRunFrom(parseCron(cron), now).
3. `pruneExecutions()` + `pruneSnapshots()` + `pruneAudit()` per retention.
4. Return number of schedules fired.

## `runScheduleNow(scheduleId)`
Same as one tick iteration but trigger='manual', ignore nextRunAt gate.

## UI `/schedules`
- Top: "New schedule" button
- Table: name · cron · next run (relative) · last run · status badge · enabled toggle · row actions (Run now, edit, delete)
- Row expand: last 10 ScheduledRunRecord + NotificationLogEntry rows

## Acceptance
- Given cron `every 1 min`, when 60s elapses, then `runScheduleTick` fires it, ScheduledRunRecord appended, NotificationLogEntry per channel.
- "Run now" creates trigger='manual' record.
- Setting retentionDays=1 prunes records older than 1d on next tick.
