# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SimpleRemote is an Android app for warehouse/retail document management that integrates with 1C:Enterprise servers via HTTP. Users browse documents, scan barcodes, edit line items, and sync with a remote 1C database. There is no local data caching — all document data is fetched on-demand.

## Build & Run

```bash
# Build debug APK (full flavor)
./gradlew assembleFullDebug

# Build release APK (full flavor)
./gradlew assembleFullRelease

# Build standard flavor
./gradlew assembleStandartDebug

# Run unit tests
./gradlew testFullDebugUnitTest

# Run instrumented tests
./gradlew connectedFullDebugAndroidTest

# Clean build
./gradlew clean
```

**Note:** Each build auto-increments the patch version in `app/version.properties`. The version format is `0.PATCH` with flavor suffix (`.f` for full, `.s` for standart).

**Product flavors:** `full` and `standart` — differ only in version name suffix.

## Architecture

**Pattern:** MVVM with Repository pattern, using Hilt for DI.

**Package structure** (`ua.com.programmer.simpleremote`):
- `ui/` — Fragments + ViewModels, organized by feature (document, selector, connection, camera, catalog, shared)
- `repository/` — Repository interfaces (`NetworkRepository`, `ConnectionSettingsRepository`)
- `http/` — Retrofit API client, auth interceptor, token refresh, request/response DTOs
- `dao/` — Room database, DAO interfaces, entity classes for local connection settings
- `di/` — Hilt modules: `GlobalModule` (DB, FileManager), `Repository` (Retrofit/network), `DomainModule` (bindings)
- `entity/` — Domain models (Document, Content, Product, Catalog, UserOptions, etc.)

**Navigation:** Single-Activity (`MainActivity`) with Navigation Component + SafeArgs. Start destination is `selectDocumentTypeFragment`. Navigation graph at `res/navigation/navigation.xml`.

**SharedViewModel** is activity-scoped and bridges data between fragments (current connection, user options, active document, barcode input).

## Network Layer

All communication goes through Retrofit to a 1C server at `{server}/{databaseName}/hs/rc/`. Authentication uses Basic Auth for initial check, then token-based for subsequent requests. `HttpAuthInterceptor` adds credentials; `TokenRefresh` authenticates on 401 with up to 3 retries.

Cleartext HTTP is allowed (`android:usesCleartextTraffic="true"`) because 1C servers often run without TLS.

## Local Storage

Room database (`simple_remote_database`, version 5) stores only `ConnectionSettings` — no document caching. Current migration path: `MIGRATION_3_5`.

## Barcode Scanning

Two input methods:
1. **Hardware scanner** — handled in `MainActivity.dispatchKeyEvent()` with 60ms keystroke timeout to distinguish scan input from typing
2. **Camera** — CameraX + ML Kit barcode detection in `CameraFragment`/`BarcodeImageAnalyzer`

## Key Conventions

- **Language:** Kotlin, JVM target 1.8
- **UI:** ViewBinding (not Compose), Material Design components
- **Async:** Coroutines with `viewModelScope`, `Dispatchers.IO` for network/DB, LiveData for UI state
- **Code generation:** KSP for Hilt and Room (not kapt, though kapt plugin is still declared)
- **Orientation:** Landscape-locked (`screenOrientation="landscape"`)
- **Localization:** Ukrainian (values-uk), Russian (values-ru) string resources
- **ProGuard:** Keeps all entity classes (`dao.entity.*`, `http.entity.*`, `entity.*`) and Retrofit/Gson configs
- **Firebase:** Crashlytics for error reporting, Analytics, Firestore, Auth
