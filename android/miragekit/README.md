# MirageKit Android

A Kotlin implementation of the MirageKit client protocol for Android.

## Usage

1. Add the dependency to your app module.
2. Add `INTERNET` permission to your manifest.
3. Use `MirageClient` to discover and connect to hosts.

```kotlin
val client = MirageClient(context)

// Discovery
client.discoverHosts().collect { hosts ->
    // Show hosts in UI
}

// Connection
client.connect(selectedHost)

// View
val mirageView = findViewById<MirageSurfaceView>(R.id.mirageView)
mirageView.attachClient(client)
```

## Requirements

- Android 8.0 (API 26)+
