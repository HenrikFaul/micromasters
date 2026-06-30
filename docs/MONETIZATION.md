# MicroMasters — Monetization Blueprint

> Strategic plan + what is implemented in the build. Pricing is in USD; the in-game
> "purchases" are **simulated** (the APK is deliberately offline and dependency-free,
> so it ships no ad SDK or Play Billing). Every mechanic below is wired and persisted,
> ready to be connected to **Google Play Billing** (IAP/subscriptions) and a **rewarded-ad
> mediation SDK** (AdMob / LevelPlay) in a networked build.

## 1. Category classification
- **Primary:** Game → Hyper-casual **Idle / Incremental** (management sub-genre).
- **Secondary:** Light strategy (territory conquest, prestige, research).
- **Signals:** very frequent short sessions; strong reward loops and progression; offline
  accrual pulls players back; cosmetic + convenience value; high tolerance for *opt-in*
  ads; a small share of players (whales) will spend repeatedly.

## 2. Primary monetization model
**Free-to-play hybrid: opt-in Rewarded Ads + In-App Purchases (consumable gems + convenience),
on top of a Subscription (VIP) and a Season Pass.** This is the genre-standard, retention-safe
stack for idle games.

## 3. Secondary models
- **Subscription** — VIP Club (recurring, the ARPU anchor).
- **Season Pass** — repeatable time-boxed monetization for engaged players.
- **Cosmetic IAP** — gold unit skin (identity, non-pay-to-win).
- **One-time Starter Pack** — the new-player conversion on-ramp.

## 4. Why this fits
Idle games live on a long retention tail and a tiny paying minority. **Forced interstitials
and pay-to-win destroy that tail**, so the design is *trust-first*: the only ads are
**opt-in rewarded** (the player chooses to watch for a clear reward), and all paid power is
**bounded and account-wide** (VIP +25%, never "win the game"). Gems are a soft premium
currency earned in-game *and* buyable, so spenders accelerate while non-spenders still
progress. Recurring value (daily production, offline accrual) justifies a subscription;
discrete value (gem packs, starter bundle) justifies consumable IAP.

## 5. Recommended pricing (real build)
| Item | Type | Price (USD) | Notes |
|---|---|---|---|
| Starter Pack | one-time consumable | **$1.99** | shown once to new/non-payers; ~5× value anchor |
| Handful of gems | consumable | $0.99 | entry tier (~80 gems) |
| Chest of gems | consumable | $4.99 | mid tier (~500 gems), "best value" badge |
| Pile of gems | consumable | $19.99 | top tier (~2,500 gems) |
| Remove-ads | — | n/a | **intentionally none** — ads are opt-in only |
| VIP Club | subscription | **$4.99 / mo**, $39.99 / yr | +25% production, +4 h offline cap, daily gems |
| Season Pass | time-boxed | **$7.99 / season** | doubled daily/quest rewards for the season |
| Gold Units | cosmetic non-consumable | $2.99 (or 40 gems) | identity, no power |

Annual VIP creates a meaningful discount (≈33% off monthly). Layered gem ladder lets whales
spend deeply while the $0.99 tier converts the curious.

## 6. Ad formats & placement
- **Rewarded video only.** Placements, all opt-in: "watch → +5 min production", "watch → +15 💎",
  the **2× offline-earnings** button on the welcome-back screen, and the in-game 2× boost.
- **No** banners, **no** interstitials, **no** app-open ads — they would interrupt the calm
  idle loop and cap a rewarded eCPM that's far more lucrative here.
- Frequency: rewarded offers are always available but never auto-triggered; the player pulls them.

## 7. Paywall logic
There is **no hard paywall** — the whole game is free. "Soft paywalls" are contextual offers:
- **Starter Pack** surfaces in the shop until claimed (one-time).
- **VIP / Season Pass** are shown in the shop's Premium section with their live value, and
  reflect an **AKTÍV** owned-state once purchased (no re-charge, no dark pattern).
- Gem sinks (world unlocks, boosts, gold skin) create *organic* demand for gems without ever
  blocking core progression.

## 8. Premium feature set
- **VIP Club:** +25% global production (one extra multiplier in the canonical formula),
  +4 h offline cap (12 h base → up to 28 h ceiling), +15 💎 per daily claim.
- **Season Pass:** ×2 daily reward (and a natural home for a reward track).
- **Gold Units:** glowing gold worker skin (cosmetic).
- **Starter Pack:** 100 💎 + a large coin injection + a 15-min 2× boost, once.

## 9. Free-tier limits (deliberately generous)
None that block play. The "limits" are *time* (offline cap, boost durations) and *premium
currency scarcity* — both relieved by either patience, rewarded ads, or IAP. The free player
can reach every world and prestige; paying only accelerates.

## 10. Conversion triggers
- First world cleared / first prestige (player has felt the loop → Starter Pack lands well).
- Storage full repeatedly (offline cap pressure → VIP value is obvious).
- Wanting the next world unlock (gem demand → gem pack or rewarded gems).
- Daily streak engagement (Season Pass doubles the reward they already value).

## 11. Upgrade messaging
Value-first, honest, specific: "Termelés ×1.25 · +4 óra offline · napi 15 💎" (VIP),
"100 💎 + nagy érme-bónusz + 2× boost — egyszeri" (Starter). No countdown manipulation,
no fake scarcity beyond the genuine one-time Starter Pack.

## 12. Retention protections
- Opt-in ads only; no forced interruptions.
- No pay-to-win: paid multipliers are bounded and the game is beatable free.
- Cosmetics never affect balance.
- Purchases persist and show owned-state (no accidental double-charge).
- Sound/odds/values are transparent.

## 13. Implementation order
1. ✅ Premium currency plumbing (gems earn + grant) and gem sinks.
2. ✅ Rewarded-ad value-exchange placements (offline 2×, +coins, +gems, 2× boost).
3. ✅ VIP economy hooks (production mult, offline cap, daily gems) + owned-state.
4. ✅ Season Pass (reward doubling) + Starter Pack (one-time bundle) + cosmetic.
5. ⏭ Wire to **Play Billing** (real IAP/subscription entitlements) + **rewarded-ad SDK**.
6. ⏭ Server/Play receipt validation; remote price/offer config; A/B the Starter Pack price.

## 14. Risks & anti-patterns avoided
- ❌ Forced interstitials in a calm idle loop → ✅ rewarded-only.
- ❌ Pay-to-win → ✅ bounded, account-wide, beatable-free.
- ❌ Subscription with no recurring value → ✅ VIP gives daily, ongoing benefit.
- ❌ Unusable free tier / aggressive gating → ✅ generous free progression.
- ❌ Dark-pattern paywalls → ✅ transparent owned-state, one honest one-time offer.

## 15. Final blueprint (one line)
**Free-to-play idle game monetized by opt-in rewarded video + a tiered gem IAP ladder, with a
VIP subscription as the ARPU anchor and a Season Pass + one-time Starter Pack as the
conversion on-ramps — all bounded to protect retention and the no-pay-to-win promise.**

---
*In this offline demo the storefront is fully functional but purchases are simulated. The
hooks (`GameState.buyVip()/buySeasonPass()/claimStarter()/addGems()`, the `vipMult()` term,
the VIP offline-cap and daily-gem bonuses, and the rewarded-ad grants) are the exact seams a
networked build connects to Google Play Billing and an ad mediation SDK.*
