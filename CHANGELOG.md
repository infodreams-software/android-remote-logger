## 0.1.3
* **Feature**: Implemented automatic retry with exponential backoff for failed uploads (1min, 5min, 15min intervals).
* **Robustness**: Upload failures no longer require manual intervention - the logger will automatically retry before falling back to retry-on-restart.
* **Docs**: Documented upload retry behavior in README.

## 0.1.2
* **Fix**: Updated Supabase SQL schema in README to include missing columns (`app_version`, `os_version`, `device_model`) that are referenced in `SupabaseLogUploader` but were not in the original CREATE TABLE statement.
* **Docs**: Expanded Supabase configuration section with complete SQL setup script, matching Flutter package documentation.
* **Docs**: Added migration SQL for existing databases to add the missing columns.

## 0.1.1
* **Docs**: Updated documentation to reflect cross-platform consistency.

* **Fix**: Resolved `kotlinx.serialization` crash by enforcing explicit `JsonObject` construction for Supabase uploads.
* **Refactor**: Switched to file-based UUID for Device ID to match Flutter implementation and remove system dependency.
* **Tests**: Added initial unit verification for `DeviceInfoProvider` using JUnit and MockK.

## 0.1.0
* **Initial Release**: Ported core functionality from `flutter-remote-logger` v0.3.0.
* **Persistent Device ID**: Uses `Settings.Secure.ANDROID_ID`.
* **Nested Remote Paths**: Added `remotePath` parameter to `initialize()` for organizing logs in custom folder structures.
* **Uploaders**: Included `FirebaseLogUploader` and `SupabaseLogUploader`.
