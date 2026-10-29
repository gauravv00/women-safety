# Women Safety SOS

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Android Gradle Plugin](https://img.shields.io/badge/AGP-8.7.3-green.svg)](https://developer.android.com/studio/releases/gradle-plugin)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-orange.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-red.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

A native Android women's safety and emergency SOS application engineered with **Kotlin**, **Jetpack Compose (Material 3)**, and a strict **Clean Architecture (Data / Domain / UI)** pattern. Designed for life-critical reliability, rapid emergency broadcast, real-time live GPS tracking, and discrete escape workflows.

---

## Highlights & Core Features

### 1. Dual-Engine Live Tracking (Google Maps + OpenStreetMap)
- **Zero-Config Instant OpenStreetMap Fallback**: Real map tiles load out-of-the-box using `osmdroid` without requiring a Google Cloud billing account or API keys.
- **Hardware-Accelerated Google Maps**: Supports Google Maps SDK with high-contrast dark JSON styling, camera transitions, and precision accuracy radii when an API key is provided.
- **Dynamic 1-Tap Toggle**: Seamlessly switch between OpenStreetMap and Google Maps via the interactive provider chip.
- **LRU Geocoding Cache**: High-speed reverse-geocoding with in-memory caching to prevent network latency and UI stalls during movement.

### 2. Hyper-Realistic Indian Fake Call Escape
- Designed for discrete exits from unsafe or uncomfortable situations.
- **Authentic Indian Presets**: Realistic phone numbers (`+91 98101 23456`, `+91 98202 34567`) and caller identities (`Mom`, `Papa`, `Rohit Verma`, `Office HR`, `Cab Driver`).
- **Carrier Subtitles**: Displays realistic cellular badges (`Calling via Jio 5G`, `Calling via Airtel 5G`).
- **Interactive Call Screen**: Active in-call timer, working dial pad, mute/speaker toggles, and localized Indian English Text-to-Speech audio script.

### 4. Zero-Cost Guardian Network (Firebase Spark Plan)
- **Pairing Code Handshake**: 6-character safety codes (`SOS-XXXXXX`) allowing guardians to link to ward devices with zero cost.
- **Client-Side Fan-Out**: Automatically updates guardian inboxes via Firestore without requiring paid Cloud Functions.
- **FCM Push Notifications**: Delivers loud, high-importance heads-up notifications with vibration patterns even when guardian devices are backgrounded or killed.

### 5. Multi-Channel Emergency Dispatch
- **2-Second Hold SOS Button**: Tactile, scale-animated button preventing accidental triggers.
- **1-Tap Quick Dial**: Modal panel for immediate dialing of National Emergency Helpline (`112`), Women's Helpline (`1091`), and priority contacts.
- **Evidence Gathering**: Background audio evidence recording automatically uploaded upon emergency resolution.

---

## Architecture

The project adheres to a strict layered **Clean Architecture**:

```
app/src/main/java/com/womensafety/sos/
├── data/                        # Repositories, Room Database, SharedPreferences, Network/Firebase
│   ├── local/                   # Room DB (TrustedContact, IncidentLog, PairedWard)
│   ├── location/                # Reverse geocoding & address resolution (LRU cache)
│   ├── pref/                    # UserPreferencesRepository (safety code, settings)
│   └── repository/              # SafetyRepositoryImpl, EmergencyRepositoryImpl, PairingRepositoryImpl
├── domain/                      # Domain interfaces and core business models
│   ├── model/                   # Data classes (Contact, Incident, Emergency, Notification)
│   └── repository/              # SafetyRepository, EmergencyRepository, PairingRepository
├── service/                     # Background services & hardware integration
│   ├── EmergencyForegroundService.kt  # Sticky service handling GPS, audio recording & siren
│   ├── SosTriggerHelper.kt            # Centralized emergency dispatcher
│   ├── NotificationHelper.kt          # System notification channels & banners
│   └── AppFirebaseMessagingService.kt # FCM background notification receiver
└── ui/                          # Jetpack Compose UI & ViewModels
    ├── components/              # SafetyMap, SosButton, EmergencyCallPanel, AppTopBar
    ├── screens/                 # Home, LiveTracking, FakeCall, Contacts, Settings, Notifications
    ├── theme/                   # Color.kt, Theme.kt, Type.kt (Dark Brutalist palette)
    └── MainActivity.kt          # App entrypoint
```

---

## UI / UX Design Style

The app features a **Dark Brutalist-Clean** visual language optimized for high visibility and night readability:

| Element | Color Hex | Preview / Purpose |
|---------|-----------|-------------------|
| `DarkBackground` | `#0A0A0A` | Deep contrast background, battery efficient |
| `DarkSurface` | `#121212` | Card and sheet backgrounds |
| `DarkSurfaceVariant` | `#1E1E1E` | Secondary containers and inputs |
| `AlertRed` | `#FF1744` | High-urgency SOS buttons and emergency banners |
| `SafetyGreen` | `#00C853` | Safe status indicators and call accept buttons |
| `TrustBlue` | `#2979FF` | Primary actions and location markers |

---

## Getting Started

### Prerequisites
- **Android Studio** Ladybug (2024.2.1) or newer
- **JDK 17** or **JDK 21**
- **Android SDK Platform 35**
- Android device or emulator running **Android 8.0 (API 26)** or higher

### Setup & Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/gauravv00/women-safety.git
   cd women-safety
   ```

2. **Configure Local Properties:**
   Open or create `local.properties` in the project root:
   ```properties
   sdk.dir=C:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk
   
   # Optional: Google Maps API key (leave empty to use OpenStreetMap free tiles)
   MAPS_API_KEY=
   ```
   > **Note on Maps**: If `MAPS_API_KEY` is not provided, the app automatically runs on OpenStreetMap (`osmdroid`) with zero configuration or cost!

3. **Firebase Setup:**
   Ensure `google-services.json` is present in the `app/` directory configured for your Firebase project.

4. **Build and Run:**
   - **From Terminal:**
     ```bash
     # Build debug APK
     ./gradlew assembleDebug

     # Install directly to attached emulator / device
     ./gradlew installDebug
     ```
   - **From Android Studio:**
     Select `app` run configuration and click **Run (Shift + F10)**.

---

## Testing Workflows

### Multi-Device Guardian Pairing Test
1. **Device A (Ward)**:
   - Open **Settings** → Copy your unique 6-character pairing code (`SOS-XXXXXX`).
2. **Device B (Guardian)**:
   - Open **Settings** → Tap **Pair Ward** → Enter Ward's code and name → Confirm.
3. **Device A (Ward)**:
   - A bell badge appears on top bar → Open **Notifications**.
   - Tap **Accept** on the pending pair request.
4. **Emergency Simulation**:
   - Device A triggers SOS (hold button for 2 seconds).
   - Device B immediately rings with a high-priority heads-up emergency notification displaying Ward's name, coordinates, and live tracking map link.

---

## Security & Rules

Cloud Firestore security rules (`firestore.rules`) enforce strict authorization:
- Only authenticated users can access their own notifications and profiles.
- Emergency location feeds are only readable by authorized guardians paired with the ward.
- Client-side fan-out limits writes strictly to valid recipient notification sub-collections.

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
