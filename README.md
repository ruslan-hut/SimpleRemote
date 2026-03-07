# SimpleRemote

An Android app for warehouse and retail document management that integrates with 1C:Enterprise servers over HTTP. Users can browse documents, scan barcodes (hardware scanner or camera), edit line items, and sync changes back to a remote 1C database.

Available on [Google Play](https://play.google.com/store/apps/details?id=ua.com.programmer.simpleremote).

## Features

- Browse and filter documents by type (sales, inventory, assembly, etc.)
- View and edit document line items with quantity tracking
- Barcode scanning via hardware scanner or built-in camera (CameraX + ML Kit)
- Product catalog browsing with search and hierarchical groups
- Document locking for concurrent editing safety
- Multi-server connection management
- Landscape-optimized tablet/terminal UI

## Architecture

- **Pattern:** MVVM + Repository, Hilt for dependency injection
- **UI:** Single-Activity with Navigation Component, ViewBinding, Material Design
- **Network:** Retrofit client communicating with a 1C:Enterprise HTTP service
- **Local storage:** Room database for connection settings only — no document caching
- **Async:** Kotlin Coroutines + StateFlow/LiveData
- **Crash reporting:** Firebase Crashlytics

See [API.md](API.md) for the server-side HTTP API specification.

## Requirements

- Android 7.0+ (API 24)
- 1C:Enterprise server with the compatible HTTP service published (see [API.md](API.md))

## Build

```bash
# Debug APK (full flavor)
./gradlew assembleFullDebug

# Release APK (full flavor)
./gradlew assembleFullRelease

# Standard flavor
./gradlew assembleStandartDebug

# Run unit tests
./gradlew testFullDebugUnitTest

# Run instrumented tests
./gradlew connectedFullDebugAndroidTest
```

**Product flavors:** `full` and `standart` — differ only in version name suffix (`.f` / `.s`).

Each build auto-increments the patch version in `app/version.properties`. Version format: `0.PATCH`.

## Project Structure

```
app/src/main/java/ua/com/programmer/simpleremote/
├── ui/            # Fragments + ViewModels by feature
├── repository/    # Repository interfaces
├── http/          # Retrofit API client, auth, request/response DTOs
├── dao/           # Room database, DAOs, connection settings entities
├── di/            # Hilt modules
└── entity/        # Domain models
```

## Contributing

Contributions are welcome. Please open an issue first to discuss proposed changes.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
