# Metabolic Lens — Google Play declarations

Verified against current code and official guidance: 2026-09-01

This is paste-ready preparation for the release that includes Metabolic Lens. Recheck
the exact release AAB and current Play Console wording before submission.

## Shipped data access

Metabolic Lens requests exactly one Health Connect permission:

`android.permission.health.READ_BLOOD_GLUCOSE`

It does not request glucose write access, background health access, or access to health
history older than Health Connect's default window. The app has no direct Bluetooth
connection to a sensor and does not call Abbott or another health-data server. It reads
only records the user has authorized in Health Connect while Metabolic Lens is in use.

Although Health Connect names the record type `BloodGlucoseRecord`, the app preserves
the record's specimen source. The UI says `sensor glucose` only when every displayed
record is marked `INTERSTITIAL_FLUID`; otherwise it uses the generic word `glucose`.

## Health apps declaration

Declare all health features in the same form:

- **Period tracking** for Track's local menstrual-flow log and estimates.
- **Nutrition and weight management** for Metabolic Lens's personal glucose reflection
  around user-added meal, movement, sleep, and note markers. Do not select disease or
  condition management unless the product is materially changed to provide that use.

### Health Connect permission justification

Paste for the read-glucose data type, adjusting only if the release behavior changes:

> Metabolic Lens reads glucose records that the user explicitly authorizes through
> Android Health Connect. It uses those records to draw a neutral 24-hour, 7-day, or
> 30-day history; show observed low, median, high, count, and data gaps; and place the
> user's own meal, movement, sleep, and note markers on the timeline. The user selects
> one source app when more than one source is present, so records from different origins
> are not mixed. Shotlist does not write health records, run health sync in the
> background, provide threshold alerts, recommend dosing, diagnose conditions, or send
> glucose records to Shotlist servers.

### External product disclosure

Use this in the store description near the Metabolic Lens feature:

> Metabolic Lens requires a compatible app or sensor service that writes glucose
> records to Android Health Connect. Abbott Lingo users need a compatible Lingo
> biosensor and the Lingo app with Health Connect sync enabled. Shotlist does not connect
> directly to the sensor. Lingo currently sends 5-minute interstitial-fluid readings to
> Health Connect after an approximately 3-hour delay, so Metabolic Lens is not a live
> glucose display.

Use this health disclaimer in the full store description:

> Shotlist is not a medical device and does not diagnose, treat, cure, or prevent any
> medical condition. Metabolic Lens presents previously recorded information for
> general reflection only. Consult a qualified healthcare professional for medical
> advice, diagnosis, or treatment.

Do not market Metabolic Lens as live, real-time, continuous monitoring, an alarm, a
diabetes-management tool, or a replacement for the source app or sensor.

## Data Safety answers for glucose data

Health Connect glucose records and the user's Metabolic Lens moments are accessed and
stored solely on the device. Under Play's on-device-processing rule, mark these health
data types as **not collected** and **not shared**, provided release verification still
shows no transmission or Android backup path for them.

The standard Shotlist export excludes all health tables. A user may deliberately export
health data only after biometric/device-credential unlock, selecting an initially
unchecked `Include health data` control, and confirming a second warning. That
user-initiated transfer goes to Android's share sheet and must be described in the
privacy policy even when it qualifies for Play's user-initiated-action exception.

Do not let this health-specific answer overwrite any conservative operational-data
declarations required by bundled Google SDKs for non-health features. The overall app
Data Safety form must still be reconciled with the exact dependency set, SDK Index, and
release traffic/IPC verification.

| Health data | Collected | Shared | Processing purpose |
| --- | --- | --- | --- |
| Health info: glucose records | No | No | On-device app functionality; user-authorized Health Connect read |
| Health info: meal, movement, sleep, and note moments | No | No | On-device app functionality; entered locally by the user |

## Storage, retention, and deletion wording

Paste into the public and in-app privacy disclosures:

> Shotlist copies authorized glucose records into its private on-device database so
> Metabolic Lens can display history without mixing source apps. It initially imports
> up to the most recent 30 days because it does not request Health Connect's extended
> history permission. New and deleted records are reconciled through Health Connect's
> change feed while that synchronization state remains valid. If the change
> history expires, Shotlist can recheck only the current 30-day window; an older local
> copy may therefore remain after its source record was deleted from Health Connect.
> Use Metabolic Lens → Disconnect → Delete local history or Shotlist → Delete all my
> data to remove local glucose records and moments immediately.

Both Metabolic Lens disconnect choices attempt to revoke Shotlist's Health Connect
permission. `Keep history` retains the already imported local copy. `Delete local
history` erases samples, moments, and sync state even if Android's permission-revocation
call fails. `Delete all my data` also performs local deletion independently of whether
revocation succeeds. Clearing app storage or uninstalling erases Shotlist's local copy
and Android revokes the app's Health Connect permissions.

## Release checks

- Confirm the merged release manifest contains read glucose only—no write, background,
  or extended-history health permission.
- Confirm the merged release manifest contains neither `INTERNET` nor
  `ACCESS_NETWORK_STATE` for Shotlist.
- Exercise grant, deny, two cancels, revoke, pause, both disconnect choices, and Delete
  all my data on a physical device.
- Verify standard export excludes health; verify the gated health export contains only
  canonical mmol/L JSON plus moments and a warning README.
- Verify every Metabolic Lens route, sheet, dialog, screenshot, and Recents thumbnail is
  protected, and verify logcat contains no health values, timestamps, source IDs, or
  notes.
- Replace all publication placeholders and publish the final privacy policy at an
  active, public, non-geofenced, non-PDF URL.

## Official references

- [Publish a Health Connect app](https://developer.android.com/health-and-fitness/health-connect/publish)
- [Health Connect data types and permissions](https://developer.android.com/health-and-fitness/health-connect/data-types)
- [Health Connect raw-data history limits](https://developer.android.com/health-and-fitness/health-connect/read-data)
- [Google Play Health Content and Services policy](https://support.google.com/googleplay/android-developer/answer/16679511)
- [Google Play Data Safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Abbott Lingo Health Connect behavior](https://support.hellolingo.com/hc/en-us/articles/31154992786450-How-do-I-sync-Lingo-with-Health-Connect)
