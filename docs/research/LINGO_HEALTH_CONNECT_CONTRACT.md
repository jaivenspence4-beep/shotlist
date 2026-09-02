# Abbott Lingo → Health Connect → Metabolic Lens contract

Verified from official sources: 2026-09-01

## Supported integration

Metabolic Lens integrates through Android Health Connect, not directly with the Abbott
Lingo biosensor. The supported user path is:

1. The user applies and activates a compatible Lingo biosensor in Abbott's Lingo app.
2. In Lingo, the user opens You → Settings → App settings → Sync with Health Connect →
   Manage and grants the Lingo permissions needed to send its data.
3. In Shotlist, the user opens Metabolic Lens and grants Shotlist read access to glucose
   through Health Connect.
4. Metabolic Lens reads the selected Health Connect data origin while its screen is in
   use and stores an on-device copy for the chart.

There is no supported direct BLE contract for third-party reads in the researched
consumer documentation. Shotlist must not scan for, pair with, impersonate, or reverse
engineer the Lingo sensor. It must not request a Lingo username/password or call an
undocumented Abbott cloud endpoint.

## Abbott's published export behavior

Abbott states that Lingo:

- sends measurements from interstitial fluid, not capillary blood;
- writes one glucose value for each 5-minute interval;
- sends those values to Health Connect at an approximately 3-hour delay;
- exports only Lingo's measured range of 3.1–11.1 mmol/L; and
- may therefore show a different current average from Health Connect.

These are source-specific facts, not global filters. Metabolic Lens imports every finite,
positive record supplied by the user's selected Health Connect origin. It must not drop
valid records from another source merely because they fall outside Lingo's published
range.

The Health Connect API exposes these values through the platform record named
`BloodGlucoseRecord` and the read permission named `READ_BLOOD_GLUCOSE`. That platform
naming does not change Abbott's specimen claim. Metabolic Lens preserves the supplied
specimen source and uses `sensor glucose` only when all visible records are marked
interstitial fluid. Mixed or unknown specimens are labeled simply `glucose`.

## Product boundaries

- Never claim the Health Connect view is live or real-time.
- Show record age and stale states; do not infer freshness from the sensor's presence.
- Never provide threshold alarms, dosing, diagnosis, treatment, or causal claims about
  a meal, movement, sleep, or note marker.
- Keep sources separate. Auto-select only when Health Connect reports one origin;
  otherwise require the user to choose.
- Treat package names as identifiers discovered from Health Connect. Do not hardcode an
  assumed Lingo package name until it is verified on the release device.
- Do not claim Abbott endorsement or affiliation.

## Partnership route

Abbott publicly invites product-integration inquiries through its Lingo partner program:

- Email: `Lingopartnerships@abbott.com`
- Page: [Lingo for Partners](https://www.hellolingo.com/partners)

Contacting Abbott may establish a future supported commercial integration, but it is
not a prerequisite for the current Health Connect read path and does not authorize use
of Abbott marks beyond nominative compatibility wording.

## Sources

- [Lingo Support: sync Lingo with Health Connect](https://support.hellolingo.com/hc/en-us/articles/31154992786450-How-do-I-sync-Lingo-with-Health-Connect)
- [Lingo for Partners](https://www.hellolingo.com/partners)
- [Android: Health Connect data types](https://developer.android.com/health-and-fitness/health-connect/data-types)
- [Android: read Health Connect data](https://developer.android.com/health-and-fitness/health-connect/read-data)
- [Android: synchronize Health Connect data](https://developer.android.com/health-and-fitness/health-connect/sync-data)

