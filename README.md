# Google Photos Backup

Native Android app for importing a full Google Takeout export into an organized local photo/video archive. Google Takeout is used to export the full library; this app imports the resulting ZIP archives directly without requiring Google Photos API access.

## Features
- Clean native Android UI, custom photo/cloud app icon and splash screen
- Open Google Takeout in browser and import downloaded Takeout ZIP files
- Stream media from ZIP archives into `Pictures/GooglePhotosBackup/YYYY/MM` in app-specific external storage
- Supports common image/video formats; skips JSON sidecars and non-media files
- Progress, item counts, duplicate filename avoidance, and local archive location
- No Google password collection, no cloud upload, no automatic cloud deletion

## Build APK
Push to `main` or run **Actions → Build Android APK**. Download the `google-photos-backup-debug-apk` artifact from the workflow run.

## Build locally
Install JDK 17 and Android SDK, then run `gradle assembleDebug` (or use Android Studio). The Gradle wrapper is intentionally not committed; GitHub Actions installs Gradle.

## Full-library workflow
1. Tap **Open Google Takeout** and sign into the Google account in the browser.
2. Export Google Photos and download the Takeout ZIP archive(s).
3. In the app, tap **Import Takeout ZIP** and select each ZIP. Large exports may be split across multiple archives.
4. Wait for import completion before removing downloaded ZIP files.
5. Find imported media in the app-specific external Pictures folder. Android may remove app-specific files when the app is uninstalled, so copy/export them to a user-chosen folder before uninstalling.

Google Photos API policy does not permit a new app to read an entire existing library via the old broad Library API scopes. This app uses the user-controlled Takeout export route instead. The app does not delete anything from Google Photos or from Takeout ZIPs. Verify your local files before freeing space in Google Photos.
