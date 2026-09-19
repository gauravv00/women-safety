# Project Map - Women Safety SOS

## Overview
Native Android app written in Kotlin + Jetpack Compose (Material3) for emergency SOS broadcasts, trusted contacts escalation, live location tracking, incident logging, and fake call simulation.

## Components & Architecture
- **`com.womensafety.sos.ui.components.OsmMap`**: OpenStreetMap integration via `osmdroid-android` with inverted dark tile filtering, live location marker, accuracy circle, and polyline tracking path.
- **`com.womensafety.sos.ui.screens.map.LiveTrackingScreen`**: Live location tracking UI using `OsmMap` with dark brutalist-clean overlay card and location sharing.
- **`com.womensafety.sos.ui.navigation.MainNavGraph`**: Main navigation graph and root Scaffold handling bottom navigation and window insets.
- **`com.womensafety.sos.ui.components.AppTopBar`**: Top app bar component with transparent status bar edge-to-edge support.

## Dependencies
- `org.osmdroid:osmdroid-android:6.1.20` - OpenStreetMap tile rendering (no API keys / billing required).
- Room DAO & Repository implementation.
- Firebase Realtime Database & Storage.
- Jetpack Compose (Material3).
