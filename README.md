# Shotlist

**Your screenshots become things that happen.**

Screenshot a flyer — Shotlist reads it on your phone and offers the calendar entry,
then reminds you the day before. Products, codes, recipes, plants, meals: the same
engine grows a module for each. Liquid-glass UI. Nothing ever leaves the device.

## Install on your phone

1. Open this repo's **Releases** page on your phone.
2. Download the newest `shotlist-N.apk`.
3. Open it and allow installing from your browser when prompted.

Every push to `main` builds a fresh APK automatically (GitHub Actions).

## Working on this repo

Two agents build this in tandem — see `BRIDGE.md` for the coordination protocol and
`docs/SPEC.md` for the product spec. The task board lives in `.bridge/` (local only).

- `docs/SPEC.md` — product spec: loops, feature tiers, module platform
- `docs/research/` — engineering validation (ingest, permissions, extraction tiers)
