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

To use Supabase, you need to set up your project with the required tables and storage bucket.

1.  **Dependencies**: Add `supabasekt` dependencies to your `build.gradle`.
2.  **Run Setup SQL**: Login to your Supabase Dashboard, go to the **SQL Editor**, and run the following script to create the necessary tables and buckets:

```sql
-- 1. Create Storage Bucket for logs
INSERT INTO storage.buckets (id, name, public) 
VALUES ('remote_logs', 'remote_logs', true)
ON CONFLICT (id) DO NOTHING;

-- Policy to allow uploading logs (Adjust capabilities as needed)
CREATE POLICY "Allow public uploads" ON storage.objects
FOR INSERT WITH CHECK ( bucket_id = 'remote_logs' );

CREATE POLICY "Allow public reads" ON storage.objects
FOR SELECT USING ( bucket_id = 'remote_logs' );

-- 2. Create Sessions Table
CREATE TABLE IF NOT EXISTS public.remote_log_sessions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    session_id TEXT NOT NULL UNIQUE,
    device_id TEXT NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    user_id TEXT,
    app_version TEXT,
    os_version TEXT,
    device_model TEXT,
    log_file_url TEXT,
    custom_data JSONB DEFAULT '{}'::jsonb
);

-- 3. Create Device Links Table (for User Identity)
CREATE TABLE IF NOT EXISTS public.remote_log_device_links (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    device_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    linked_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. Migration for Existing Databases (if remote_log_sessions already exists)
-- Run this ONLY if you already have the table and need to add the missing columns:
ALTER TABLE public.remote_log_sessions
ADD COLUMN IF NOT EXISTS app_version TEXT,
ADD COLUMN IF NOT EXISTS os_version TEXT,
ADD COLUMN IF NOT EXISTS device_model TEXT;

-- 5. Enable RLS (Security)
ALTER TABLE public.remote_log_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.remote_log_device_links ENABLE ROW LEVEL SECURITY;

-- 6. Create Open Policies (⚠️ FAST SETUP ONLY - Restrict in Production!)
-- These policies allow anyone (even unauthenticated) to insert logs.
CREATE POLICY "Enable insert for all" ON public.remote_log_sessions
FOR INSERT WITH CHECK (true);

CREATE POLICY "Enable select for all" ON public.remote_log_sessions
FOR SELECT USING (true);

CREATE POLICY "Enable insert for all" ON public.remote_log_device_links
FOR INSERT WITH CHECK (true);
```

3.  **Reload Schema Cache**: After running the SQL, execute this command to ensure the API knows about the new tables:
    ```sql
    NOTIFY pgrst, 'reload config';
    ```

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
