# Shotlist Google Play readiness

Last code audit: 2026-09-01 (`f9c9a97`)

Last policy check: 2026-09-01

This is the submission source of truth for the current Android main branch. It must be
rechecked against the exact release AAB: main now contains substantially more than the
original screenshot Inbox, and several old “future feature” assumptions are no longer
true.

## Current shipping surface

| Area | What exists on current main | Store consequence |
| --- | --- | --- |
| Screenshot Inbox | Broad-library backfill, live observer, 15-minute WorkManager catch-up, share-sheet ingest, Photo Picker import, local OCR/classification, detail-first actions | Keep screenshot understanding as the first and dominant listing promise if requesting broad photo access. Do not promise immediate closed-app detection. |
| Recall | Local full-text screenshot search; billing-off release builds get full history, while debug can preview the 30-day Free limit | Do not market Pro until purchase and restore exist. |
| Scan | Camera “Anything,” bundled ML Kit barcode/QR handling, and Google Play services document scanning to PDF | Camera permission is now intentional. Docs mode has a Play services/runtime-download dependency and may be unsupported on low-RAM or non-GMS devices. |
| Track | Local menstrual-flow/cycle estimates plus habits and streaks | This is a health feature. The old “Health apps: no” answer is invalid. |
| Metabolic Lens | Read-only Health Connect glucose history, descriptive statistics, and user-added context markers | Declare the read-glucose data type and health use, publish the health-data lifecycle, and never market this as live monitoring or medical functionality. |
| Vault | Device-credential/biometric lock for sensitive findings; billing-off release builds are unlimited, while debug can preview the 3-item Free limit | Reviewers need device credentials to exercise the lock. |
| Shatter | User-confirmed MediaStore trash flow for stale screenshots on Android 11+ | Describe it as review-and-trash, never automatic deletion. |
| Other | Widgets, reminders, weekly/share cards, themes, and living backgrounds | Only market features retained in the release candidate and demonstrated with fabricated data. |

There is no account, advertising SDK, cloud sync, or connected billing implementation.
`BILLING_LIVE=false` therefore defaults release builds to full access. The honest Pro
sheet and Free limits are reachable only through the debug tier preview.

## Release verdict

**Not ready for Play submission.** Current main has a credible broad-photo-access case
and good local-data controls, but the release still has policy, disclosure, health,
SDK-verification, and packaging blockers.

### P0 blockers

| Blocker | Current-main evidence | Required outcome |
| --- | --- | --- |
| Broad image permission is not review-ready | The manifest requests `READ_MEDIA_IMAGES`; Play permits this only when persistent/frequent access is indispensable to promoted core functionality and Photo Picker is insufficient. | Submit the declaration and review video below. Keep recurring screenshot-library analysis as the main listing and onboarding purpose. Prepare a picker/share-only release variant if Play rejects the use case. |
| Prominent disclosure is incomplete | Onboarding says Shotlist scans existing screenshots and that contents stay on-device, but it does not present access scope, storage, and fallback together in one disclosure immediately before the system prompt. “Choose screenshots” is available later in You, not as the onboarding secondary action. | Ship the consolidated disclosure and three choices below, then record the exact production flow. Keep notifications out of this request. |
| Public and in-app privacy text is stale | The in-app policy omits cycle/period and habit data. `PRIVACY_POLICY_DRAFT.md` still says deletion and backup are future work and does not cover Scan, QR, Docs, vault, or health data. No public URL exists. | Reconcile both policies with the exact AAB, add health-data handling and Play-services document scanning, replace all publication placeholders, publish a stable public non-PDF page, and link the same policy in Play Console and the app. |
| Health features change the policy classification | Track stores menstrual-flow days and estimates cycle timing. Metabolic Lens reads glucose records through Health Connect. Current main therefore needs both feature and data-type declarations plus a complete disclaimer. | Declare **Period tracking** and Metabolic Lens's **Nutrition and weight management** use, justify read-glucose access, explain that both are local and informational, and add the required non-medical-device and professional-advice language to the store description. Verify whether any intended audience or regional obligations require more. |
| ML Kit and Play services Data Safety need release proof | Bundled text recognition and barcode scanning use ML Kit. Docs uses `play-services-mlkit-document-scanner`, whose models, logic, and UI are dynamically delivered and run through Google Play services. Removing `INTERNET` from Shotlist's process does not prove that a Google process transfers no operational data. | Inspect the exact merged release manifest and SDK Index, test release traffic/IPC on a GMS device, and reconcile findings with Google's current ML Kit disclosure. Use the conservative Data Safety answer until verified. |
| Notification permission is requested too broadly | `requestPrimaryAction` asks for `POST_NOTIFICATIONS` before the first action of any kind, including code, place, link, contact, product, recipe, and noise actions. | Ask only when accepting an event/deadline that schedules a reminder or when the user explicitly enables reminders, with nearby benefit copy. |

### P1 submission gates

- Produce and test a signed, minified Android App Bundle. CI currently builds and
  distributes only a debug APK. Protect the upload key and enroll in Play App Signing.
- Confirm `app.shotlist` is the permanent package name before the first upload.
- Keep `BILLING_LIVE=false` while billing is absent so release users retain full access.
  Before ever flipping it, connect compliant Play Billing and test pricing, purchase,
  restore, cancellation, offline startup, and entitlement recovery; the `true` branch
  deliberately starts release users in Free until billing proves Pro.
- Versioning is now automated: CI uses `GITHUB_RUN_NUMBER` for `versionCode` and
  `0.1.<run>` for `versionName`, with local fallback code `1`. Confirm the intended
  public version name and that every uploaded AAB's code remains monotonically higher.
- The adaptive launcher icon is no longer a blank placeholder. Still produce and review
  the separate 512×512 store icon and 1024×500 feature graphic.
- Run merged-manifest, dependency, permission, and SDK Index checks on the exact AAB.
- Move `play-services-mlkit-document-scanner:16.0.0-beta1` to an approved release
  dependency or document why the beta is acceptable; test first-run module download,
  no-GMS behavior, and the documented low-RAM unsupported path.
- Test Pixel, Samsung, and a non-stock/budget device across full, partial, denied,
  revoked, picker-only, and share-only image access.
- Test camera denied/revoked, unavailable camera, QR types, biometric unavailable,
  Android 11+ trash confirmation, and Docs unsupported/download-failure states.
- Verify actual observer behavior under force-stop, process death, OEM battery limits,
  reboot, and 15-minute WorkManager scheduling. Market eventual catch-up, not instant
  background detection.
- Verify TalkBack, font scaling, color contrast, keyboard/focus order, rotation, and
  process restoration for every store-promoted path.
- Use fabricated review media; never expose a real Wi-Fi password, ticket, address,
  period log, medical detail, calendar event, or access code.

## Resolved on current main

- `compileSdk` and `targetSdk` are API 36; current CI unit tests and debug APK pass.
- Manifest merge removes transitive `INTERNET` and `ACCESS_NETWORK_STATE`, Android
  backup is disabled, and app-private imported images are deleted after OCR.
- Unused direct calendar, foreground-service, and boot permissions are absent.
  Calendar creation remains on Android's user-confirmed `ACTION_INSERT` flow.
- Camera permission is present because the shipping Scan tab uses CameraX. Hardware is
  declared optional so non-camera devices are not filtered from Play automatically.
- Android 14 selected-photo access, Photo Picker import, and image share-sheet ingest
  provide minimum-scope paths. Multi-select copies are serialized and collision-safe.
- An in-app privacy policy, `Delete all my data`, biometric vault lock, and
  screenshot-specific upload wording ship.
- Billing-off release builds default to full access, avoiding a dead-end Pro gate;
  debug builds can still preview both entitlement tiers.
- `WAKE_LOCK` is retained for WorkManager's bounded OCR jobs; confirm its final merged
  purpose before submission.

## Photo permission declaration

### Console selection

- Permission: `READ_MEDIA_IMAGES` only. Do not request video access.
- Access frequency: core use / broad access.
- Supported use case: choose **Other** if the available choices do not accurately
  describe screenshot understanding. Do not call Shotlist a gallery or photo editor.
- Suggested label: `Recurring screenshot analysis and action extraction`.

### Core functionality — paste-ready draft

> Shotlist's primary purpose is to scan the user's screenshot collection and turn
> information in those images into reviewable actions, such as calendar events,
> deadlines, codes, links, and places. On first run it can scan existing screenshots;
> after that it observes new screenshots while active and uses scheduled catch-up work.
> This repeated access to the Screenshots collection is the app's central user-facing
> function, not an optional attachment feature.

### Why Photo Picker is insufficient — paste-ready draft

> Android Photo Picker returns only images the user manually selects during a picker
> session. It cannot query the Screenshots collection by media metadata, perform the
> promoted bounded history scan without repeated selection, or observe newly created
> screenshots for the recurring screenshot-to-action loop. Users who decline broad
> access can still import selected images with Photo Picker or share individual images
> to Shotlist.

### Data minimization — paste-ready draft

> Shotlist queries image metadata and opens image content only after filtering for
> likely screenshots using screenshot folder and filename signals. The first scan is
> capped, and later queries are limited to recent images. OCR and classification run on
> the device. Screenshot images and extracted text are not sent to Shotlist servers,
> used for advertising, or sold. Results remain in private app storage until the user
> deletes them or clears Shotlist data.

This intentionally says **Shotlist servers**, not **zero network traffic**, because
Google documents ML Kit operational data, and Docs is powered by Google Play services.

### Required prominent disclosure

Place this in one visible block immediately before the broad image permission request.
Do not combine it with camera, notification, health, or marketing consent.

> Shotlist accesses images in your Screenshots collection to scan existing and new
> screenshots for dates, deadlines, codes, links, and places. Screenshot contents and
> extracted text are processed and stored on this device; they are not sent to Shotlist
> servers. You can continue without full access by choosing or sharing individual
> screenshots.

Primary button: `Allow screenshot scanning`

Secondary button: `Choose screenshots instead`

Tertiary action: `Use share sheet only`

Current code instead uses `Scan my screenshots` and `Use share sheet only`; do not
record the review video until the production labels and picker route match this section.

### Review instructions — paste-ready draft

> No account or developer credentials are required. A device screen lock is needed only
> to test the private vault. Install on a device containing at least five fabricated
> screenshots with clear English text: two future dates/times, one address, one URL, and
> one short code. Launch Shotlist, choose “Allow screenshot scanning,” and grant full
> image access. The bounded onboarding scan reports screenshots read and useful finds.
> In Inbox, tap an event card to open its detail sheet, then tap the explicit calendar
> action; Android's insert screen opens and saves nothing until the reviewer confirms.
> Cancel, return, and copy the fabricated code from its detail sheet. Open Recall and
> search for a phrase from the fixtures. To test minimum scope, clear app data, relaunch,
> choose the picker or share-only path, then import a fabricated screenshot from You →
> Import screenshots.

Replace the labels with the exact release UI after the disclosure blocker is fixed.

### Required declaration video

Create one unlisted 45–75 second YouTube video with no cuts hiding permission state:

1. Show the exact version/package and a gallery containing only fabricated fixtures.
2. Launch from a clean install and show the complete in-app disclosure.
3. Grant full image access in the Android prompt.
4. Show bounded backfill and resulting Inbox cards.
5. Open event detail, open the calendar insert UI, and cancel without saving.
6. Return and copy a fabricated code.
7. Take a new text screenshot, return to Shotlist, and show observer/catch-up behavior
   without claiming it was found while force-stopped.
8. Show the Photo Picker or share-sheet fallback.

Put package name, version code/name, device/OS, and exact test steps in the video
description. Keep this separate from any public marketing video.

### Likely reviewer objection and response

**Objection:** Users can share or pick images, so broad access is only convenience.

**Response:** The listing and first-run experience make recurring screenshot-library
analysis the product itself. Picker/share support is a degraded manual path; it cannot
perform the promoted bounded history scan or recurring screenshot query. Demonstrate
both. If Play rejects the declaration, ship the picker/share-only build without
`READ_MEDIA_IMAGES`; do not repeatedly submit unchanged wording.

## Health apps declaration

- Complete the declaration for every track, including testing tracks.
- Select **Period tracking**. Habits alone may be general productivity, but menstrual
  flow logging and cycle timing estimates unambiguously match Play's category.
- Select **Nutrition and weight management** for Metabolic Lens's personal reflection
  around glucose history and user-added meal/movement/sleep/note markers. Do not select
  disease or condition management unless the release actually adopts a medical use.
- Declare and justify exactly `android.permission.health.READ_BLOOD_GLUCOSE`. The
  release requests no glucose write, background-health, or extended-history access.
- Use the paste-ready permission justification and external-product wording in
  `METABOLIC_LENS_PLAY_DECLARATIONS.md`.
- Describe Track as local logging and an estimate based on prior entries. Do not claim
  fertility, diagnosis, treatment, prevention, clinical accuracy, or guaranteed timing.
- Add this store-description disclaimer unless qualified counsel requires stronger text:

> Shotlist's cycle estimates are for general informational use. Shotlist is not a
> medical device and does not diagnose, treat, cure, or prevent any medical condition.
> Metabolic Lens presents previously recorded information for general reflection only.
> Consult a qualified healthcare professional for medical advice, diagnosis, or
> treatment.

- Add menstrual-flow entries, derived cycle timing, habit names/ticks, glucose records,
  context markers, storage, retention, deletion, and any user-initiated sharing to both
  privacy policies.
- Confirm the intended age rating and regional availability after the health feature
  set is frozen.

## Store listing

The draft below reflects current main. Remove any feature cut from the release AAB. Do
not mention Pro until a real purchase/restore path exists; the current billing-off
release exposes full Recall and vault access.

### Metadata

- App name: `Shotlist`
- Primary category: `Productivity`
- Ads: `No`
- Target audience: `13 and older`; `Not designed for children`
- App access: no account or developer credential; tell reviewers that vault testing
  uses the device's own screen lock.
- Content rating: answer against local user-provided screenshot and period data; do not
  misclassify local screenshots as public user-generated content.

### Short description

> Turn screenshots and scans into useful actions—privately, on your device.

### Full description

> Your screenshots already contain plans, deadlines, addresses, tickets, links, and
> codes. Shotlist turns that information into something you can use.
>
> FIND IT AGAIN
>
> • Scan a bounded screenshot history into a focused Inbox
>
> • Search recognized screenshot text with Recall
>
> • Review details before opening calendar, maps, links, contacts, or copy actions
>
> • Review stale screenshots before sending them to Android's trash
>
> SCAN WHAT IS IN FRONT OF YOU
>
> • Capture printed text with the Anything camera
>
> • Read QR codes for links, Wi-Fi, contacts, and events
>
> • Scan paper into a cleaned, shareable PDF on supported Google Play services devices
>
> TRACK YOUR RHYTHM
>
> • Log menstrual flow days and view locally calculated cycle estimates
>
> • Build simple habits and streaks
>
> • Keep cycle and habit entries in Shotlist's private on-device database
>
> SEE YOUR METABOLIC STORY
>
> • View glucose history you authorize through Android Health Connect
>
> • Add meal, movement, sleep, and note markers for personal reflection
>
> • Choose one source at a time and keep health information in private on-device storage
>
> PRIVATE BY DESIGN
>
> Screenshot OCR and classification happen on your device. Screenshot contents and
> extracted text are not sent to Shotlist servers. There is no Shotlist account, no ad
> profile, and no sale of screenshot data. Sensitive codes can be kept behind your
> device screen lock, and Delete all my data clears Shotlist's local database and files.
>
> WHY PHOTO ACCESS?
>
> Full image access powers the initial screenshot history scan and recurring catch-up.
> If you decline, you can still import selected images or share individual screenshots.
>
> Shotlist never saves a calendar event or trashes a screenshot without your explicit
> Android confirmation. Permissions can be revoked at any time in Android Settings.
>
> Metabolic Lens requires a compatible app or sensor service that writes glucose data
> to Android Health Connect. Abbott Lingo users need a compatible Lingo biosensor and
> the Lingo app with Health Connect sync enabled. Shotlist does not connect directly to
> the sensor. Lingo's Health Connect values are delayed, so Metabolic Lens is not a live
> glucose display.
>
> Shotlist's cycle estimates and Metabolic Lens are for general informational and
> reflection use. Shotlist is not a medical device and does not diagnose, treat, cure,
> or prevent any medical condition. Consult a qualified healthcare professional for
> medical advice, diagnosis, or treatment.

### First release notes

> Meet Shotlist: turn screenshots and camera scans into useful actions, search what you
> forgot, keep sensitive finds private, and track simple rhythms—all without a Shotlist
> account.

## Data Safety form

Play says solely on-device access is not collection. Shotlist's local screenshot text,
documents, codes, cycle entries, and habits therefore should not be declared as
collected **only if release verification confirms they never leave the device**.
Google's current ML Kit disclosure separately describes operational SDK data.

### Conservative answer before release verification

| Console question | Conservative answer | Basis |
| --- | --- | --- |
| Does the app collect or share required user-data types? | `Yes — collects` | Google's ML Kit disclosure lists app/device information, performance metrics, API configuration, event/error data, and identifiers. |
| Is user data shared with third parties? | `No`, if the exact SDK Index treatment agrees | Google describes this operational data as not transferred to third parties. User-initiated calendar/maps/share actions may qualify for Play's expected-action exceptions. |
| Is data encrypted in transit? | `Yes` for declared ML Kit operational data | Google documents HTTPS. |
| Can users request deletion? | Do not claim deletion of ML Kit telemetry without a verified mechanism. Separately state that `Delete all my data` erases Shotlist's local database/files. | The in-app control now exists, but it cannot be assumed to erase operational data already handled by Google. |
| Does the app follow the Families policy? | `No / not applicable` | Proposed audience is 13+ and not designed for children. |

Declare these operational types conservatively, then reconcile them against the exact
dependency set and current Play form:

| Data type | Collected | Shared | Required | Purpose |
| --- | --- | --- | --- | --- |
| Device or other identifiers | Yes | No | Yes while affected ML Kit SDKs are present | Analytics; diagnostics/app functionality as offered by the form |
| App info and performance / Diagnostics | Yes | No | Yes | Diagnostics |
| App info and performance / Other performance data | Yes if listed by the SDK Index/form | No | Yes | Analytics and diagnostics |

Do not mark photos/videos, OCR text, document contents, calendar data, locations, codes,
menstrual-flow entries, cycle estimates, habits, glucose records, or Metabolic Lens
moments as collected if the exact release keeps them solely on-device. Health is also
excluded from the standard export. The biometric-gated, twice-confirmed health export
is a user-confirmed transfer to another app; handle it according to Play's expected-action
exceptions and describe it in the privacy policy.

### No-collection answer after verification

Only answer `Data collected: No` and `Data shared: No` if all are true:

- the exact AAB's merged manifest contains neither `INTERNET` nor
  `ACCESS_NETWORK_STATE`;
- release traffic and IPC inspection, including Docs through Google Play services,
  finds no relevant operational transfer attributable to the app/SDK use;
- SDK Index and Play pre-review checks do not require conflicting disclosure;
- backup remains disabled/excluded; and
- no other dependency or platform integration transmits user data.

The Data Safety form and public privacy-policy URL remain required either way.

### Other App content answers

- Privacy policy: required in Play Console and inside the app; current public URL is
  missing.
- Ads: no.
- App access: no login; provide the device-lock note for vault review.
- Data deletion: no account-deletion section unless account creation ships; document
  local deletion and a support/privacy contact.
- Health apps: **Yes — Period tracking; Nutrition and weight management**. Declare
  read-only Health Connect glucose access with the exact justification in
  `METABOLIC_LENS_PLAY_DECLARATIONS.md`.
- Medical device: no intended medical-device function; include the disclaimer above.
- Government, financial, news/magazine, and COVID-19/contact tracing: no.

## Screenshot and preview plan

Use only fabricated fixtures and capture the exact release build. A focused six-image
set is preferable to advertising every secondary feature:

| Order | Headline | Actual screen/evidence |
| --- | --- | --- |
| 1 | `Your screenshots become useful actions` | Inbox with one fabricated event, deadline, and code |
| 2 | `Find what you forgot` | Recall results for a fabricated phrase, with no real sensitive text |
| 3 | `Scan text, QR, or paper` | Scan mode selector plus a fabricated successful result |
| 4 | `Review before anything happens` | Finding detail immediately before a user-confirmed action |
| 5 | `Private rhythms, kept on your phone` | Track with fabricated flow/habit data and no medical claim |
| 6 | `Full scan or selected import—your choice` | Final production permission disclosure/import fallback |

Asset checklist:

- Store icon: 512×512 32-bit PNG with alpha, at most 1 MB.
- Feature graphic: 1024×500 JPEG or 24-bit PNG without alpha.
- Phone screenshots: at least two; use six consistent high-resolution portrait assets.
- Permission review video: separate from optional public marketing video.
- Keep real UI prominent; avoid tiny text, fake notifications, prices, awards,
  rankings, roadmap features, or anything not in the AAB.
- Use one fabricated fixture set across screenshots and video so review is reproducible.

## Submission sequence

1. Freeze the release feature set and keep billing-off full access unless a complete,
   tested Play Billing implementation replaces it.
2. Fix the prominent photo disclosure and notification request context.
3. Reconcile and publish the privacy policy, including cycle, glucose, Metabolic Lens
   markers, the 30-day recovery limitation, gated export, and Play services data.
4. Complete the Period tracking and Metabolic Lens Health apps declarations, submit the
   read-glucose justification, and add the non-medical/professional-advice disclaimer.
5. Build the signed API-36 release AAB and inspect its merged manifest, dependencies,
   SDK Index, and Play pre-review output.
6. Run release-mode traffic/IPC, storage/deletion, backup, and device-matrix tests.
7. Capture final store assets and permission video from that exact candidate.
8. Upload to Internal testing and complete Data Safety, App content, content rating,
   app access, and photo permission forms.
9. If broad photo access is rejected, switch to the prepared picker/share-only build.
10. Use staged production rollout and monitor Android vitals and policy feedback.

## Official policy and SDK sources

Verified on 2026-09-01:

- [Permissions and APIs that access sensitive information](https://support.google.com/googleplay/android-developer/answer/16558241)
- [Photo and Video Permissions policy details](https://support.google.com/googleplay/android-developer/answer/14115180)
- [User Data policy and prominent disclosure](https://support.google.com/googleplay/android-developer/answer/10144311)
- [Data Safety form guidance](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Health Content and Services](https://support.google.com/googleplay/android-developer/answer/16679511)
- [Health apps declaration form](https://support.google.com/googleplay/android-developer/answer/14738291)
- [Health app categories](https://support.google.com/googleplay/android-developer/answer/13996367)
- [ML Kit data disclosure](https://developers.google.com/ml-kit/android-data-disclosure)
- [ML Kit document scanner](https://developers.google.com/ml-kit/vision/doc-scanner/android)
- [Google Play target API requirements](https://developer.android.com/google/play/requirements/target-sdk)
- [Google Play preview-asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151)

Policy, SDK disclosures, and Play Console wording change independently of the code.
Recheck every answer against the exact release date and AAB; this is a preparation
record, not a guarantee of approval or legal advice.
