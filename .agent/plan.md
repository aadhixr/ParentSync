# Project Plan

ParentSync: A free, open-source parental control and monitoring Android app featuring child device monitoring, app activity tracking, app usage limits/blocking, and remote management/monitoring via a web interface or companion dashboard.

## Project Brief

# Project Brief: ParentSync (MVP)

ParentSync is a free, open-source parental control and monitoring Android application designed to help parents monitor child device activity, set app usage limits, and manage permissions securely.

---

## Features

1. **App Activity Tracking**: Real-time monitoring and reporting of daily app usage, screen time statistics, and category breakdowns on the child's device.
2. **App Usage Limits & Blocking**: Ability to configure daily time quotas for specific apps or categories and instantly block restricted apps when limits are reached.
3. **Remote Management & Dashboard**: Companion view / remote dashboard interface enabling parents to review usage metrics and update blocking rules.

---

## High-Level Tech Stack

- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose & Material 3
- **Concurrency**: Kotlin Coroutines & StateFlow
- **Architecture**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF)
- **Navigation**: Jetpack Navigation 3 (State-driven navigation)
- **Adaptive Layouts**: Compose Material Adaptive library for multi-device support (phones, tablets, foldables)

## Implementation Steps

### Task_1_CoreArchitectureAndAppActivityTracking: Set up project data layer, MVVM architecture with StateFlow, and App Activity Tracking feature for real-time monitoring and daily usage stats.
- **Status:** COMPLETED
- **Updates:** Completed core architecture (MVVM with StateFlow) and App Activity Tracking screen with usage statistics, category breakdown, and usage repository.
- **Acceptance Criteria:**
  - project builds successfully
  - MVVM architecture with StateFlow established
  - App Activity Tracking screen displays usage statistics and category breakdown

### Task_2_AppUsageLimitsAndBlocking: Implement App Usage Limits & Blocking functionality, allowing parents to configure daily time quotas and instantly block restricted apps.
- **Status:** COMPLETED
- **Updates:** Implemented App Usage Limits & Blocking feature with Room local database, DAO, repository, ViewModel, and Jetpack Compose UI for setting daily quotas and instant blocking.
- **Acceptance Criteria:**
  - daily time quotas configuration works
  - restricted apps are blocked when limits are reached
  - quota management logic integrated with ViewModels

### Task_3_RemoteDashboardAndNavigation: Implement Jetpack Navigation, Material 3 Adaptive layouts, and Remote Management & Dashboard UI for monitoring and updating blocking rules.
- **Status:** COMPLETED
- **Updates:** Implemented Jetpack Navigation, Material 3 adaptive navigation (bottom bar for phones, nav rail for tablets/foldables), and Remote Dashboard screen with cloud/local DB sync status and quick management controls.
- **Acceptance Criteria:**
  - Compose Navigation configured across screens
  - Material 3 Adaptive layouts implemented
  - Remote Dashboard UI displays overview and rule updating controls

### Task_4_RunAndVerify: Run and Verify application stability, build pass, ensure no crashes, and confirm alignment with user requirements.
- **Status:** COMPLETED
- **Updates:** Verified app build, unit tests, stability, and Material 3 adaptive UI across phone and tablet emulators. No crashes encountered. All core requirements met.
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - instruct critic_agent to verify application stability and requirement alignment

### Task_5_BackgroundSyncServiceAndAPI: Implement Background Sync Service (Foreground service on child device) to periodically push usage reports, battery status, and app activity to the cloud database / server, including API_KEY integration.
- **Status:** COMPLETED
- **Updates:** Implemented DeviceSyncForegroundService, SyncApiService, SyncApiClient, and SyncRepository for background data syncing of usage reports and device status to cloud/web dashboard backend.
- **Acceptance Criteria:**
  - Foreground service successfully runs and collects usage reports and battery status
  - API_KEY integration configured for secure cloud syncing
  - project builds successfully with background service components

### Task_6_RemoteControlCommandReceiver: Implement Remote Control Command Receiver on the child device to listen for parent commands from the web dashboard (instant lock, update quotas, block app) and integrate with local management engine.
- **Status:** COMPLETED
- **Updates:** Implemented RemoteCommandReceiver, RemoteCommandPoller, DeviceStateManager, and DeviceSyncForegroundService integration for listening to and executing remote parent control commands (lock device, block app, update quotas).
- **Acceptance Criteria:**
  - Command receiver listens for remote instructions (instant lock, quotas, block app)
  - Remote commands trigger local app actions and updates
  - integration with background sync and local database verified

### Task_7_RunAndVerify: Run and Verify application stability for background sync and remote commands, build pass, ensure no crashes, and confirm alignment with user requirements.
- **Status:** COMPLETED
- **Updates:** Verified app build, unit/instrumented tests, stability, and remote control command integration across emulators. No crashes encountered. All requirements fully met.
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - instruct critic_agent to verify application stability and requirement alignment for background sync and remote control features
- **Duration:** N/A

