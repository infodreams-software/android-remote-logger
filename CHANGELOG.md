## 0.1.1
* **Docs**: Updated documentation to reflect cross-platform consistency.

* **Refactor**: Switched to file-based UUID for Device ID to match Flutter implementation and remove system dependency.
* **Tests**: Added initial unit verification for `DeviceInfoProvider` using JUnit and MockK.

## 0.1.0
* **Initial Release**: Ported core functionality from `flutter-remote-logger` v0.3.0.
* **Persistent Device ID**: Uses `Settings.Secure.ANDROID_ID`.
* **Nested Remote Paths**: Added `remotePath` parameter to `initialize()` for organizing logs in custom folder structures.
* **Uploaders**: Included `FirebaseLogUploader` and `SupabaseLogUploader`.
