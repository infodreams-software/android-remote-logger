## 0.1.0
* **Initial Release**: Ported core functionality from `flutter-remote-logger` v0.3.0.
* **Persistent Device ID**: Uses `Settings.Secure.ANDROID_ID`.
* **Nested Remote Paths**: Added `remotePath` parameter to `initialize()` for organizing logs in custom folder structures.
* **Uploaders**: Included `FirebaseLogUploader` and `SupabaseLogUploader`.
