# Shotlist — product spec

**One line:** your screenshots become things that happen.

**Ambition (Jaiven, 2026-08-31):** this is a super-app — "everything that made all
apps popular in one." The engine spine (capture → understand → act → remember) is a
module platform: events first, then calorie scan, plant ID (port from
`~/Desktop/PlantMap`), cycle tracking, and whatever earns its tab. Each module ships
as its own update — every release is a fresh TikTok demo.

**Design system:** LIQUID GLASS. Translucent layered surfaces with real backdrop blur
(Haze), refraction highlights, depth, spring physics. Dark-first, Material You dynamic
color underneath. Nobody has iOS-glass on Android — the look is part of the pitch.

**Shell:** four glass tabs — **Inbox** (screenshots→actions, the wedge) · **Scan**
(live camera modules: food/calories, plants, anything) · **Track** (cycle, habits,
streaks) · **You** (vault, privacy dashboard, settings).

**The demo bar** (from `research/android-screenshot-ingest-validation.md`): on a normal
user's existing screenshots, find ≥3 genuinely useful missed/upcoming actions in under
one minute, with no typing. If the backfill can't do that, nothing else matters.

**The law:** events/deadlines first. Every other feature is a passenger until the
flyer→calendar loop is undeniable. Marketing never says "organize your screenshots" —
it shows the phone answering back.

## Loops

1. **Moment loop** — new screenshot detected → extraction → notification action card:
   *"Friday 8pm at The Catalyst — add it?"* One tap, calendar entry with the screenshot
   attached, day-before reminder armed.
2. **Ritual loop** — the inbox: swipeable cards of everything actionable found,
   Accept / Snooze / Trash. Cleared inbox feels like inbox-zero.
3. **Rescue loop** — backfill: first run scans the existing graveyard and reveals what
   it found. This IS the TikTok clip and the onboarding wow.

## Feature tiers

### V1 — ship to Jaiven's phone
- Backfill scan with staged reveal ("2,481 screenshots read · 7 events · 3 codes · $214 of things you meant to buy")
- Live ingest: MediaStore observer + WorkManager, OEM path quirks per validation doc
- ML Kit OCR (bundled, on-device, all devices) + rule/regex extraction: dates, times,
  addresses, URLs, phones, prices, wifi/door codes, tracking numbers
- Intent classifier (Tier 1, local): event / deadline / product / place / code /
  recipe / meme / noise — with confidence + "needs review" state
- Event → calendar with confirmation, edit, undo; day-before + morning-of reminders
- Smart inbox UI: swipeable action cards, category tabs, spring animations,
  Material You dynamic color, dark-first
- Full-text search over every screenshot ever OCR'd ("wifi", "that thai place")
- Expiring flags: coupons/tickets get countdown badges
- Share-sheet ingest (store-compliant fallback path, ships now, exercised in v1)
- Privacy dashboard: "0 bytes have left your phone" counter — a feature, not a footnote
- Notification action cards with inline Accept/Dismiss

### V1.5 — fast follows
- Vault: codes/IDs/sensitive shots behind biometric, auto-suggested
- Junk cleaner: duplicates, blur, dead memes → "free 1.2 GB?" (second viral clip)
- Weekly digest: shareable "my week in screenshots" card
- Widgets: Next Up (pinned upcoming actions), quick-search
- Recipe → ingredient checklist; contact → save-contact; tracking № → carrier link
- Gemini Nano / AICore tier on capable devices (better titles, visual-only flyers)
- Multi-language OCR packs

### Module roadmap (the super-app build-out)
- **Scan › Food**: point at a plate → calories/macros card, daily log, streaks
  (the Cal AI loop, inside the platform)
- **Scan › Plants**: identify + care reminders — port logic/assets from PlantMap
- **Track › Cycle**: private on-device period tracking + predictions (post-Roe
  privacy story is a genuine differentiator: zero cloud, provable)
- **Track › Habits**: lightweight streaks tied to actions completed
- Modules share: the engine pipeline, Room store, glass design system, and the
  privacy dashboard. One module ships per release cycle; each gets its own clip.

### V2 — money + reach
- Price watch on product screenshots (backend; pings on drop)
- Free tier: backfill capped + N actions/week. Pro: unlimited actions, vault, price
  watch, digest history — ~$3.99/mo or ~$24.99/yr (test pricing)
- Play Billing + Play Store submission (core-use declaration prepared; share-sheet
  mode is the fallback posture). Sideload/beta phase: everything free.
- iOS, then desktop companion — only after Android proves the loop

## Architecture

```
app.shotlist
├── engine/     ingest (observer, screenshot filter, dedupe), ocr, extract (regex/rules),
│               classify (intent + confidence), pipeline (WorkManager)     [Claude]
├── data/       Room: Shot, Finding, ActionItem, prefs via DataStore       [Claude]
├── actions/    calendar writer, reminder scheduler, notifier, intents     [Codex]
├── ui/         inbox, cards, search, categories, theme, animations       [Codex]
└── onboarding/ permission education, backfill reveal                      [Codex]
```

Boundary contract: `ui/`+`onboarding/` consume `data/` DAOs and enqueue `engine/`
work by name; `actions/` is invoked only from confirmed user taps (no blind
automation — validation doc rule 3). All extraction local; no network in v1 at all.

## Non-negotiables
- No cloud calls in v1. Not one.
- Nothing auto-commits to the calendar without a user tap.
- Non-action screenshots must be rejected fast (meme pollution kills the inbox).
- Every push builds an installable APK via GitHub Actions; a broken main is a
  protocol violation.
