# Android Screenshot Ingest Validation

## Verdict

Shotlist-style background screenshot ingest is technically plausible on Android, but it is not a zero-risk primitive.

The safest v1 architecture is:

1. Local-first import from the user's Screenshots collection.
2. New-screenshot detection through `MediaStore` observation where permission is granted.
3. ML Kit OCR as the baseline extraction layer.
4. Gemini Nano / AICore only as an opportunistic enhancement on supported devices.
5. Cloud classification only as explicit opt-in, default off.

This preserves the privacy pitch and lets the product validate the core behavior before relying on Play Store approval or uneven on-device VLM availability.

## Product constraint

The product should market one primary loop:

> Screenshots with dates, times, places, tickets, bills, or invitations become reviewed calendar/reminder actions.

Do not launch as a generic "organize every screenshot" app. That category already has live competitors and weak urgency. Events/deadlines are the cleanest daily-value wedge because the nudge is naturally time-bound.

Secondary loops such as shopping, price tracking, recipes, codes, and memes should be treated as later retrieval/action types, not the v1 promise.

## Ingest options

| Option | UX | Permission posture | Reliability | Notes |
| --- | --- | --- | --- | --- |
| Share sheet | User taps Share after taking a screenshot | Low risk | High | Store-friendly fallback, but loses the magic zero-labor demo. |
| Photo Picker / selected import | User grants selected screenshots | Low risk | High | Good for onboarding/backfill. Not automatic for new screenshots. |
| Broad photo access + `MediaStore` observer | App notices new screenshot media | Higher risk | Medium | Best magic loop, but needs policy justification and OEM validation. |
| Accessibility/screen capture service | App observes screen directly | Very high risk | Fragile | Avoid for v1. Privacy optics are bad and platform restrictions are stricter. |
| Cloud photo account integration | Syncs from Google Photos/other cloud | High product and privacy risk | Variable | Avoid. Adds auth, trust, and platform dependency before product proof. |

## Permissions and policy risk

Android's privacy direction favors user-selected media access. The Android Photo Picker gives temporary access to chosen files without broad storage permission, but it does not support passive background ingestion of every new screenshot.

For automatic screenshot detection, the app likely needs frequent access to images created by other apps. Google Play treats broad or frequent photo/video access as sensitive and expects the app to prove that access is core functionality. Shotlist can make that argument, but it should not be assumed to pass without review friction.

Implementation implication:

- v1 sideload/internal distribution can validate demand using broad image access.
- Store-ready v1 must include a fallback mode based on share sheet and user-selected import.
- The Play submission should frame broad screenshot access as the app's core function, not an optional convenience.
- Onboarding must explain that only screenshots are indexed, not the full camera roll as a user-facing promise. Internally, still design defensively because OS permissions may expose more than screenshots.

## New screenshot detection

A practical Android approach is:

1. Register a `ContentObserver` on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`.
2. On change, query recent images ordered by date added/taken.
3. Filter likely screenshots by:
   - relative path containing `Screenshots`,
   - display name patterns,
   - MIME type/image dimensions,
   - date within a small window.
4. Deduplicate by media ID and content hash.
5. Queue OCR/extraction work through `WorkManager`.

Validation needed on real devices:

- Pixel / stock Android
- Samsung Gallery devices
- OnePlus / Oppo style gallery paths
- Xiaomi / Redmi style gallery paths
- Devices using third-party screenshot tools

Known weak points:

- OEMs vary screenshot folder names and metadata.
- Some devices delay media indexing.
- Users may deny broad image permission or select limited media access.
- Battery optimizers can delay background work.
- A screenshot taken inside a secure app may not be saved or may be blank.

## Extraction tiers

| Tier | Availability | What it should do | Product promise |
| --- | --- | --- | --- |
| Tier 0: OCR + rules | Broad Android support with ML Kit/Text Recognition or platform OCR | Extract text, dates, times, URLs, phone numbers, addresses, candidate titles | Baseline; works locally. |
| Tier 1: OCR + lightweight classifier | Broad if bundled/on-device model is small enough | Classify event, bill, reservation, product, recipe, code, meme/non-action | Best default v1 path. |
| Tier 2: Gemini Nano / AICore | Supported-device dependent | Better summaries and intent classification; possible image understanding | Enhancement, not required for core. |
| Tier 3: Cloud VLM | Universal network fallback if enabled | Rich classification of visual-only flyers/products | Opt-in only; default off. |

The v1 action loop should not require a general VLM. Most high-value event/deadline screenshots contain text. OCR plus deterministic date/time extraction and a small classifier is enough for the first product test.

## Competitor map

Live or near-live products already cover parts of the concept:

- Screenshot Organizer & Search: Android app claiming automatic screenshot sorting, on-device OCR, capture-time notes, reminders, old screenshot import, no cloud, and Gemini Nano labels on supported phones.
- Bloop: iPhone app/site focused on screenshot actions, including calendar events, reminders, links, notes, and email drafts; local OCR/rules.
- SnapAction: screenshot-to-actions product focused on calendar events, reminders, shopping lists, and other action types; iPhone-focused.
- Capture To Do: screenshot-to-task/reminder app with free/pro pricing and Android/iOS positioning.
- mymind: broader save-anything memory product with OCR/image tagging and mobile/desktop apps; more manual-save and prosumer aesthetic.
- Google Photos / Samsung Gallery: strong default storage/search surfaces, but not focused on turning screenshots into actions.

Conclusion: this is not an empty market. The defensible claim is that no mass consumer brand owns "screenshots become actions," not that nobody has built it.

## Differentiation requirements

To be worth building, Shotlist needs a sharper v1 than existing wrappers:

1. Events/deadlines first, not generic organization.
2. Passive new-screenshot detection after permission, not share-only.
3. Reviewed one-tap action creation, not blind automation.
4. Local-first extraction with clear privacy language.
5. A backfill demo that finds missed events/deadlines in the existing screenshot graveyard.
6. Fast rejection of non-action screenshots so memes and random images do not pollute the action inbox.

If the product cannot beat Bloop/SnapAction on the first 30 seconds of onboarding, it should not proceed.

## MVP validation plan

Build and test these before any broader product scope:

1. Permission flow:
   - selected import fallback,
   - broad screenshot access path,
   - denial/retry copy.
2. Backfill:
   - scan last 100 screenshots,
   - identify actionable event/deadline candidates,
   - show review cards.
3. New screenshot ingest:
   - detect new screenshots through `MediaStore`,
   - enqueue OCR,
   - create local action suggestion notification.
4. Extraction:
   - OCR text,
   - date/time parsing,
   - title/location inference,
   - confidence score,
   - safe "needs review" state.
5. Calendar/reminder action:
   - create only after user confirmation,
   - include screenshot thumbnail/context,
   - allow edit/undo.
6. Device matrix:
   - at least one Pixel,
   - one Samsung,
   - one budget/non-stock Android device.

## Open risks

- Competitor risk is real. Existing products are near-exact, but available evidence does not show a breakout consumer brand yet.
- Play Store review may reject or constrain broad photo access unless the core-use case is presented clearly.
- Android limited-photo-access UX may weaken the zero-labor story.
- Fully local rich classification may not be available on enough devices; v1 must work with OCR-only.
- The product could collapse into "another reminders app" unless the screenshot-specific demo is materially better.

## Recommendation

Proceed only with an events/deadlines-first Android prototype. Do not build generic screenshot organization first.

Success criterion for the prototype:

> On a normal user's existing screenshots, the app finds at least three genuinely useful missed or upcoming actions in under one minute, with no typing.

If that fails, the idea should be killed before investing in broader UI, subscriptions, price tracking, or cross-platform work.
