# Android Remote Logger

A robust **Remote Logging** library for Native Android applications (Kotlin/Java).

`android-remote-logger` mirrors the functionality of the Flutter `flutter-remote-logger` package, capturing logs, device metadata, and session information. It buffers them locally and uploads them to a remote target (Firebase or Supabase) in the background.

## Features

*   **Session-based Logging**: Every app launch creates a unique Session ID.
*   **Automatic Metadata**: Captures Device Model, Brand, SDK Version, etc. automatically.
*   **Persistent Device ID**: Uses a file-based UUID to ensure consistency with the Flutter implementation. Persists across restarts but resets on uninstall.
*   **Nested Remote Paths**: Supports uploading logs to custom nested folder structures (e.g. `project_name/v1.0/`).
*   **Local Buffering**: Logs are written to secure local storage (`.jsonl` files) ensuring no data loss on crashes.
*   **Backend Agnostic**: Comes with built-in uploaders for **Firebase** and **Supabase**.

## Getting Started

### Installation

Include the library in your `settings.gradle`:

```gradle
include ':android-remote-logger'
```

Add the dependency to your app's `build.gradle`:

```gradle
dependencies {
    implementation project(':android-remote-logger')
    // Add Firebase or Supabase dependencies as needed
}
```

### Configuration

#### Option A: Firebase

1.  Set up Firebase in your Android project.
2.  Ensure Storage/Firestore rules allow writes to `logs/{deviceId}/{sessionId}.jsonl` (or your custom `remotePath`).

#### Option B: Supabase

1.  Add `supabasekt` dependencies.
2.  Initialize Supabase Client.
3.  Configure Buckets/Tables (see Flutter README or SQL schema).

## Usage

### Initialization

Initialize `RemoteLogger` instance in your `Application` class or main Activity `onCreate`.

**Example: Using Supabase**

```kotlin
import com.infodreams.remotelogger.core.RemoteLogger
import com.infodreams.remotelogger.uploader.SupabaseLogUploader
// ...

val supabase = createSupabaseClient(url, key) { ... }

RemoteLogger.instance.initialize(
    context = this,
    uploader = SupabaseLogUploader(supabase),
    autoUploadIntervalMillis = 5 * 60 * 1000, // 5 minutes
    remotePath = "my_project/v1.0" // Optional: Nested folder path
)
```

**Example: Using Firebase**

```kotlin
import com.infodreams.remotelogger.core.RemoteLogger
import com.infodreams.remotelogger.uploader.FirebaseLogUploader

RemoteLogger.instance.initialize(
    context = this,
    uploader = FirebaseLogUploader(),
    remotePath = "my_project/production"
)
```

### Logging

```kotlin
RemoteLogger.instance.log("App started successfully")

RemoteLogger.instance.log(
    message = "Login failed",
    level = "ERROR",
    tag = "AUTH",
    payload = mapOf("reason" to "wrong_password")
)
```

### Linking User

```kotlin
RemoteLogger.instance.identifyUser("user-uuid-123")
```

### Force Upload

```kotlin
RemoteLogger.instance.uploadCurrentSession()
```
