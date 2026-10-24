# AGENTS.md — Women Safety SOS

## Project Context
Android app (Kotlin + Jetpack Compose, Material 3). Package: `com.womensafety.sos`.
Architecture: **data / domain / ui** (strict layering — no Firebase or Room calls in composables or ViewModels directly, always through repository interfaces).

## Firebase Plan
**Spark (free)** — client-side fan-out. No Cloud Functions.
- Ward writes notification docs for each guardian directly.
- Guardian app listens to Firestore while alive; shows local notifications from FCM when killed.

## Firestore Collections

| Collection | Purpose |
|------------|---------|
| `users/{uid}` | `displayName`, `fcmToken`, `pairingCode`, `lastSeen` |
| `notifications/{uid}/items/{notifId}` | Per-user notification inbox (real-time Firestore listener) |
| `pairRequests/{requestId}` | Pending pair requests (for audit trail) |
| `pairs/{wardUid}_{guardianUid}` | Active pairs |
| `emergencies/{emergencyId}` | Emergency event docs with location history sub-collection |

## Architecture Rules
- All Firestore/Firebase calls → `data/` layer only
- Interfaces live in `domain/repository/`
- ViewModels use `ServiceLocator` to get repositories; never instantiate repos directly
- Composables are pure UI — no logic, no coroutine launches except `LaunchedEffect`
- `NotificationHelper` (singleton object) for all system notification posting
- `FirebaseAuthManager` ensures anonymous auth before any Firestore write

## Build Rules
- Compile after every phase; do not start next phase on failing build
- Prefer small targeted edits (`str_replace`-style) over full rewrites
- CRLF files (Manifest, CRLF-mixed screens) may be full-replaced when necessary

## UI Style
Dark brutalist-clean: `DarkBackground` (#0A0A0A), `DarkSurface` (#121212), `DarkSurfaceVariant` (#1E1E1E), `AlertRed` (#FF1744), `SafetyGreen` (#00C853), `TrustBlue` (#2979FF).
No white backgrounds. Cards use `RoundedCornerShape(16.dp)`. Chips/badges use `RoundedCornerShape(8.dp)`.

## Phase Checklist
- [x] Phase 0 — Notification Panel (NotificationsScreen, NotificationViewModel, NotificationRepository, FCM service, channels)
- [x] Phase 1 — Firestore Pairing (pairCodes lookup, pairRequests, pairs, duplicate block, unpair)
- [x] Phase 2 — Emergency Firestore docs + foreground service location writes to Firestore
- [x] Phase 3 — Multi-emergency notification cards
- [x] Phase 4 — Live Firestore → osmdroid tracking
- [x] Phase 5 — Resolve flow + resolved-card final location
- [x] Security rules (`firestore.rules`)
- [ ] Offline persistence, SMS fallback, expired-code errors

## Emulator Test Steps (after each phase)

### Phase 1
1. **Emulator A (Ward)**: Open Settings → copy pairing code
2. **Emulator B (Guardian)**: Open Settings → "Pair Ward" → enter Ward's code + name → confirm
3. **Emulator A (Ward)**: Bell badge appears → Notifications → PAIR_REQUEST card with Accept/Decline
4. Accept → badge clears on A; B gets PAIR_ACCEPTED notification
