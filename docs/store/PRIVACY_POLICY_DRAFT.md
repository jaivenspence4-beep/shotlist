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

This information remains until it is deleted through Shotlist's data controls, Android's
clear-storage control, or app uninstall, subject to Android's device-backup behavior.
Before publication, `[DEVELOPER LEGAL NAME]` will either disable backup for
screenshot-derived data or describe the final configured backup behavior here.

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
- **Calendar and clipboard actions:** occur only after your tap. The
  production release should not request direct read or write calendar permission when
  the Android calendar insert interface is sufficient.

You can change app permissions at any time in Android Settings.

## Data sharing

We do not sell personal information. We do not share screenshot contents or recognized
text with advertisers, data brokers, or our own servers. Limited ML Kit operational data
is processed by Google as described above. User-initiated transfers to another Android
app occur only when you choose the corresponding action.

## Security

Shotlist limits screenshot-derived information to private app storage and uses Android
permission controls. No storage or software system can be guaranteed completely secure.
Keep your device and lock screen secure, especially if screenshots contain access codes
or other sensitive information.

## Retention and deletion

The production app will provide a `Delete all Shotlist data` control. You can also delete
local Shotlist data through Android Settings or by uninstalling the app. Because the
current release has no Shotlist account or Shotlist server storage, there is no
server-side screenshot history to delete.

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
