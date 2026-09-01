# Shotlist Google Play readiness

Last policy check: 2026-08-31

This is the submission source of truth for the current Android v1. It is deliberately
limited to features that exist in the build. Future Scan, Track, vault, search, price
watch, subscription, and cloud features must not appear in the listing until they ship.

## Release verdict

**Not ready for Play submission yet.** The product has a credible broad-photo-access
case, but the bundle, disclosure, fallback, and privacy claims need work before review.

### P0 blockers

| Blocker | Why it matters | Required outcome |
| --- | --- | --- |
| `compileSdk` / `targetSdk = 35` | Starting 2026-08-31, new mobile apps and updates submitted to Play must target Android 16 / API 36 unless an extension applies. | Compile and target API 36, then regression-test photo access, sharing, notifications, calendar intents, and edge-to-edge UI on Android 16. |
| Broad image permission is not yet review-ready | `READ_MEDIA_IMAGES` requires a declaration proving persistent or frequent access is indispensable to the app's promoted core function. Approval is not automatic. | Use the declaration and review video below. Keep screenshot scanning as the first and dominant listing promise. |
| Permission fallback is too hidden | A user who declines broad access can receive externally shared images, but there is no visible in-app import control. Play requires a reasonable minimum-scope alternative. | Add an `Import screenshots` action backed by Android Photo Picker and retain the share-sheet path. The automatic loop may remain the full-access benefit. |
| Disclosure is incomplete | The pre-permission screen says Shotlist scans screenshots, but it does not plainly state the scope, storage, and fallback in one disclosure immediately before the system prompt. | Use the disclosure copy below, followed by a distinct affirmative button. Ask for notifications later, in context, rather than in the image-permission batch. |
| Privacy policy is not reachable in-app | Apps accessing sensitive data need a public privacy-policy URL in Play Console and an in-app link. | Publish the companion privacy-policy draft as a non-PDF public page and link it from onboarding and the You tab. Replace every placeholder. |
| `0 bytes uploaded` needs release-level proof | Commit `ab9b08a` strips `INTERNET` and `ACCESS_NETWORK_STATE` during manifest merge, preventing ordinary in-process network access. However, ML Kit's current SDK disclosure says bundled features collect app/device information, performance metrics, and a per-installation identifier; manifest removal alone does not prove the SDK never hands telemetry to another Google process. | Inspect the merged manifest, run release traffic/IPC tests on a Google Play services device, and reconcile the result with Play's SDK Index. Until then, use `Screenshot contents stay on this device` and the conservative Data Safety answer below. |
| Notification permission is premature | `POST_NOTIFICATIONS` is requested during onboarding, but the registered reminder receiver is a no-op and no shipping loop posts notifications. Sensitive permissions must support a current, promoted feature and should be requested in context. | Remove it for this release, or finish and test the reminder loop and request notification access only when the user enables a reminder. |
| Android 14 partial access path is inconsistent | Code requests `READ_MEDIA_VISUAL_USER_SELECTED`, but the app manifest does not declare it, and there is no picker-based reselection UI. | Implement and test selected-photo access correctly or remove the partial-mode claim and rely on Photo Picker/share sheet. |
| Android 8–9 screenshot queries need an API guard | The app supports API 26, but its MediaStore projection unconditionally includes `RELATIVE_PATH`, which was added in API 29. The first scan can fail on supported older devices. | On API 26–28 omit `RELATIVE_PATH` and filter by display-name hints; add tests for both projection paths. |
| Local data has no in-app deletion/retention control | OCR text, findings, screenshot URIs, codes, and copied share-sheet images persist in app storage. Shared image copies are not deleted after OCR. | Add `Delete all Shotlist data`; define retention; delete temporary shared images after processing when no longer needed; test uninstall/clear-data behavior. |
| Android backup conflicts with the privacy posture | `android:allowBackup="true"` may permit platform-managed backup of the Room database and preferences, including OCR-derived data. | Disable backup or add explicit backup/data-extraction rules excluding screenshot-derived data. State the final behavior in the privacy policy. |

### P1 submission gates

- Produce a signed Android App Bundle, not only an APK; protect the upload key and
  enroll in Play App Signing.
- Use a non-placeholder launcher icon and unique store icon.
- Confirm `app.shotlist` is the permanent package name before the first upload.
- Replace version `0.1.0` / code `1` with the intended first-store version.
- Run the merged-manifest and SDK Index checks on the exact release AAB.
- Test on Pixel, Samsung, and one non-stock/budget device with full, partial, denied,
  revoked, and share-only access states.
- Verify the observer's real promise. It is process-scoped today, so do not claim that
  closed-app screenshots are always detected unless a compliant implementation ships.
- Add a clear empty state and recovery path when no screenshots contain readable text.
- Use fabricated review media; never put a real Wi-Fi password, ticket, address, or
  calendar event in screenshots or the permissions video.

### Resolved by manifest hardening (`ab9b08a`)

- Removed unused camera, read/write calendar, foreground-service, and boot permissions.
- Kept calendar creation on Android's user-confirmed `ACTION_INSERT` flow.
- Added merge-level removal of transitive `INTERNET` and `ACCESS_NETWORK_STATE`.
- Kept `WAKE_LOCK` for WorkManager's bounded OCR jobs; confirm its presence and purpose
  in the final merged manifest.

## Photo permission declaration

### Console selection

- Permission: `READ_MEDIA_IMAGES` only. Do not request video access.
- Access frequency: core use / broad access.
- Supported use case: choose **Other** if the available choices do not accurately
  describe screenshot understanding. Do not call Shotlist a gallery or photo editor.
- Suggested label: `Recurring screenshot analysis and action extraction`.

### Core functionality — paste-ready draft

> Shotlist's primary purpose is to scan the user's screenshot collection and turn
> information in those images into reviewable action cards, such as calendar events,
> deadlines, and codes. On first run it can scan existing screenshots; while
> active it detects newly created screenshots and processes them. This repeated access
> to the Screenshots collection is the app's central user-facing function, not an
> optional attachment feature.

### Why Photo Picker is insufficient — paste-ready draft

> Android Photo Picker only returns images the user manually selects during a picker
> session. It cannot query the Screenshots collection by media metadata, backfill an
> existing screenshot history without repeated selection, or observe newly created
> screenshots for the app's recurring screenshot-to-action loop. Shotlist therefore
> needs image-library access for its promoted automatic and bulk screenshot workflow.
> Users who decline broad access can still import selected images with Photo Picker or
> share individual screenshots to Shotlist.

Only include the Photo Picker sentence after that in-app control ships.

### Data minimization — paste-ready draft

> Shotlist queries image metadata and opens image content only after filtering for
> likely screenshots using screenshot folder and filename signals. The first-run scan
> is capped, and later queries are limited to recent images. OCR and classification of
> screenshot contents run on the device. Screenshot images and extracted text are not
> sent to Shotlist servers, are not used for advertising, and are not sold. Results are
> stored in private app storage until the user deletes them or clears Shotlist data.

This wording intentionally says **Shotlist servers**, not **zero network traffic**,
because the bundled ML Kit SDK documents operational telemetry.

### User-facing prominent disclosure

Place this in its own visible block immediately before the broad image permission
request. Do not combine it with notification consent.

> Shotlist accesses images in your Screenshots collection to scan existing and new
> screenshots for dates, deadlines, and codes. Screenshot contents and
> extracted text are processed and stored on this device; they are not sent to
> Shotlist servers. You can continue without full access by importing or sharing
> individual screenshots.

Primary button: `Allow screenshot scanning`

Secondary button: `Choose screenshots instead`

Tertiary action: `Use share sheet only`

### Review instructions — paste-ready draft

> No account or special credentials are required. Install on a device containing at
> least five test screenshots with clearly printed English text, including two future
> dates/times, one event address, and one short code. Launch Shotlist, tap “Allow screenshot
> scanning,” and choose full photo access in the Android dialog. The onboarding scan
> shows how many screenshots were read and then opens an Inbox of action cards. Tap an
> event card to open Android's calendar insert screen; no event is saved until the
> reviewer confirms in the calendar app; an address in that screenshot should prefill
> the event location. Tap a code card to copy it. To test the
> minimum-scope fallback, clear app data, relaunch, choose “Choose screenshots instead”
> or share an image to Shotlist from Android's share menu.

Update the exact labels after the fallback UI ships.

### Required declaration video

Make one unlisted 45–75 second YouTube video with no cuts hiding permission state:

1. Show Shotlist's Play-ready version number and a device gallery containing only
   fabricated screenshot fixtures.
2. Launch from a clean install and show the complete in-app disclosure.
3. Tap the affirmative button and grant full image access in the Android prompt.
4. Show the bounded backfill scan and resulting Inbox action cards.
5. Open an event card into the system calendar insert UI, then cancel without saving.
6. Return to Shotlist and copy a fabricated code.
7. Take a new text screenshot while Shotlist is active, return, and show it being found.
8. Show the minimum-scope Photo Picker or share-sheet fallback.

Put the package name, version code, device/OS, and exact test steps in the video
description. The permission declaration requires a review video even if no public
store preview video is used.

### Likely reviewer objection and response

**Objection:** Users can share or pick images, so broad access is only convenience.

**Response:** The app listing and first-run experience make recurring screenshot-library
analysis the product itself. A picker supports a degraded manual import path, but cannot
perform the advertised bounded history scan or notice new screenshots. Demonstrate both
in the video. If Play still rejects the declaration, ship the picker/share build without
`READ_MEDIA_IMAGES`; do not repeatedly resubmit unchanged wording.

## Store listing

### Metadata

- App name: `Shotlist`
- Category: `Productivity`
- Tags: choose only current utility/productivity tags exposed by Play Console.
- Ads: `No` for the current build.
- Target audience: `13 and older`; select `Not designed for children`.
- App access: `All functionality is available without special access`.
- Content rating: complete the questionnaire against local user-provided screenshot
  content; do not treat local screenshots as public user-generated content.

### Short description

> Turn screenshot dates and codes into useful, reviewable action cards.

### Full description

> Your screenshots already contain plans, deadlines, addresses, tickets, and codes.
> Shotlist turns that information into something you can use.
>
> Shotlist's main job is to scan screenshots on your device, read their text, and make
> reviewable action cards. Scan your existing screenshot history, then deal with useful
> finds from one focused Inbox.
>
> WHAT SHOTLIST DOES
>
> • Finds dates, times, deadlines, event addresses, and short codes in screenshots  
> • Opens a prefilled calendar event only after you tap an event card  
> • Copies codes when you need them  
> • Lets you dismiss or snooze findings that are not useful  
> • Accepts individual images from Android's share menu  
> • Offers a selected-image import path when you prefer not to grant full access
>
> PRIVATE BY DESIGN
>
> Screenshot OCR and classification happen on your device. Screenshot contents and
> extracted text are not sent to Shotlist servers. There is no Shotlist account, no ad
> profile, and no sale of screenshot data.
>
> WHY PHOTO ACCESS?
>
> Shotlist uses photo access for its core function: finding and repeatedly processing
> images in your Screenshots collection. Full access enables the initial history scan
> and detection of new screenshots while Shotlist is active. If you decline, you can
> still import selected images or share individual screenshots to the app.
>
> You stay in control. Shotlist never saves a calendar event without your confirmation,
> and you can revoke photo access at any time in Android Settings.

Do not publish the Photo Picker claims until the control exists. Until reliable
closed-app ingest ships, retain the qualifier `while Shotlist is active`.

### First release notes

> Meet Shotlist: scan screenshots for dates, deadlines, event details, and codes; review useful
> finds in one Inbox; and turn them into actions without uploading screenshot contents.

## Data Safety form

The product is local-first and its manifest now strips ordinary network permissions.
That is strong evidence, but Play's current ML Kit disclosure still says the SDK collects
limited telemetry even for bundled features. Choose the form answer from measured release
behavior, not from either claim in isolation.

### Conservative answer before release verification

| Console question | Answer | Basis |
| --- | --- | --- |
| Does the app collect or share required user-data types? | `Yes — collects` | ML Kit documents SDK telemetry; this avoids under-declaring while actual release transport remains unverified. |
| Is user data shared with third parties? | `No` | ML Kit describes the data as not shared with third parties; treat Google as the processing SDK/service provider for this function. User-initiated calendar/maps transfers are excluded from “sharing” when the user reasonably expects them. |
| Is data encrypted in transit? | `Yes` | ML Kit documents HTTPS for its collected telemetry. |
| Can users request deletion? | Do not claim an in-app mechanism yet. | There is no account or developer server, but the app still needs a clear local `Delete all data` control and a public contact method before submission. |
| Does the app follow the Families policy? | `No / not applicable` | The proposed audience is 13+ and not designed for children. |

Declare these types conservatively, then reconcile them against the SDK Index entry and
the exact Play form shown for the release AAB:

| Data type | Collected | Shared | Required | Purpose |
| --- | --- | --- | --- | --- |
| Device or other identifiers | Yes | No | Yes while ML Kit is present | Analytics; diagnostics/app functionality as offered by the form |
| App info and performance / Diagnostics | Yes | No | Yes while ML Kit is present | Diagnostics |
| App info and performance / Other app performance data | Yes if listed by the current ML Kit SDK entry | No | Yes while ML Kit is present | Analytics and diagnostics |

Do **not** mark photos/videos, OCR text, calendar data, location, codes, or other
screenshot-derived content as collected if the final binary keeps those data solely on
device. On-device-only access is outside Play's definition of collection. User-confirmed
transfer to the Android calendar or maps app is a user-initiated action and is excluded
from the form's definition of sharing.

Before submitting, verify the ML Kit disclosure for the exact dependency version. SDK
behavior can change independently of Shotlist code.

### No-collection answer after verification

The merge-level removal of `INTERNET` and `ACCESS_NETWORK_STATE` is necessary evidence,
but not sufficient on its own. Only use the answers below if all of these are true:

- the exact release AAB's merged manifest contains neither permission;
- release-mode traffic and IPC inspection on a device with Google Play services finds
  no ML Kit telemetry leaving the app or being handed to another process;
- Play Console's SDK Index and pre-review checks do not indicate a conflicting required
  disclosure;
- backup of screenshot-derived data is disabled or excluded; and
- no other dependency or platform integration transmits user data.

Then the form may say:

- Data collected: `No`
- Data shared: `No`
- Screenshot access/processing: local-only and therefore not declared as collection

Even in that configuration, the Data Safety form and a privacy-policy URL are required.

### Other App content answers

- Privacy policy: required in Play Console and inside the app.
- Ads: no.
- App access: no login or restricted feature.
- Data deletion: no account deletion section is needed unless account creation ships;
  still provide local deletion and a support contact.
- Government apps: no.
- Financial features: no.
- Health apps: no for this release. Re-answer before any cycle, calorie, or health
  tracking module ships.
- News/magazine: no.
- COVID-19/contact tracing: no.

## Screenshot and preview plan

Use six 1080 × 1920 portrait images. Play requires at least two phone screenshots; four
high-resolution 9:16 screenshots are recommended for broader promotional eligibility.
Use JPEG or 24-bit PNG without alpha and keep every image an honest representation of
the shipping build.

| Order | Headline | Actual screen/evidence | Purpose |
| --- | --- | --- | --- |
| 1 | `Your screenshots become things that happen` | Inbox with one event, one deadline, and one code card | Explain the product in one glance. |
| 2 | `Find what you forgot` | Onboarding reveal with fabricated scan/useful counts | Show the history-rescue moment. |
| 3 | `Add the plan in one tap` | Event action card immediately before the calendar insert action | Show the concrete payoff. |
| 4 | `Codes and deadlines, ready when you need them` | Two real action-card states with fake data | Broaden utility without promising future modules. |
| 5 | `Screenshot contents stay on your phone` | Privacy/disclosure screen using the production wording | Build trust without the false zero-network claim. |
| 6 | `Full scan or selected import — your choice` | Production permission-choice screen after Photo Picker ships | Demonstrate consent and fallback. |

Asset checklist:

- Store icon: 512 × 512, 32-bit PNG with alpha, at most 1 MB.
- Feature graphic: 1024 × 500, JPEG or 24-bit PNG without alpha.
- Phone screenshots: at least two; use six at 1080 × 1920.
- Public preview video: optional. If used, make the first 15 seconds show screenshot →
  action with no logo animation or feature roadmap.
- Permission review video: required and separate from the marketing plan above.
- Keep actual UI prominent in the first three screenshots; avoid device frames, tiny
  text, fake notifications, rankings, prices, awards, and features not in the AAB.
- Use one consistent fabricated fixture set across screenshots and video so reviewers
  can reproduce it.

## Submission sequence

1. Resolve every P0 blocker and freeze the feature set.
2. Build the signed API-36 release AAB and inspect its merged manifest and SDK Index.
3. Run release-mode network capture and on-device storage/backup tests.
4. Publish the privacy policy and wire the same URL inside the app.
5. Capture final screenshots from the exact release candidate.
6. Record the permissions video from a clean install of that candidate.
7. Upload the AAB to Internal testing and complete all App content forms.
8. Submit the photo permission declaration and allow several weeks for extended review.
9. If rejected, use the prepared picker/share-only build without broad image permission
   rather than delaying all distribution.
10. After approval, use a staged production rollout and watch Android vitals.

## Policy sources

Official sources checked on 2026-08-31:

- [Permissions and APIs that access sensitive information](https://support.google.com/googleplay/android-developer/answer/16558241)
- [Restricted permissions and minimum-scope alternatives](https://support.google.com/googleplay/android-developer/answer/16935362)
- [Permissions declaration process](https://support.google.com/googleplay/android-developer/answer/9214102)
- [User Data policy and prominent disclosure](https://support.google.com/googleplay/android-developer/answer/10144311)
- [Data Safety form guidance](https://support.google.com/googleplay/android-developer/answer/10787469)
- [ML Kit data disclosure](https://developers.google.com/ml-kit/android-data-disclosure)
- [ML Kit terms and privacy](https://developers.google.com/ml-kit/terms)
- [Google Play target API requirements](https://developer.android.com/google/play/requirements/target-sdk)
- [Google Play preview-asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151)
- [Store listing best practices](https://support.google.com/googleplay/android-developer/answer/13393723)

Policy and SDK disclosures change. Recheck all links against the exact release date and
bundle; this document is preparation, not a guarantee of Play approval.
