# MicroMasters — Game Design Document & Implementation Blueprint

**Genre:** Hyper-casual Idle Strategy · **Platforms:** iOS / Android · **Audience:** 10–99
**Pillars:** *Understand in 10s · Reward every few seconds · Deep underneath · Delightful & readable on a phone*
**Status legend:** ✅ implemented in the current build · 🟡 partial · ⬜ planned

This document is the single source of truth. Terminology is fixed here and used everywhere
(code identifiers in `code font`). Numbers are starting values tuned for the pacing targets in
§3 and §6; they live in data (§12) so designers can retune without touching logic.

---

## 1. High Concept

You are a **MicroMaster**: a tiny overseer who builds a microscopic empire inside everyday
miniature worlds — a `kitchen` countertop, a `bathroom` sink, a `garden` bed, a `workshop`
bench. Tap to gather, grow a workforce of charming micro-units, upgrade buildings, conquer
new zones, and unlock the next world. It plays itself while you're away (idle) and rewards a
few seconds of taps when you're back (active). The fantasy is **"microscopic empire building"**:
small, cozy, satisfying, habit-forming — never stressful.

**One-sentence pitch:** *Grow a tiny civilization across miniature household worlds with one
thumb — simple on the surface, deep underneath.*

**Three-goal framing (always true on screen):**
- **Short-term (seconds):** collect resources, buy the next cheap upgrade.
- **Mid-term (a session):** finish a building tier / unlock the next zone or worker.
- **Long-term (days+):** complete a world, unlock new worlds, advance meta & prestige.

---

## 2. Core Loop

```
        ┌────────────────────────────────────────────────────────┐
        ▼                                                        │
  Enter world → Collect (tap/auto) → Spend on Upgrades ───┐     │
        ▲                                                  │     │
        │                                                  ▼     │
   Claim idle ←── Return later ←── Idle accrues ◄── World visibly grows
        │                                                  │     │
        └────────── Quests nudge next action ◄────────────┘     │
                        Unlock zone / worker / WORLD ────────────┘
```

**Moment-to-moment (active):** the in-world screen shows roaming units harvesting nodes; a
big **`COLLECT` (GYŰJTÉS)** button banks accumulated output with a juicy pop (number burst +
haptic + button bounce). Tapping the world also collects. Every spend produces a *visible*
state change: more units roaming, a building growing a tier, a locked zone opening, brighter
ambient life.

**Loop guarantees (design invariants):**
- The player can always afford *some* upgrade within ≤10s of active play in the early game.
- Every screen surfaces exactly one obvious "next action" (highlighted button or quest).
- No action is a dead end: every resource has a sink that yields visible progress (§3).

✅ Implemented: enter world, tap/auto collect, idle accrual, COLLECT with juice, visible
unit-count growth on upgrade, zone conquest, world unlock.
🟡 Quest-driven nudge, before/after building visuals.

---

## 3. Resource & Economy Design

Five resource *roles*. Each has an explicit **source → sink → purpose**; no overlap.

| Resource | Code id | Tier | Source | Sink | Purpose |
|---|---|---|---|---|---|
| **Coins** (Érme) | `coins` | Soft | Active collect + idle in active world | Unit & building upgrades, territory conquest | Primary local growth ✅ |
| **World Essence** (e.g. Crumbs/Bubbles/Pollen) | `essence[worldId]` | World-specific | Harvesting that world's nodes (refined output) | World-only buildings, zone unlocks, world prestige | Makes each world feel distinct ⬜ |
| **Gems** (Gyémánt) | `gems` | Hard/Premium | Daily/streak, world-clear, Lab faucet, shop (IAP/ads), events | World unlock, boosts, cosmetics, time-skip | Acceleration & personalization ✅ (faucet 🟡) |
| **Cores** (Mag) | `cores` | Meta | World mastery milestones, prestige | Account-wide Research Tree (§6) | Permanent cross-world power ⬜ |
| **Mastery Stars** (Csillag) | `stars` | Prestige | Prestige a world/account (§6) | Prestige tree, prestige-only content & skins | Long-term replay ⬜ |

**Anti-inflation / anti-dead-resource rules:**
- Soft (`coins`, `essence`) use **exponential cost, polynomial-ish reward** so they never
  trivialize: `cost(L) = base · g^L`; production scales with *level sum*, not `g^L`.
- Hard (`gems`) is **never required** for core progress — only convenience. All gem sinks
  have a coin/time alternative.
- Meta/prestige currencies are **monotonic faucets with capped per-run yield** (§6) so a
  single lucky run can't break the curve.
- **Storage soft cap** with overflow: when full, production *slows to 25%* (not 0) and a
  gentle "raktár megtelt" hint appears — never a hard stop, never silent loss.

**Resource detail to implement:**
- Pickup animation: node → arc to COLLECT bar (coin spray particles) ✅ (burst) / 🟡 (arc-to-bar).
- Capacity: `cap(world) = baseCap · warehouseMult` ✅.
- Idle income: §6.
- Storage/refinement buildings: §5 (Storage, Refinery) ⬜.
- Rarity tiers for `essence` nodes (common/rare/golden node = ×1/×5/×25 with a sparkle) ⬜.
- Event-only resource `eventToken` with its own shop, wiped at event end (§7) ⬜.

**Formulas (canonical):**
```
unitProd(world)      = Σ_i unit[i].base · unit[i].level
worldProd/sec        = unitProd · world.prodMult · workshopMult · territoryBonus · prestigeMult · boost
workshopMult         = 1 + 0.12·workshopLvl
warehouseMult        = 1 + 0.75·warehouseLvl
territoryBonus       = 1 + 0.05·territoriesCleared      (10 per world → up to ×1.5)
labGems/sec          = 0.5·labLvl / 3600                (gem faucet)
upgradeCost(L)       = floor(base · growth^min(L,200))  (capped to avoid Double→Long overflow)
territoryCost(t)     = floor(base · 1.4^t)
```

---

## 4. World & Level Structure

A **hub** (World Select) lists worlds as compact dioramas; each world is a self-contained
scene with **10 territories** (zones) to conquer, its own palette, `essence`, hazard, and a
mastery reward. Worlds are *small but deep* — never an empty open map.

### Worlds (4 existing + 3 new required + roadmap)

| World | id | Theme twist | Essence | Hazard (Keeper counters) | Status |
|---|---|---|---|---|---|
| Kitchen (Konyha) | `kitchen` | Onboarding world, fastest pacing | Crumbs | Ants steal Crumbs | ✅ |
| Bathroom (Fürdőszoba) | `bathroom` | Slippery: workers move faster, nodes refill faster | Bubbles | Soap slick (slows builders) | ✅ |
| Garden (Kert) | `garden` | Growth: nodes *regrow* over time, planting | Pollen | Aphids (eat Pollen) | ✅ |
| Spaceship (Űrhajó) | `spaceship` | Late "fantasy" world, premium economy | Plasma | Zero-G drift | ✅ |
| **Workshop (Műhely)** | `workshop` | **Production chains:** raw → parts → machines; Refinery-centric | Sparks | Rust (decays parts) | ⬜ new |
| **Fridge (Hűtő)** | `fridge` | **Cold/decay:** uncollected Essence spoils; Heaters keep units fast | Frost | Mold (spreads, halves a node) | ⬜ new |
| **Toybox (Játékdoboz)** | `toybox` | **Wind-up:** units run on wind-up charge; a fun "rewind" tap gives burst | Glee | Tangle (jams a unit) | ⬜ new |
| Bookshelf, Desk, Attic… | … | Future | … | … | ⬜ roadmap |

Each world's 10 territories are themed sub-zones revealed on conquest (e.g. Kitchen:
*Countertop → Sink → Spice Rack → Fridge Door → Under-Counter*). Conquering a zone:
`+5%` world production, reveals art, and at 10/10 grants a one-time **gem clear-reward** and a
**Core** for mastery. Worlds remain worth revisiting via daily quests, events, and prestige.

✅ 10-territory conquest, clear reward, locked/gem-unlock for later worlds, per-world palette
& roaming theme. 🟡 zone-specific art reveals & per-world hazard/Keeper. ⬜ Essence & twists.

---

## 5. Units, Buildings & Upgrades

### 5.1 Worker archetypes (6 — brief requires ≥5)

| Archetype | id | Role | Key stat scaling | Visual evolution |
|---|---|---|---|---|
| **Gatherer** (Bányász) | `miner` | Harvest nodes → Coins | `base·level` production | Bigger pickaxe, hard-hat tiers ✅ |
| **Carrier** (Hordár) | `carrier` | Throughput / flow to storage | production + raises effective flow | Cart → backpack → conveyor ✅ |
| **Keeper** (Őr) | `guard` | Counters the world hazard; protects output | production + hazard mitigation | Shield/tool, color glow ✅ |
| **Specialist** (Tudós) | `scientist` | World special action (research/gems) | production + Essence/gem trickle | Goggles, lab coat ✅ |
| **Builder** (Építő) | `builder` | Speeds building upgrades | −% building cost / +% build speed | Hammer, scaffolding ⬜ |
| **Explorer** (Felfedező) | `explorer` | Cheapens/auto-progresses territory conquest; finds bonus drops | −% territory cost, +rare-node chance | Map, lantern, flag ⬜ |

Units are **semi-automatic** (roam & work on their own); the only player input is *upgrading*
(spend Coins to raise a level → more roam, more output). No micromanagement. Worker count on
screen scales with total levels (capped ~16 for readability/perf) ✅.

### 5.2 Building categories (7 — brief requires ≥6)

| Category | Example | Effect | Status |
|---|---|---|---|
| **Production** | Burrow / Hive | +flat or +% base production | 🟡 (via units) |
| **Storage** | Warehouse (Raktár) | `cap = baseCap·(1+0.75·L)` | ✅ |
| **Processing/Refinery** | Mill (Malom) | Convert raw nodes → refined Essence (×value) | ⬜ |
| **Automation** | Workshop (Műhely) | +12%/L production; auto-collect at high tiers | ✅ (mult) / 🟡 (auto) |
| **Utility/Economy** | Lab (Labor) | Gem faucet `0.5·L /h`; Bank stores offline | ✅ (lab) |
| **Decorative/Landmark** | Statue, Fairy Lights | Cosmetic + small **set bonus** (collection) | ⬜ |
| **Expansion Anchor** | Zone Gate | Unlocks a territory when built/funded | 🟡 (conquest = anchor) |

Every building upgrade must *visibly* change the structure (size/material/animation/attached
detail) — this is the core "before/after" dopamine. Costs escalate (`g≈1.45–1.7`) but early
tiers are cheap to guarantee frequent wins.

### 5.3 Upgrade rules
- Tap a row in the **Upgrades sheet** (tabs: Egységek / Épületek / Boostok ✅) → spend → row
  pops + haptic + re-prices ✅.
- Show per-row marginal value ("+X/mp", "+Y 💎/h") so spend decisions are legible ✅.
- Level cap 200 (cost overflow guard) ✅; beyond that, prestige is the progression vector.

---

## 6. Idle, Offline & Progression Systems

### 6.1 Idle / Offline (first-class feature)
- **Live idle:** active world produces continuously; pending accrues to `cap` ✅.
- **Offline:** on resume, `accrueOffline = rate · min(awaySec, OFFLINE_CAP) · offlineEff`,
  credited as a **lump to Coins, decoupled from the live cap** (so 8h away ≫ 7-min cap) ✅.
  - `OFFLINE_CAP = 8h` base → +1h per Research level (§6.3); `offlineEff = 0.5` base → up to 1.0.
- **Welcome-back moment:** a summary card — "Távollétedben +X érme, +Y 💎, Z node regrown" —
  with a single **CLAIM** button and a small surprise bonus (≤10% chance of ×2) ⬜ (toast ✅).
- **Anti-frustration:** never punish inactivity; offline only *adds*. Spoilage (Fridge) only
  affects *uncollected world Essence*, never banked currency, and is capped.

### 6.2 Progression layers (all five, each a distinct need)
| Layer | Player need | Mechanic |
|---|---|---|
| Session | quick reward | collect + cheap upgrades ✅ |
| World | local growth | unit/building tiers, 10 territories ✅ |
| Account | permanent power | **Research Tree** (Cores) ⬜ |
| Collection | completion | worker skins, decoration sets, world badges, micro-bestiary ⬜ |
| Meta/Prestige | novelty/replay | **Re-Miniaturize** prestige (Stars) ⬜ |

### 6.3 Meta system A — Research Tree (account-wide, spends `cores`)
Branches: **Industry** (+% global prod), **Logistics** (+offline cap & efficiency, +auto-collect),
**Science** (+gem faucet, +rare-node chance), **Expansion** (−territory & unlock costs). Each
node: `cost = 1·1.6^owned` Cores, capped tiers. Applies multiplicatively across *all* worlds —
the reason to keep mastering worlds.

### 6.4 Meta system B — Collection / Museum
Collectible sets grant permanent small bonuses on completion: **Worker Skins** (per archetype),
**Decoration Sets** (per world, +set bonus), **World Mastery Badges** (10/10 + prestige once),
**Micro-Bestiary** (spot rare critters). 100% of a set = a Star + a cosmetic frame.

### 6.5 Prestige — "Re-Miniaturize" (permanent layer)
Reset *one world* (units/buildings/territories) for **Mastery Stars**:
`stars = floor( k · sqrt( lifetimeCoins_world / scale ) )` (monotonic, sqrt-damped so it can't
spike). Stars buy a **Prestige Tree** (permanent per-world multipliers, faster first units,
prestige-only skins). Account prestige ("New Microcosm") later, gated behind N world masteries.

### 6.6 Pacing targets (designers tune data to hit these)
| Horizon | Target experience |
|---|---|
| 0–60s | first collect + first upgrade + first visible change |
| ≤5 min | first worker tier + first territory; second unit type affordable |
| First session | clear ≥1 world objective; unlock hub reward loop & a 2nd world entry |
| First day | 2–3 worlds in play; first Research node; first prestige preview |
| First week | meaningful automation, a collection set near-complete, an event run |

✅ live+offline idle, territory progression, lab gem faucet, punchy early economy.
⬜ Research Tree, Collection, Prestige, welcome-back card.

---

## 7. Quests, Events & Retention

### 7.1 Quests (layered, never chores — reward what the player already does)
- **Main questline:** soft tutorial spine ("upgrade a Gatherer to L5", "clear a territory",
  "unlock Bathroom"). Always one active → the persistent "next action."
- **Daily tasks (3/day):** collect N, upgrade X, conquer 1, claim daily. Reset at local midnight.
- **Weekly objectives:** larger targets → gem/Core chest.
- **World challenges:** per-world ("Reach 1k/sec in Garden").
- **Achievements:** permanent milestones → Stars/cosmetics.
UI: progress bar + reward preview + one-tap **CLAIM**; completed quests glow.

### 7.2 Events (seasonal, data-driven)
- **Recurring buff:** Weekend ×2 production (banner + countdown to midnight ✅ banner/🟡 buff).
- **Event biome:** a temporary world (e.g. *Picnic*, *Holiday Tree*) with its own `eventToken`
  faucet and an **Event Shop** (cosmetics, boosts). Runs `start..end`; on end, biome locks,
  tokens convert to a trickle of gems (never silently lost).
- **Leaderboard event:** score = event output; ranked rewards (§8).

### 7.3 Retention (ethical hooks)
Streaks & **daily login** (7-day table, escalating, gem finale) ✅; timed boosts; limited
offers; **collection completion urge**; visible near-term goals; "one more zone" anticipation.
Reward cadence: a *visible* win every few seconds active, a *meaningful* win every session,
a *novelty* win most days. Caps prevent noise (e.g., max 1 surprise-bonus toast / session).

---

## 8. UI/UX & Onboarding

### 8.1 Screen map (shallow navigation)
| Screen | Purpose | Primary actions | Status |
|---|---|---|---|
| Splash | brand / warm-up load | — | 🟡 (instant) |
| Title | entry | **PLAY (JÁTSSZ)**, Login, Settings | ✅ |
| Hub / World Select | meta overview | resource bar, worlds, **+ (Shop)**, event banner, bottom nav (Worlds/Friends/Settings) | ✅ |
| In-World | core play | COLLECT, Fejlesztés, Egységek, Térkép, Boost | ✅ |
| Upgrades sheet | spend | tabs Units/Buildings/Boosts, upgrade rows | ✅ |
| Worker mgmt | inspect/assign | (folded into Units tab for v1) | 🟡 |
| Quests | goals | claim, track | ⬜ |
| Daily reward | streak | claim cell w/ celebration | ✅ |
| Settings | options | sound/haptics, reset, **share**, credits | ✅ |
| Shop | monetization | gems, battle pass, VIP, skins, ad-coins | ✅ |
| Event | seasonal | event progress, event shop | ⬜ |
| Offline reward | claim idle | summary + CLAIM | 🟡 (toast) |
| **Crash report** | diagnostics | show/share stack trace | ✅ |

Navigation is shallow: Title → Hub → World, with sheets/dialogs for everything else. Bottom
nav on the hub. One-handed: large bottom buttons, strong contrast, dark theme, big tap targets.

### 8.2 Onboarding (playable in <60s, no wall of text)
Step → reward, one action at a time, contextual highlight + ≤1 short line:
1. "Tap to collect" → coins fly in.  2. "Upgrade your Gatherer" → unit pops, more roam.
3. "Collect again" → bigger number.  4. "Conquer a zone (Térkép)" → world expands.
5. Hand control back; main quest takes over. Extended tips are optional, contextual, dismissible.

---

## 9. Visual & Audio Direction

**Visual:** bright-but-controlled palette per world; cute, simple silhouettes; soft shadows;
low-poly/cartoon readability over detail; clear interactive highlights; satisfying particles
(coin spray, sparkle, dust). Current build renders the scene on a custom **Canvas `GameView`**
(gradient sky, ground mound, roaming emoji units, resource nodes, capacity-glow, floating
reward pops, screen-shake) ✅. Every action has visual confirmation: bounce / glow / pop /
expand ✅. **Emoji are restricted to Unicode ≤7** so they never render as tofu on Android 8–12 ✅.
Roadmap: vector/sprite worker art with level-tier accessories; zone-reveal animations.

**Audio:** soft per-world ambient loops; light click/collect blips; distinct upgrade/unlock/
reward stings; rare-event cue; calm overall. Options: music vol, SFX vol, **haptics** (already
firing on collect/upgrade/claim ✅), mute. (Audio assets ⬜; haptics ✅.)

---

## 10. Monetization Model (strong but fair — core is 100% free)

| Product | Type | Effect | Rule |
|---|---|---|---|
| Rewarded ad — coins | Opt-in ad | +5 min production | Player-initiated only ✅ |
| Gem packs | IAP | Hard currency | Convenience ✅ (simulated) |
| **Battle/Season Pass** | IAP track | Seasonal cosmetic + boost rewards | Free + premium lane ✅ row / ⬜ track |
| **VIP** | Subscription | +prod %, daily bonus, auto-collect QoL | No exclusive power-gates ✅ row |
| **Skins** | IAP/gems | Worker/world/UI cosmetics | Pure cosmetic ✅ (gold skin) |
| Starter pack / offers | IAP | Time-limited bundles | Non-coercive ⬜ |
| Event bundles | IAP | Event cosmetics/boosts | ⬜ |

**Ad placement policy:** ads appear ONLY behind opt-in "watch for reward" buttons and an
optional post-prestige offer. **Never** interstitial mid-collect, never on launch, never
blocking core flow. Frequency-capped; an ad-free guarantee for paying users. *(In the current
demo all purchases/ads are simulated and labeled as such — no real SDKs, no permissions.)*

---

## 11. Technical Architecture (Android-native today; portable design)

Current stack: **Kotlin, AndroidX, Material 3, ViewBinding**, custom Canvas view, no network
(offline, no INTERNET permission). The architecture below mirrors the live code and the path
to Unity/Cocos if ported (data-driven content makes that a re-skin of the same model).

- **Game state (`GameState`)** — single in-memory authority; `coins/gems`, per-world
  `WorldState` (unit & building levels, territories, pending, gemPending, lastTick), daily,
  boost, prestige fields. All simulation rules are pure methods on it. ✅
- **Content defs (`Defs` in `Models.kt`)** — static `WorldDef/UnitDef/BuildingDef/BoostDef`
  + `DAILY` table. This is the **data-driven content layer** (§12): add a world/unit by adding
  a def, no logic changes. ✅
- **Persistence (`Storage`/`Game`)** — JSON in `SharedPreferences`; **schema-versioned**,
  rejects newer/incompatible saves → `newGame`; load & save both guarded (try/catch). ✅
- **Update loop** — Activity `Handler` ticks `tick(now)` ~5 Hz to advance pending & refresh the
  HUD; `GameView` animates at display refresh via `postOnAnimation`, **lifecycle-gated**
  (pause/resume) so it never runs backgrounded. ✅
- **Offline calc** — `accrueOffline(now, awayMs)` resume-only, cap-decoupled (§6.1). ✅
- **UI state** — per-screen Activities + `Dialogs` (bottom sheet / alert). Refresh callbacks
  (`onChange`) re-render after each mutation. ✅
- **Event system** — (planned) a small `EventBus`/listener for "resourceChanged",
  "buildingUpgraded", "zoneCleared" to drive quests/achievements without coupling. ⬜
- **Animation/audio** — view-driven property animators + `GameView` particles ✅; audio
  manager wrapping `SoundPool` ⬜.
- **Reward distribution** — single `grant(reward)` choke-point that updates state, persists,
  and emits an event (prevents duplicate/!atomic rewards). 🟡 (centralize ⬜).
- **Error-proofing** — process-wide uncaught-exception handler → on-device **`CrashActivity`**
  (separate `:crash` process, pure-framework UI) that records & shows the stack trace; launcher
  surfaces any saved trace. Plus `onDraw` wrapped so a bad frame can't crash. ✅

---

## 12. Data Schemas / Pseudo-Structures

**WorldDef** (content-author this; logic reads it):
```json
{
  "id": "fridge",
  "nameRes": "world_fridge",
  "emoji": "🧊",
  "essence": "frost",
  "prodMult": 90.0,
  "baseCap": 40000,
  "territoryBaseCost": 60000,
  "unlockGems": 300,
  "clearReward": 250,
  "hazard": "mold",
  "twist": "spoilage",            // enum: none|spoilage|regrow|windup|chain
  "palette": { "skyTop": "#7FB8E6", "skyBottom": "#15324A", "ground": "#244056", "accent": "#CFE8FF" },
  "worker": "🐧",
  "nodes": ["🧊", "🥶", "💧", "🧊", "❄"]   // Unicode<=7 only
}
```
**UnitDef / BuildingDef:**
```json
{ "id":"explorer", "nameRes":"unit_explorer", "emoji":"🧭", "role":"explorer",
  "base":18.0, "baseCost":4200, "growth":1.18, "perk":"territoryCost-3%/L" }
{ "id":"refinery", "nameRes":"bld_refinery", "category":"processing", "emoji":"⚙️",
  "baseCost":1800, "growth":1.6, "effect":"essenceValue+0.2/L" }
```
**Save (persisted, schema-versioned):**
```json
{ "schema":2, "coins":0, "gems":0, "cores":0, "stars":0, "activeWorld":"kitchen",
  "dailyStreak":0, "lastClaimDay":-1, "boostMult":1.0, "boostExpiry":0, "skinGold":false,
  "research": {"industry":0,"logistics":0,"science":0,"expansion":0},
  "collection": {"skins":["miner_gold"], "sets":{"kitchen_decor":3}},
  "worlds": { "kitchen": {
     "unlocked":true, "territories":1, "clearRewardClaimed":false,
     "units":[3,0,0,0,0,0], "buildings":[0,0,0,0,0,0,0],
     "pending":0.0, "gemPending":0.0, "essence":0.0, "prestige":0, "lastTick":0 } } }
```
**Quest / Event (data-driven):**
```json
{ "id":"q_upgrade_miner_5", "type":"upgradeUnitTo", "world":"kitchen", "unit":"miner",
  "target":5, "reward":{"coins":500}, "next":"q_clear_zone_1" }
{ "id":"evt_picnic_2026s", "biome":"picnic", "start":1719000000, "end":1719600000,
  "tokenId":"picnic_token", "shop":["skin_picnic_worker","boost_x2_1h"] }
```

---

## 13. Edge Cases & Failure Handling

| Case | Safe behavior |
|---|---|
| App closed during reward collect | State is mutated *then* persisted atomically; on relaunch, pending reflects truth — no double-grant, no loss. Centralize in `grant()`. ✅/🟡 |
| Connection lost during ad reward | Reward granted only on the SDK's verified-complete callback; on failure, no grant + retry offer. (Demo: simulated, always safe.) ✅ |
| Device clock changed backwards | `dt = max(0, now−lastTick)`; negative elapsed yields 0, never negative income; offline clamped to cap. ✅ |
| Clock jumped far forward | `min(away, OFFLINE_CAP)`; bounded. ✅ |
| Upgrade during world transition | Mutations target `activeWorld` snapshot; transition re-reads state in `onResume`; no cross-world bleed. ✅ |
| Save partially corrupt / older / newer | `fromJson` guarded → `newGame`; **schema>current ⇒ reject**; per-field `opt*` defaults + clamps (streak≥0, territories 0..10, finite doubles). ✅ |
| Quest references locked content | Quest validator skips/holds quests whose prereqs aren't met; never hard-references missing ids. ⬜ |
| Reward overflows storage | Coins/gems are unbounded `Long` (capped formulas); world `pending` clamps to `cap` with 25% trickle, never lost. ✅ |
| Unknown runtime crash | Global handler → CrashActivity shows/【shares trace; `onDraw` try/catch. ✅ |

**Principle:** never lose progress silently; fail to a safe, legible state; prefer "newGame"
over a corrupt session, but only after schema/parse failure (never on a valid save).

---

## 14. Content Expansion Strategy

All content is **data-driven** (§12): a new world/unit/building/quest/event is a new def +
strings + (optional) art — **no core-logic edits**. Roadmap cadence:
- **Worlds:** monthly drop from the backlog (Workshop → Fridge → Toybox → Bookshelf → Desk…).
- **Events:** biweekly limited biome or buff using the same `WorldDef`/event schema.
- **Workers/skins:** seasonal cosmetic sets feeding the Collection.
- **Build tiers & prestige layers:** extend caps & prestige-tree nodes via data.
- **Localization:** strings already externalized; default = Hungarian, structure ready for more.
Designer workflow: edit `Defs`/JSON tables → validate (schema lint) → ship. A def-validation
unit test (ids unique, arrays sized to `UNITS/BUILDINGS`, emoji Unicode≤7) guards content PRs.

---

## 15. Final Implementation Notes (build order)

Current build already delivers the spine (✅ items above): title→hub→world, idle+offline,
upgrades, territories, daily/streak, shop tiers, leaderboard, juice, crash-safety. Recommended
next increments, each shippable and low-risk:

1. **Stabilize:** confirm the on-device crash via the new CrashActivity trace; fix root cause.
2. **Quests + EventBus:** the persistent "next action" and the glue for retention.
3. **World Essence + Refinery + node rarity:** make worlds mechanically distinct.
4. **Research Tree (Cores)** then **Prestige (Stars)** — the long-tail.
5. **Collection/Museum** + decorative buildings (set bonuses).
6. **New worlds** Workshop/Fridge/Toybox with their twists (data-only once §3–§6 land).
7. **Audio** pass + welcome-back claim card + zone-reveal animations.

**Non-negotiables to preserve while extending:** 10-second legibility; a sub-10s reward in
early active play; one obvious next action per screen; no hard economic dead ends; everything
readable one-handed on a small dark screen; data-driven content; crash-safe persistence.

---

*This GDD is versioned with the code. When a number here changes, change the data (§12), not
the logic; when a system here is marked ⬜ and then built, flip it to ✅ in the same PR.*
