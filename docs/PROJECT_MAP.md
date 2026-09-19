# Project Map — Women Safety SOS

## Overview
Native Android app written in Kotlin + Jetpack Compose (Material3), package `com.womensafety.sos`.
Provides emergency SOS broadcasts, automated contact escalation, live location tracking, audio evidence recording, incident logging, and paired guardian device monitoring.

## Architecture Layers
```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (Jetpack Compose)               │
│  HomeScreen │ LiveTrackingScreen │ Contacts │ Settings      │
└──────────────────────────────┬──────────────────────────────┘
                               │ ViewModels
┌──────────────────────────────▼──────────────────────────────┐
│                    Domain Layer                             │
│  SafetyRepository (Interface)                                │
└──────────────────────────────┬──────────────────────────────┘
                               │ ServiceLocator DI
┌──────────────────────────────▼──────────────────────────────┐
│                    Data Layer                               │
│  SafetyRepositoryImpl │ UserPreferencesRepository           │
│  Room: AppDatabase (TrustedContact, IncidentLog, PairedWard)│
│  Firebase: Realtime Database + Storage                      │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                    Background Services                      │
│  EmergencyForegroundService │ GuardianMonitorService        │
│  ShakeDetector │ AudioRecorderService                       │
└─────────────────────────────────────────────────────────────┘
```

## Key Components & Packages

### 1. UI Layer (`com.womensafety.sos.ui`)
- **`components.OsmMap`**: High-performance OpenStreetMap integration using `osmdroid-android:6.1.20`. Employs overlay diffing (reuses Marker, Polygon, Polyline across recompositions) and throttled `animateTo` (skips updates if moved < 5m) with dark tile color filtering.
- **`components.AppTopBar`**: Edge-to-edge top bar with proper window insets.
- **`components.EmergencyCallPanel`**: Modal bottom sheet with quick-dial buttons for Police (112), Women Helpline (1091), and prioritized trusted contacts.
- **`screens.home.HomeScreen` & `HomeViewModel`**: SOS big button with long-press trigger, active emergency status badge, quick actions, and shake detector integration.
- **`screens.map.LiveTrackingScreen` & `LiveTrackingViewModel`**: Live map rendering with debounced location updates, direct Emergency Call button, and live coordinate sharing.
- **`screens.contacts.TrustedContactsScreen` & `TrustedContactsViewModel`**: Contact priority hierarchy management with native contact picker and phone number sanitization.
- **`screens.settings.SettingsScreen` & `SettingsViewModel`**: Settings toggles (shake trigger, loud siren, auto-call delay, sequential escalation, battery-aware polling) and Guardian Device Pairing (view/copy/share safety code, add/remove monitored wards, toggle guardian monitoring).

### 2. Services (`com.womensafety.sos.service`)
- **`EmergencyForegroundService`**: Manages active emergency state:
  - High-accuracy Fused Location Provider with throttled Realtime Database writes (≥10m or ≥10s).
  - Location history capped at 500 points.
  - Background audio evidence recording with Firebase Storage upload on resolution.
  - Optional loud siren alarm.
  - Auto-calling priority #1 contact with fallback to dialer.
  - Realtime Database SOS broadcast to paired guardians (`guardians/{myPairingCode}`).
- **`GuardianMonitorService`**: Long-running background/foreground monitor service:
  - Attaches Firebase Realtime Database `ValueEventListener` to all paired wards.
  - On `"ACTIVE"` emergency status, triggers high-priority alarm notification with vibration and sound.
- **`ShakeDetector`**: Accelerometer-based shake detector with debounced trigger threshold.
- **`AudioRecorderService`**: 3GP/AAC microphone audio recorder.

### 3. Data & Persistence (`com.womensafety.sos.data`)
- **`local.AppDatabase`**: Room Database (version 2) with tables:
  - `trusted_contacts`: Priority-ordered contact list.
  - `incident_logs`: Historical & active emergency incidents.
  - `paired_wards`: Monitored ward codes, names, coordinates, and status.
- **`pref.UserPreferencesRepository`**: SharedPreferences wrapper with reactive `SafetySettings` StateFlow, unique 6-character safety pairing code generator (`SOS-XXXXXX`), and display name.
- **`util.PhoneUtils`**: Phone number sanitizer and validator preserving E.164 country codes.
- **`repository.SafetyRepositoryImpl`**: Single source of truth managing Room DB, Firebase Realtime Database (`incidents/` & `guardians/`), and Firebase Storage (`audio_evidence/`).

## Guardian Pairing Protocol (Zero-Cost Stack)
1. **Code Generation**: Primary device creates a unique code (e.g. `SOS-9X2K7B`) stored in `UserPreferencesRepository`.
2. **Pairing**: Guardian device adds ward by entering the code and a display name into `SettingsScreen`.
3. **Broadcasting**: When primary device triggers SOS, `EmergencyForegroundService` writes `{ "status": "ACTIVE", "latitude": lat, "longitude": lng, "wardName": name, "timestamp": timestamp }` to `guardians/{wardCode}` in Firebase Realtime Database.
4. **Alerting**: Guardian device running `GuardianMonitorService` receives the update and immediately fires a heads-up emergency danger notification with sound and vibration.
5. **Resolution**: On emergency cancellation, primary device writes `{ "status": "RESOLVED" }`, automatically silencing and clearing the emergency state.
