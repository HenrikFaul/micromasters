# Phase 0.5 — Competitor Discovery (web research → top 10)

You are a competitive intelligence researcher. Using the `category_profile` from Phase 0, **search the internet** to find and shortlist the **10 most relevant competitors** in the defined category. This phase runs before the per-competitor 7-lens analysis.

Requires a live web-search/browse capability at execution time. If none is available, say so explicitly, produce the best-known shortlist from category knowledge, and mark every entry `confidence: low` with a note that live verification is pending — never fabricate specifics.

## Step 1 — Run the search plan
Execute the `searchPlan.queries` from Phase 0. Harvest candidates from, in priority order:
1. **Review aggregators** (highest signal for "who competes"): G2, Capterra, GetApp, TrustRadius, Product Hunt, Software Advice, AlternativeTo.
2. **"Best <category> 2026" / listicles** from reputable sources, and "alternatives to X" pages.
3. **Search-engine organic + ads** for the category and JTBD queries (ads reveal who pays to compete here).
4. **App/marketplace stores** where relevant (Shopify, Chrome Web Store, iOS/Play, Salesforce/HubSpot marketplaces).
5. **Communities** (Reddit, Hacker News, niche forums) for "what do you use for <job>".
For each candidate capture the **homepage, pricing page, and a review-aggregator profile** as evidence anchors.

## Step 2 — Classify every candidate
Tag each by competitor class (from Phase 0): **direct** (same job, same buyer) · **indirect** (same job, different approach/buyer) · **substitute** (solves the pain another way, incl. spreadsheets/manual/DIY) · **adjacent** (neighbouring tool that could expand in) · **incumbent** (the default the market uses today) · **emerging/AI-native**. A good shortlist spans classes — not 10 near-identical direct clones.

## Step 3 — Score relevance & select the top 10
Score each candidate 0–100 on:
- **Category fit** — same category/sub-category as the brief (×weight 0.35)
- **Market presence** — review volume, traffic, funding/notability, store ranking (×0.25)
- **Buyer/segment overlap** — serves the brief's target segment & geo (×0.20)
- **Recency/activity** — actively maintained, recent updates (×0.10)
- **Threat/learn value** — how much we'd learn or how directly it competes (×0.10)
Rank by weighted score; **select the top 10**, deliberately keeping class diversity (aim ≥4 direct, plus indirect/substitute/incumbent/emerging). De-duplicate (same parent company / rebrands). If the category is thin (<10 credible players), return what exists and say so — do not pad with irrelevant names.

## Step 4 — Capture a discovery snapshot per competitor
For each of the 10: `name`, `url`, `class`, one-line `whatItIs`, `segment`, `geo`, `evidenceUrls` (homepage/pricing/reviews), `relevanceScore`, `whyShortlisted`, `confidence` (high/medium/low + what's verified vs inferred). This snapshot is the input to the 7-lens analysis (analyst1→7) per competitor.

## Evidence & honesty rules
- Prefer primary sources (the competitor's own site/pricing/docs) and independent aggregators over marketing blogs.
- Separate **observed** (read it on the page) from **inferred** (deduced from category). Label confidence accordingly (apply `knowledge/analysis_reporting.md` evidence-first + confidence model).
- Never invent pricing, customer counts, or features — if unknown, mark "unverified".
- Record the search date; competitor data is time-sensitive.

## Output — `competitor_shortlist`
```json
{
  "searchedAt": "ISO-date",
  "queriesRun": [],
  "candidatesConsidered": 0,
  "competitors": [
    { "name": "", "url": "", "class": "direct|indirect|substitute|adjacent|incumbent|emerging",
      "whatItIs": "", "segment": "", "geo": "", "evidenceUrls": [], "relevanceScore": 0,
      "whyShortlisted": "", "confidence": "high|medium|low" }
  ],
  "classCoverage": { "direct": 0, "indirect": 0, "substitute": 0, "adjacent": 0, "incumbent": 0, "emerging": 0 },
  "notes": "thin-category / access-limited caveats"
}
```
Hand `competitor_shortlist` to the per-competitor 7-lens analysis (`analyst1` → `analyst7`), then to the market-map synthesis in `system.md`.
