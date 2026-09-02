# Shotlist Privacy Policy — publication draft

> **Do not publish with placeholders.** Replace the legal name, contact information,
> effective date, and public URL. Reconcile this text with the exact release AAB, then
> publish it as a stable, public, non-PDF web page and link it inside Shotlist.

Effective date: `[DATE]`

Shotlist is provided by `[DEVELOPER LEGAL NAME]` (`we`, `us`, or `our`). This policy
explains how the Shotlist Android app accesses and handles information.

## What Shotlist accesses

If you grant photo access, Shotlist queries images on your device to identify likely
screenshots. It uses screenshot folder and filename signals before opening an image for
processing. You may instead choose individual images with Android Photo Picker or share
individual images to Shotlist through Android's share menu.

Screenshots can contain personal or sensitive information. Depending on the screenshot,
Shotlist may recognize text such as event details, deadlines, locations, links, prices,
tracking numbers, or short codes.

## How screenshot information is used

Shotlist uses screenshot images and recognized text only to provide its user-facing
features: creating reviewable action cards, preparing user-confirmed calendar entries,
and copying codes at the user's request.

Screenshot optical-character recognition and classification run on the device.
Screenshot images, recognized text, and action-card contents are not sent to Shotlist
servers. We do not sell screenshot information or use it for advertising.

When you explicitly open a calendar or another app from Shotlist, Android passes the
information needed for that action to the app you chose. That app handles the information
under its own privacy policy.

## Information stored on the device

Shotlist stores screenshot references, recognized text, extracted findings, action
states, and onboarding preferences in private app storage. Images shared directly to
Shotlist may be copied into private app storage so processing can finish reliably.

Shotlist also stores menstrual-flow entries, locally calculated cycle estimates, habit
names and completion ticks, and any items the user puts in the private vault.

Android backup is disabled for Shotlist. Local information remains until the user
deletes it through Shotlist's data controls, clears Shotlist storage in Android, or
uninstalls the app.

## Health Connect and Metabolic Lens

Metabolic Lens can read glucose records that you explicitly authorize through Android
Health Connect. Shotlist requests read-only glucose access. It does not request glucose
write access, background health access, or extended health-history access. Shotlist does
not connect directly to a glucose sensor over Bluetooth and does not send glucose data
to Shotlist or Abbott servers.

Metabolic Lens stores an on-device copy of the selected Health Connect source's record
identifier, source-app package, observation time and optional time-zone offset, value in
mmol/L, specimen source, and import time. It also stores meal, movement, sleep, and note
markers that you add. This information is used only to display your 24-hour, 7-day, or
30-day history, descriptive observed statistics, data gaps, and your own markers. It is
not used for advertising, diagnosis, treatment, dosing, threshold alerts, or automated
medical decisions.

The initial import and recovery snapshot cover up to the latest 30 days. While its
Health Connect change history remains valid, Shotlist also applies additions, updates,
and deletions reported by Health Connect. If that change history expires, Shotlist can
recheck only the current 30-day window because it does not request extended-history
access. An older local copy may therefore remain after its source record was deleted
from Health Connect. You can remove it immediately with Metabolic Lens → Disconnect →
Delete local history or `Delete all my data`.

Both Metabolic Lens disconnect choices attempt to revoke Shotlist's Health Connect
access. Choosing to keep history retains the imported local copy. Choosing to delete
history erases local glucose samples, markers, and synchronization state even if Android
cannot complete permission revocation. Clearing app storage or uninstalling also erases
the local copy and revokes the app's Health Connect permissions.

The standard Shotlist data export excludes all health data. After unlocking with your
device credential or biometric, you may deliberately select an initially unchecked
`Include health data` option and confirm a second warning. That one export adds separate
glucose-sample and moment JSON files in canonical mmol/L to the ZIP. Android then sends
the ZIP only to the destination you choose; that destination handles it under its own
privacy and security terms.

## Limited operational information from ML Kit

Shotlist uses Google's ML Kit Text Recognition SDK. Google states that image inputs and
recognized text are processed on-device and are not sent to Google servers. Google also
states that ML Kit Android SDKs may collect limited device and application information,
performance metrics, and a per-installation identifier for diagnostics and usage
analytics. Google states that this operational data is encrypted in transit and is not
shared with third parties.

See Google's [ML Kit data disclosure](https://developers.google.com/ml-kit/android-data-disclosure)
and [ML Kit terms and privacy](https://developers.google.com/ml-kit/terms).

Shotlist does not include advertising SDKs and does not create a Shotlist account in the
current release.

## Permissions and choices

- **Photos and images:** enables the core history scan and detection of likely
  screenshots. You can decline or revoke this access and use selected import or the
  Android share menu instead.
- **Notifications:** optional alerts related to useful findings or reminders. You can
  disable them in Android Settings.
- **Health Connect glucose:** optional read-only access for Metabolic Lens. You can
  pause synchronization, manage access in Health Connect, disconnect and keep local
  history, or disconnect and delete local history.
- **Calendar and clipboard actions:** occur only after your tap. The
  production release should not request direct read or write calendar permission when
  the Android calendar insert interface is sufficient.

You can change app permissions at any time in Android Settings.

## Data sharing

We do not sell personal information. We do not share screenshot contents, recognized
text, cycle entries, habits, glucose records, or Metabolic Lens markers with advertisers,
data brokers, or our own servers. Limited ML Kit operational data is processed by Google
as described above. User-initiated transfers to another Android app occur only when you
choose the corresponding action, including when you explicitly include health data in
an export.

## Security

Shotlist limits local information to private app storage, disables Android backup, and
uses Android permission controls. The private vault and health-export flow use your
device credential or biometric, and Metabolic Lens requests secure-window handling to
block ordinary screenshots and Recents previews. No storage or software system can be
guaranteed completely secure. Keep your device and lock screen secure, especially if
screenshots or exports contain access codes or health information.

## Retention and deletion

The app provides a `Delete all my data` control. It clears Shotlist's local database,
private files, export cache, and health data, and it attempts to revoke Health Connect
access. Local deletion proceeds even if permission revocation fails. You can also delete
local Shotlist data through Android Settings or by uninstalling the app. Because the
current release has no Shotlist account or Shotlist server storage, there is no
server-side Shotlist history to delete.

For questions or a privacy request, contact `[PRIVACY EMAIL]`. Describe the request and
the app version, but do not email screenshots, passwords, or access codes.

## Children

Shotlist is not directed to children under 13. We do not knowingly collect personal
information from children through a Shotlist account or Shotlist server because the
current release provides neither.

## Changes to this policy

We may update this policy when Shotlist's features, SDKs, or legal obligations change.
We will update the effective date and provide any additional notice required by law.

## Contact

`[DEVELOPER LEGAL NAME]`  
`[POSTAL ADDRESS, IF REQUIRED]`  
`[PRIVACY EMAIL]`  
`[PUBLIC PRIVACY POLICY URL]`
