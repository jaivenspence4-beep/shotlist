# Metabolic Lens physical acceptance

Status: **PENDING — phone intentionally disconnected while the laptop charges**

Prepared: 2026-09-02

Run this only on the exact release candidate after claiming the bridge device lock. Use
a supported Android phone with Health Connect and Abbott Lingo installed. Do not copy a
Health Connect database, Lingo database, raw logcat dump, or real glucose screenshot off
the phone. Record pass/fail and non-sensitive metadata only: app version, Android
version, device model, source-app display name, permission state, and approximate record
age.

## Preconditions

- [ ] Claude has released `DEVICE LOCK t55` and the accepting agent has claimed it.
- [ ] The phone has sufficient charge, is unlocked, and has authorized USB debugging.
- [ ] `adb devices -l` reports exactly the intended device as `device`, not
  `unauthorized`, `offline`, or an empty list.
- [ ] The candidate APK matches a fully green CI commit and upgrades in place.
- [ ] Lingo has a working sensor session and Lingo → Health Connect sync is enabled.
- [ ] No screen recorder, notification mirroring, desktop phone link, or cloud screenshot
  upload is active during the health pass.

## 1. Install and route integrity

- [ ] Install the CI APK without clearing existing app data unless this is the clean-run
  portion of the test.
- [ ] Launch Shotlist and open You → Modules → Metabolic Lens.
- [ ] Close with the in-app back button and Android system Back; both return to Shotlist
  without exiting or leaving an invisible overlay.
- [ ] Open from Track. Before setup, Track shows the generic dismissible private-tracker
  invitation; after connection or retained history, it shows the named Metabolic Lens
  entry.
- [ ] Rotate portrait → landscape → portrait. The selected window, open state, and data
  remain coherent, with no clipped controls or duplicate permission prompt.
- [ ] Background and resume the app. It rechecks Health Connect once, does not perform a
  background read, and does not duplicate moments or records.

## 2. Availability and permission states

Exercise only states that can be reached safely on the device/build under test.

- [ ] Initial setup explains read-only Health Connect access and the delayed source path;
  it makes no live, alert, diagnosis, treatment, or dosing claim.
- [ ] First denied/cancelled permission request returns to setup and says nothing was
  read.
- [ ] Second denied/cancelled request routes to Android's Manage access path instead of
  repeatedly prompting.
- [ ] Granting access returns to Metabolic Lens and imports without app restart.
- [ ] Revoking access in Health Connect is reflected when Shotlist resumes. Existing
  local history may remain, but the UI does not say access is connected.
- [ ] Pause stops refresh while preserving local history. Resume refreshes in the
  foreground.
- [ ] Both disconnect choices attempt revocation. Keep retains samples and moments;
  Delete removes samples, moments, and sync state even if provider revocation fails.

## 3. Lingo source contract

- [ ] Lingo appears as the selected Health Connect source by its installed app label. If
  more than one source exists, Metabolic Lens requires a choice and never combines
  sources.
- [ ] Visible Lingo records are approximately five minutes apart where coverage is
  continuous.
- [ ] The newest visible Health Connect/Lingo record is delayed rather than presented as
  a current sensor value. Record only the approximate age bucket, never the value or
  exact timestamp.
- [ ] When all displayed records report interstitial fluid, the UI says `sensor glucose`.
  With a fabricated mixed/unknown specimen fixture, it downgrades to `glucose`.
- [ ] The 24h, 7d, and 30d views use data-driven axes, show line breaks for gaps, and do
  not clip finite positive fixture values.
- [ ] Low, median, high, count, and gaps match a small fabricated Health Connect Toolbox
  fixture. Never use the person's real readings as QA assertions.

## 4. Moments and controls

- [ ] Add meal, movement, sleep, and note moments for Now, 1h ago, and 3h ago using
  fabricated notes. `Now` uses the tap/save time even after the screen has remained open.
- [ ] Markers align with their chosen times and copy never says a marker caused a glucose
  change.
- [ ] Delete a moment and verify it disappears without deleting a glucose record.
- [ ] Toggle mmol/L and mg/dL; fixture conversions and rounding match the unit tests.
- [ ] Manual refresh is debounced/serialized and cannot create duplicate rows.
- [ ] Settings accurately shows paused/access/source state after revoke, keep-history,
  delete-history, rotation, and process resume.

## 5. Screen and Recents protection

Use fabricated Health Connect Toolbox records for visual security tests. Do not create or
retain a screenshot containing the person's real readings.

- [ ] Android's hardware screenshot gesture is blocked on the main Metabolic Lens route.
- [ ] It is blocked independently on source chooser, Add moment, Settings, and Disconnect
  confirmation windows.
- [ ] After opening each surface, entering Recents shows no readable chart, value,
  timestamp, source identifier, or note.
- [ ] Switching away and returning does not briefly flash an unsecured health frame.
- [ ] Closing Metabolic Lens restores normal screenshot behavior elsewhere in Shotlist;
  the secure flag does not leak across the whole app lifetime.

Record only `blocked`, `redacted`, or `failed` for each surface. If any capture succeeds,
delete it immediately on-device, stop the test, and treat the release as blocked.

## 6. Logging and network quarantine

- [ ] Clear logcat immediately before the pass. Exercise grant, sync, source choice,
  chart, moments, units, revoke, both disconnect paths, and Delete all.
- [ ] Inspect the Shotlist process log locally on the test machine without saving or
  attaching the raw output. There are no glucose values/units, record timestamps,
  source packages, record IDs, notes, or serialized health objects.
- [ ] Record only the count of suspected lines and pass/fail. If nonzero, redact values
  before creating a defect report.
- [ ] Reconfirm the installed APK's merged manifest contains no `INTERNET` or
  `ACCESS_NETWORK_STATE` and contains read glucose only—no write, background-health, or
  extended-history permission.

## 7. Export and deletion

- [ ] A standard export contains no health tables, health filenames, values, moments, or
  sync metadata.
- [ ] Health inclusion starts unchecked, requires biometric/device-credential unlock,
  then requires a second explicit confirmation.
- [ ] The gated export contains separate sample and moment JSON files, canonical mmol/L
  units, no sync token, no graph bitmap, and a sensitive-health-data warning.
- [ ] Cancel at each gate and verify the next standard export still excludes health.
- [ ] Delete all clears database rows, private copies, cached export ZIPs, diagnostic
  logs, health/track prompt preferences, and local settings while leaving original
  screenshots and Health Connect's own records untouched.
- [ ] Simulate or instrument provider revocation failure: local deletion still succeeds.

## Result record

Do not mark this document passed until every applicable box is checked.

| Field | Result |
| --- | --- |
| Commit / version | Pending |
| Device / Android | Pending |
| Health Connect provider | Pending |
| Lingo source visible | Pending |
| Permission and lifecycle | Pending |
| Source/timestamp behavior | Pending |
| Secure route and modals | Pending |
| Recents protection | Pending |
| Log quarantine | Pending |
| Export/deletion | Pending |
| Final verdict | **PENDING** |

