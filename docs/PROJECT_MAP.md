# Project Map — Women Safety SOS

## Overview
Native Android app written in Kotlin + Jetpack Compose (Material 3), package `com.womensafety.sos`.
Provides emergency SOS broadcasts, automated contact escalation, live dual-engine location tracking (Google Maps & OpenStreetMap), shake-to-SOS detection, ultra-realistic fake call escape dialer, audio evidence recording, incident logging, and real-time paired guardian device monitoring.

---

## Architecture Layers
```
┌────────────────────────────────────────────────────────────────────────┐
│                      UI Layer (Jetpack Compose)                        │
│  HomeScreen │ LiveTrackingScreen │ FakeCallScreen │ Contacts │ Settings│
│  SafetyMap (Google Maps / OpenStreetMap Dual Engine)                   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ ViewModels
┌───────────────────────────────────▼────────────────────────────────────┐
│                         Domain Layer                                   │
│  SafetyRepository │ EmergencyRepository │ PairingRepository            │
│  NotificationRepository                                                │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ ServiceLocator DI
┌───────────────────────────────────▼────────────────────────────────────┐
│                          Data Layer                                    │
│  Room DB (TrustedContact, IncidentLog, PairedWard)                     │
│  UserPreferencesRepository (Encrypted / SharedPreferences)             │
│  Firebase: Cloud Firestore (users, notifications, pairs, emergencies)  │
│  Firebase Storage (audio evidence) │ Firebase Messaging (FCM)          │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                      Background Services                               │
│  EmergencyForegroundService │ SosTriggerHelper                         │
│  AudioRecorderService │ AppFirebaseMessagingService                    │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Key Components & Packages

### 1. UI Layer (`com.womensafety.sos.ui`)
- **`components.SafetyMap`**: Dual-engine live tracking map:
  - **Google Maps**: Hardware-accelerated vector map with high-contrast dark JSON styling, animated camera tracking, and custom marker/accuracy overlays.
  - **OpenStreetMap (`OsmMap`)**: Instant, 100% free fallback powered by `osmdroid-android:6.1.20`. Activated automatically when no Google Maps API key is configured or upon user selection. Uses dark inverted color matrix and overlay diffing.
  - **Interactive Provider Badge**: Floating chip allowing seamless 1-tap switching between Google Maps and OpenStreetMap.
- **`components.SosButton`**: High-contrast, tactile 2-second hold-to-activate emergency SOS trigger with scale animation and haptic feedback.
- **`components.EmergencyCallPanel`**: Modal bottom sheet with 1-tap emergency dial buttons for Police (112), Women Helpline (1091), and prioritized trusted contacts.
- **`screens.home.HomeScreen` & `HomeViewModel`**: Main dashboard with active SOS status and quick action panels (Fake Call, Live GPS, Contacts, Helplines).
- **`screens.map.LiveTrackingScreen` & `LiveTrackingViewModel`**: Live map tracking screen with device GPS telemetry, cached reverse-geocoding (LRU cache), accuracy radius indicator, and external map launcher.
- **`screens.fakecall.FakeCallScreen` & `FakeCallViewModel`**: Realistic incoming fake call escape tool:
  - Authentic Indian phone number presets (+91 98101 23456, +91 98202 34567, +91 98909 87654).
  - Indian caller presets (Mom, Papa, Rohit Verma, Office HR, Auto / Cab Driver).
  - Realistic cellular carrier subtitle ("Calling via Jio 5G" / "Calling via Airtel 5G").
  - Simulated active call interface with live timer, dial pad, mute/speaker toggles, and Indian English Text-to-Speech voice script.
- **`screens.contacts.TrustedContactsScreen` & `TrustedContactsViewModel`**: Contact priority hierarchy management with native contact picker, phone number sanitization, and SOS broadcast list.
- **`screens.settings.SettingsScreen` & `SettingsViewModel`**: Settings toggles (loud siren, escalation delay) and Guardian Device Pairing (view/copy/share safety code, enter ward code, active guardian links).
- **`screens.notifications.NotificationsScreen` & `NotificationViewModel`**: Real-time notification inbox displaying emergency alerts, pair requests (with Accept/Decline actions), and status updates.

---

### 2. Services & Background Hardware (`com.womensafety.sos.service`)
- **`SosTriggerHelper`**: Centralized, thread-safe dispatcher for initiating emergency workflows from any screen or system event.
- **`EmergencyForegroundService`**: Sticky foreground service managing active emergencies:
  - High-accuracy Fused Location Provider with throttled Firestore location updates.
  - Background audio evidence recording with Firebase Storage upload on resolution.
  - Local loud siren alarm option.
  - Direct SMS and phone call escalation to priority #1 contact.
- **`AppFirebaseMessagingService`**: Firebase Cloud Messaging (FCM) service delivering heads-up high-priority emergency alerts and pairing notifications even when the app is killed.
- **`NotificationHelper`**: Singleton notification manager configuring dedicated Android notification channels:
  - `emergency_channel` (High importance, heads-up sound & vibration).
  - `pairing_channel` (Default importance for pairing invites & updates).
  - `tracking_channel` (Low importance ongoing foreground service notification).

---

### 3. Data & Persistence (`com.womensafety.sos.data`)
- **`local.AppDatabase`**: Room Database (version 2) with SQLite tables:
  - `trusted_contacts`: Priority-ordered contact list with names and phone numbers.
  - `incident_logs`: Historical audit trail of emergency broadcasts, timestamps, and locations.
  - `paired_wards`: Locally cached guardian/ward pairings.
- **`pref.UserPreferencesRepository`**: Encrypted / SharedPreferences wrapper with reactive `SafetySettings` StateFlow, unique 6-character safety pairing code generator (`SOS-XXXXXX`), and user profile state.
- **`location.LocationAddressHelper`**: Thread-safe reverse-geocoder with LRU memory caching (50 entries) to resolve GPS coordinates to Indian addresses without network overhead or UI stalls.
- **`repository.EmergencyRepositoryImpl`**: Manages Firestore `emergencies/` documents, location points sub-collection, and client-side fan-out to paired guardian inboxes.
- **`repository.PairingRepositoryImpl`**: Handles Firestore pairing codes, `pairRequests/` documents, duplicate checks, and bidirectional `pairs/` synchronization.
- **`repository.NotificationRepositoryImpl`**: Real-time Firestore snapshot listener for `notifications/{uid}/items/` with read/delete/action mutations.

---

## Zero-Cost Guardian Pairing & Notification Architecture
1. **User Identity**: Devices sign in via Firebase Anonymous Authentication (`FirebaseAuthManager`), obtaining a stable `uid`.
2. **Pairing Handshake**:
   - Ward shares their 6-character code (e.g. `SOS-9X2K7B`).
   - Guardian enters the code in `SettingsScreen`.
   - A `pairRequests` record is generated, triggering a notification in the Ward's inbox.
   - Ward taps **Accept**, committing a bidirectional record in `pairs/{wardUid}_{guardianUid}`.
3. **Emergency Broadcast (Client-Side Fan-Out)**:
   - When Ward triggers SOS, `EmergencyForegroundService` creates an emergency document in `emergencies/{emergencyId}`.
   - The app reads all active guardians from `pairs` and writes emergency notification documents to `notifications/{guardianUid}/items/{notifId}`.
   - Guardians receive instant real-time Firestore inbox updates while the app is active, and FCM push notifications when backgrounded or killed.
4. **Resolution**:
   - Tapping "Resolve SOS" sets emergency status to `RESOLVED`, timestamps the event, saves audio evidence, and silences all guardian devices.
