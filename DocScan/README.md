# DocScan

Android document scanner: scan with the camera, JPG to PDF, PDF to JPG, merge, split, compress, text extraction (OCR) and one-tap sharing.

## Build the APK on GitHub (no Android Studio needed)
1. Create a new GitHub repository.
2. Upload everything from this folder (including the hidden `.github` folder) to the `main` branch.
3. Open the **Actions** tab, then **Build APK**. It starts on every push; you can also press **Run workflow**.
4. When the run finishes, open it and download **DocScan-debug-apk** from *Artifacts*. Unzip it and install `app-debug.apk` on your phone.

## Build in Android Studio
Open this folder in Android Studio, let Gradle sync, then Run.

## Notes
- Minimum Android version: 10 (API 29).
- The scanner screen (camera, auto edge detection, crop, filters) is provided by Google Play services.
- Compress and Merge/Split use the PdfBox library. Compress turns pages into images, so text is no longer selectable.
- Text extraction supports Latin-script text.
