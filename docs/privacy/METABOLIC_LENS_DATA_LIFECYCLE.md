# Metabolic Lens health-data lifecycle

Verified against current implementation: 2026-09-01

| Stage | Behavior |
| --- | --- |
| Permission | User grants read-only glucose access in Android Health Connect. No write, background, or extended-history permission is requested. |
| Ingest | Foreground refresh reads the user-selected source. Initial/recovery snapshots cover the latest 30 days and change tokens reconcile later additions and deletions. |
| Validation | Every finite positive value is retained. The source package, record ID, observation time, optional zone offset, value in mmol/L, specimen source, and import time are stored. |
| Display | Only Metabolic Lens reads health tables. Sources are not mixed. Health values never enter Inbox, Recall, search, memories, collections, quests, widgets, notifications, shares, or diagnostics. |
| Local moments | Meal, movement, sleep, and note markers are entered locally. They are visual context only and do not assert causation. |
| Standard export | Health samples, moments, and sync state are excluded. |
| Explicit health export | After biometric/device-credential unlock, an initially unchecked control and a second confirmation can add samples and moments as separate JSON files. Values remain canonical mmol/L. Sync tokens are excluded. |
| Disconnect and keep | Health Connect revocation is attempted; imported samples and moments remain local. |
| Disconnect and delete | Samples, moments, and sync state are erased even if permission revocation fails. |
| Delete all | Local health deletion proceeds even if Health Connect cannot be reached or revocation fails. Export cache is also cleared. |
| Platform deletion caveat | A valid change token propagates source deletions. After token expiry, recovery can inspect only the current 30-day window, so an older local copy can outlive deletion of its Health Connect source record until the user deletes local history. |
| Transport and backup | Shotlist has no network permission and Android backup is disabled. Health data remains in private app storage unless the user explicitly sends a gated export to another app. |
| Screen/log protection | The route and every modal request secure-window handling. Code must not log health values, timestamps, source IDs, or notes. |

This document is an engineering data map, not the public policy. Any code change that
adds a permission, destination, retention path, health feature, export, log, or backup
path must update this map, the public privacy policy, the Play declarations, and the
physical acceptance script before release.

