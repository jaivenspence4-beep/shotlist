# Health Connect security boundary

Metabolic Lens reads glucose records from Android Health Connect only after an
in-context, user-initiated permission request. It requests only
`android.permission.health.READ_BLOOD_GLUCOSE`. The app does not request write,
background-read, or history access.

## Exported rationale activity

Health Connect requires a rationale activity for
`androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` and an Android 14+ activity
alias for `android.intent.action.VIEW_PERMISSION_USAGE`. This is Shotlist's only
exported activity besides `MainActivity`.

`HealthConnectRationaleActivity` is intentionally non-interactive and isolated:

- it ignores every Intent action, data URI, extra, and caller value;
- it reads no Room, preference, screenshot, or Health Connect data;
- it exposes no navigation or privileged action;
- it sets `FLAG_SECURE` before composing its static privacy explanation; and
- it is excluded from the Recents screen.

The alias requires `android.permission.START_VIEW_PERMISSION_USAGE`, so ordinary
third-party apps cannot launch that Android 14 entry point.

## Offline enforcement

The source manifest strips `INTERNET` and `ACCESS_NETWORK_STATE`, including
requests merged from dependencies. CI inspects the merged debug manifest after
building and fails if either permission survives. Release acceptance repeats the
same assertion against the release artifact.

The `com.google.android.apps.healthdata` package query only lets Android locate
the Health Connect provider on Android 13 and lower. It grants no data or network
access. Glucose records remain in the on-device Room database with platform
backup disabled.
