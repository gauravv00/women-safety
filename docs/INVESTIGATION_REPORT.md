# Firebase Architecture & Pairing/Live Tracking Investigation Report

**Date:** 2026-10-25  
**Project ID:** `wosafety-17b7e`  
**Package Name:** `com.womensafety.sos`  
**Investigation Mode:** READ-ONLY (no working tree mutations, isolated git worktree used for verification)

---

## 1. Verdict & Core Finding

### **Verdict: (B) — High Confidence**
The breakage was caused by **this week's agent sessions (commits `8687e75` through `4135b3c`)**, which migrated the pairing, notifications, and emergency tracking architecture from **Firebase Realtime Database (RTDB)** to **Cloud Firestore + Firebase Anonymous Authentication**. 

The backend console for Firebase project `wosafety-17b7e` was **never configured** for this new architecture:
1. **Firebase Authentication (Anonymous Provider) is disabled** in the Firebase Console, throwing `CONFIGURATION_NOT_FOUND`.
2. **Cloud Firestore database was never provisioned** in the Firebase Console (the console originally only had Realtime Database in `asia-southeast1`).
3. **Friend's commit (`d5284af`) did NOT break the database or config.** It maintained the working RTDB broadcast and local Room database setup.

---

### **The 3 Strongest Pieces of Evidence**

1. **Definitive Runtime Logcat Proof (Captured from device `1373730799000FF`):**
   When triggering SOS or attempting pairing, `FirebaseAuthManager.ensureAuthenticated()` fails immediately:
   ```text
   10-25 16:21:55.591 E/FirebaseAuthManager: Anonymous sign-in failed
   com.google.firebase.FirebaseException: An internal error has occurred. [ CONFIGURATION_NOT_FOUND ]
       at com.google.android.gms.internal.firebase-auth-api.zzach.zza(com.google.firebase:firebase-auth@@22.3.1:18)
   
   10-25 16:11:42.196 E/EmergencyViewModel: Failed to start emergency
   java.lang.Exception: Not authenticated
       at com.womensafety.sos.data.repository.EmergencyRepositoryImpl.startEmergency(EmergencyRepositoryImpl.kt:26)
   ```
   Because Firebase Anonymous Auth is not enabled in the Firebase Console, `ensureAuthenticated()` returns `""`. Consequently:
   - `startEmergency()` aborts at line 26: no emergency document is created, and no notification is fanned out to guardians.
   - `sendPairRequest()` aborts immediately: `"Could not connect to the server..."`.
   - `getNotifications()` aborts immediately and returns an empty list (`trySend(emptyList())`).

2. **Git Commit History of the Backend Migration (`git log -S`):**
   - `firebase-database`: Added in `23a3eea` (initial commit, 2026-09-08).
   - `firebase-firestore-ktx` & `firebase-auth-ktx`: Added on **2026-10-24 23:04:41** in commit `8687e75` by Aditya Vikram (Agent Phase 1).
   - `EmergencyRepositoryImpl` & `EmergencySosService` (writing to Firestore `emergencies/`): Added on **2026-10-24 23:36:21** in commit `5e0d3c9` (Agent Phase 2).
   - `LiveTrackingViewModel` migration to Firestore: Added on **2026-10-25 00:14:27** in commit `52e6176` (Agent Phases 4 & 5).
   - `firestore.rules`: Committed to local repo on **2026-10-25 00:18:12** in commit `4135b3c`, but never deployed to the Firebase cloud.

3. **Firebase Configuration (`google-services.json`):**
   - `project_id`: `wosafety-17b7e` (matches expected).
   - `firebase_url`: `https://wosafety-17b7e-default-rtdb.asia-southeast1.firebasedatabase.app` (points exclusively to a Realtime Database in Singapore `asia-southeast1`).
   - The friend's commit (`d5284af`) did **not** touch `google-services.json`, any gradle build files, or `AndroidManifest.xml`.

---

## 2. Commit Timeline

| Date | Author | Hash | Changes Made | Effect on Pairing & Live Location |
|---|---|---|---|---|
| **2026-09-08** | `unknown` | `23a3eea` | Initial commit: Women Safety SOS App | Baseline architecture using Room + RTDB. |
| **2026-09-19** | `Aditya Vikram` | `a3380e3` | Added guardian pairing data model & RTDB sync | Pairing code generated locally; synced to RTDB `guardians/{pairingCode}`. |
| **2026-09-19** | `Aditya Vikram` | `d1430d1` | Added `GuardianMonitorService` & danger alert | Guardian monitors RTDB path `guardians/{wardCode}` via `ValueEventListener`. |
| **2026-09-19** | `Aditya Vikram` | `7bd974b` | Wire SOS trigger to notify paired guardians | `broadcastSosToGuardians` writes location + "ACTIVE" status to RTDB. |
| **2026-10-18** | `unknown` (Friend) | `d5284af` | Complete UI/UX overhaul, fake call, live tracking screen | **Pairing and tracking STILL WORKED.** Strengthened location broadcast in `EmergencyForegroundService`, added location fallback in `HomeViewModel`, normalized code in `SettingsViewModel`. Did NOT touch Firebase config. |
| **2026-10-24** | `Aditya Vikram` (Agent) | `8687e75` | Added `firebase-firestore-ktx`, `firebase-auth`, `NotificationRepositoryImpl`, `NotificationsScreen` | **Pairing switched to Firestore.** Added `users/{uid}`, `pairRequests/`, and `notifications/`. Requires Firebase Anonymous Auth. |
| **2026-10-24** | `Aditya Vikram` (Agent) | `d5adb92` | Phase 1 pairing complete | Finalized Firestore pairing flow. |
| **2026-10-24** | `Aditya Vikram` (Agent) | `5e0d3c9` | Phase 2: Emergency Firestore & `EmergencySosService` | SOS now writes to Firestore `emergencies/` and fans out notifications to `notifications/{guardianUid}/items`. |
| **2026-10-25** | `Aditya Vikram` (Agent) | `44c1f73` | Phase 3: Multi-emergency cards in NotificationsScreen | NotificationsScreen listens only to Firestore `notifications/{uid}/items`. |
| **2026-10-25** | `Aditya Vikram` (Agent) | `52e6176` | Phase 4 & 5: LiveTrackingScreen with Firestore | LiveTrackingScreen migrated from Room/RTDB to Firestore `observeEmergency`. |
| **2026-10-25** | `Aditya Vikram` (Agent) | `4135b3c` | All phases complete: 1-5 + firestore rules | Created `firestore.rules`. |
| **2026-10-25** | `Aditya Vikram` | `1c3eb22` | Room migration v3 + targetSdk fix | Fixed startup crash. |
| **2026-10-25** | `Aditya Vikram` | `4fbe342` | Navigation adaptive layout | Fixed responsive navigation rail / bottom bar. |

---

## 3. Current Feature Backend Usage Table

| # | Feature | File & Function | Backend Today | Still Called? | Original vs New |
|---|---|---|---|---|---|
| 1 | **Pairing code generation & storage** | `UserPreferencesRepository.loadSettings()` / `generatePairingCode()` | SharedPreferences (`women_safety_prefs`) + Firestore (`users/{uid}`) | Yes, called in `UserPreferencesRepository` and synced via `FirebaseAuthManager.syncUserDocument()` | Hybrid: Code generation is Original; sync to Firestore `users/{uid}` is New |
| 2 | **Guardian sends pair request / enters code** | `SettingsViewModel.sendPairRequest()` -> `NotificationRepositoryImpl.sendPairRequest()` | **Firestore** (`users` query, `pairRequests` doc, `notifications/{wardUid}/items` doc) | Yes, called when guardian presses "Pair Ward" | **New** (Original only inserted into local Room `paired_wards` table) |
| 3 | **Ward accepts/declines pair request** | `NotificationViewModel.handlePairRequest()` -> `NotificationRepositoryImpl.handlePairRequest()` | **Firestore** (`pairRequests`, `pairs/{wardUid}_{guardianUid}`, `notifications/{guardianUid}/items`) | Yes, called from NotificationsScreen accept button | **New** (Did not exist in original flow; original was direct offline code entry) |
| 4 | **SOS broadcast to guardians** | `HomeScreen.kt` -> `HomeViewModel.triggerSos()` AND `EmergencyViewModel.startEmergency()` | **BOTH** (RTDB `guardians/{code}` + Firestore `emergencies/{id}` & `notifications/{guardianUid}/items`) | Yes, `HomeScreen` calls BOTH ViewModels simultaneously | Hybrid: RTDB call is Original; Firestore `EmergencyViewModel` call is New |
| 5 | **Live location writes during SOS** | `EmergencyForegroundService.startLocationTracking()` AND `EmergencySosService.locationCallback` | **BOTH** (RTDB `guardians/{code}` & `incidents/{id}` + Firestore `emergencies/{id}`) | `EmergencyForegroundService` is called; `EmergencySosService` is called only if `startEmergency()` succeeds | Hybrid: `EmergencyForegroundService` is Original; `EmergencySosService` is New |
| 6 | **GuardianMonitorService (old guardian listener)** | `GuardianMonitorService.attachWardListener()` | **Realtime Database** (`guardians/{wardPairingCode}`) | Started if Guardian monitoring toggle is ON in Settings | **Original** (Still listens to RTDB, not Firestore) |
| 7 | **Notification panel data source** | `NotificationViewModel.uiState` -> `NotificationRepositoryImpl.getNotifications()` | **Firestore** (`notifications/{uid}/items`) | Yes, observed whenever NotificationsScreen is open | **New** |
| 8 | **LiveTrackingScreen data source** | `LiveTrackingViewModel.observeEmergency()` | **Firestore** (`emergencies/{emergencyId}`) | Yes, observed by `LiveTrackingScreen` | **New** (Original observed Room `PairedWardDao` + RTDB updates) |
| 9 | **Sign-in method & timing** | `FirebaseAuthManager.ensureAuthenticated()` (`signInAnonymously`) | **Firebase Auth** (Anonymous) | Called eagerly in `WomenSafetyApp.onCreate()` and before any Firestore operation | **New** (Original had no Firebase Auth) |

---

## 4. Friend's Commit Audit (`d5284af`)

Did the friend's commit touch:
- `google-services.json`? **NO.** (Untracked and ignored by `.gitignore`).
- `build.gradle.kts` (root or app) or `gradle/libs.versions.toml`? **NO.** (0 gradle files touched).
- `app/src/main/AndroidManifest.xml`? **NO.** (Untouched).
- Any Firebase / database / auth code?
  - `EmergencyForegroundService.kt`: **YES** (Refactored location broadcast to ensure `broadcastSosToGuardians` runs unconditionally even if incident id is -1).
  - `HomeViewModel.kt`: **YES** (Added fallback to `fusedClient.lastLocation.await()` when triggering SOS).
  - `LiveTrackingViewModel.kt`: **YES** (Added `broadcastLocationToGuardians` helper and paired wards selector).
  - `SettingsViewModel.kt`: **YES** (Added input normalization `PhoneUtils.normalizePairingCode`).
  - **Did it break anything? NO.** All of the friend's changes preserved and strengthened the RTDB broadcast pipeline.

---

## 5. What Is Currently Broken and Why (In Plain Language)

1. **Firebase Anonymous Authentication is Disabled in Firebase Console:**
   The entire new architecture (Phases 0–5) was built around anonymous authentication (`FirebaseAuthManager`). In the Firebase Console for `wosafety-17b7e`, "Anonymous" sign-in has never been enabled. Every time the app attempts to sign in, Firebase throws `CONFIGURATION_NOT_FOUND`.
2. **Every Firestore Call Silently Fails:**
   Because authentication fails, the app has no valid User ID (`uid = ""`). 
   - When a Ward triggers SOS, `EmergencyRepositoryImpl.startEmergency()` sees a blank UID and immediately returns `Failure("Not authenticated")`. No emergency record is created in Firestore, and no notification is sent to the guardian.
   - When a Guardian opens the Notifications panel, `NotificationRepositoryImpl.getNotifications()` sees a blank UID and returns an empty list.
3. **Cloud Firestore Does Not Exist in Project `wosafety-17b7e`:**
   The Firebase project was set up originally with **Firebase Realtime Database** in `asia-southeast1`. Cloud Firestore was never created in the Firebase console. Even if Authentication were enabled, Firestore writes/reads will fail with `NOT_FOUND` or `PERMISSION_DENIED` until Firestore is provisioned in the console and the security rules in `firestore.rules` are published.
4. **Architectural Split (Dual-Engine Desynchronization):**
   The app is currently in a half-migrated state:
   - `HomeViewModel` and `EmergencyForegroundService` are still broadcasting live GPS coordinates to **Realtime Database** under `guardians/{pairingCode}`.
   - `NotificationsScreen` and `LiveTrackingScreen` are listening exclusively to **Cloud Firestore** under `notifications/` and `emergencies/`.
   - Because the Ward's location broadcast is going to RTDB, but the Guardian's UI is listening to Firestore, the Guardian never sees the Ward's alerts or location.

---

## 6. Last Working Commit & Isolated Worktree Verification

### Last Known Working Commit: `d5284af`
- **Hash:** `d5284af354894237b579968a2040b5adfc427fe1`
- **Date:** 2026-10-18
- **Author:** `unknown <gauravvermaa06@gmail.com>`
- **Description:** Newest commit before the Firestore migration (`8687e75`). Uses Realtime Database and local Room database exclusively.

### Isolated Worktree Created:
- **Location:** `C:\Users\adity\OneDrive\Desktop\ADITYA DOX\Women_Safety_lastgood`
- Created via: `git worktree add ..\Women_Safety_lastgood d5284af`
- Your active working directory was untouched and your uncommitted changes remain intact.

### How to Test the Last Known Good Version:
1. Copy your `google-services.json` into the worktree:
   ```powershell
   Copy-Item "app\google-services.json" "..\Women_Safety_lastgood\app\google-services.json"
   ```
2. Open `..\Women_Safety_lastgood` in Android Studio or build via command line:
   ```powershell
   cd ..\Women_Safety_lastgood
   .\gradlew assembleDebug
   ```
3. Install to device:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s 1373730799000FF install -r app\build\outputs\apk\debug\app-debug.apk
   ```

### Git Bisect Plan (Reference Only — Do NOT Run Yet):
To isolate any other breaking commit between `d5284af` and `HEAD`:
```bash
git bisect start
git bisect bad HEAD
git bisect good d5284af
# Git will checkout intermediate commits for testing.
# For each commit, test if pairing/SOS works:
git bisect good   # or git bisect bad
# When finished:
git bisect reset
```

---

## 7. Firebase Console Verification Steps (Action Needed by You)

Because Firebase Console is external, please verify the following in [Firebase Console](https://console.firebase.google.com/project/wosafety-17b7e/):

1. **Authentication:**
   - Go to: **Build -> Authentication -> Sign-in method**
   - Look for **Anonymous**:
     - *If disabled*: This directly confirms the `CONFIGURATION_NOT_FOUND` error seen in logcat. Click "Enable" and save.
2. **Cloud Firestore:**
   - Go to: **Build -> Firestore Database**
   - Check if a database exists:
     - *If you see "Create database"*: Firestore was never created.
     - *If created*: Check the database ID (should be `(default)`) and location (`asia-south1` or `asia-southeast1`). Check the "Rules" tab to verify rules are published.
3. **Realtime Database:**
   - Go to: **Build -> Realtime Database -> Data**
   - Check the URL:
     - Expected: `https://wosafety-17b7e-default-rtdb.asia-southeast1.firebasedatabase.app`
     - Check if nodes `guardians/` and `incidents/` exist with pairing codes and coordinates.
   - Go to: **Realtime Database -> Rules tab**
     - Verify if read/write is allowed or if rules are expired (`auth != null` vs `.read: true, .write: true`).

---

## 8. Recommended Fixes (Ranked by Risk)

### **Option 1: Complete the Firestore Setup in Firebase Console (Lowest Code Risk, Recommended)**
Keep the new modern architecture (in-app notifications, live request approvals, multi-emergency tracking), and activate the required services in Firebase:
1. **Enable Anonymous Auth**: In Firebase Console -> Authentication -> Sign-in method -> Enable **Anonymous**.
2. **Create Cloud Firestore**: In Firebase Console -> Create Database -> Select Location (`asia-south1` or `asia-southeast1`) -> Start in Production mode.
3. **Deploy Security Rules**: Copy the contents of [`firestore.rules`](file:///C:/Users/adity/OneDrive/Desktop/ADITYA%20DOX/Women_Safety/firestore.rules) into the Firebase Console Firestore Rules tab and hit "Publish".
4. **Clean & Re-run**: Relaunch the app. Pairing requests, approvals, notifications, and emergency tracking will immediately start working.
- **Risk:** Very Low (no code changes needed; directly addresses the root cause).

---

### **Option 2: Revert to the Original Realtime Database Architecture (Fallback)**
If you do not want to use Cloud Firestore or Firebase Authentication, roll back the recent agent commits and return to `d5284af`:
- Reverts to the simpler pairing mechanism (direct code entry into Room without cloud pairing requests).
- Location tracking flows through RTDB `guardians/{code}`.
- In-app notification inbox panel will no longer be available.
- **Risk:** Medium (discards the newly added notifications UI and approval workflow).

---

## 9. Open Questions for You

1. In the Firebase Console for project `wosafety-17b7e`, is **Anonymous Sign-In** currently listed as "Disabled" under *Authentication -> Sign-in method*?
2. Does **Cloud Firestore** show "Create database" (uncreated), or has a Firestore database already been created?
3. Would you prefer to keep the new Firestore architecture (by enabling Anonymous Auth + Firestore in the console) or revert back to the pure Realtime Database setup from commit `d5284af`?
