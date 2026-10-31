# Nullx11Sudo

Minimalist Android Sudoku scan-and-solve companion. Package: `com.nullx11sudu.official`.

## How it works
1. Install the **debug APK** from the GitHub Actions **Android APK** workflow artifact (`Nullx11Sudo-debug-apk`). GitHub Actions → latest successful run → Artifacts. This is a debug build, not a signed Play Store release.
2. Open the app, grant **display over other apps**, and accept Android's screen-capture prompt. Notification permission is requested on Android 13+; it is optional, though allowing it makes the ongoing service easier to find.
3. Open a Sudoku app, tap the floating ✦ bubble, and drag the outlined square onto the 9×9 grid. Adjust its size with the slider. Tap **Scan**. The app recognizes printed digits on-device and solves the puzzle. White digits are detected clues; teal digits are the solution in empty cells. Tap **Hide** to return to the bubble. The persistent notification has a **Stop** action.

No internet permission, no server, no screenshots saved. The overlay never taps or modifies the other app. Screen capture requires explicit Android consent on each new service session. OCR can misread digits or grid placement; inspect the recognized white clues and rescan if incorrect. The solver rejects contradictory clues rather than presenting an incorrect answer. Recognition works best on clean, upright, high-contrast Sudoku grids; handwritten or tiny digits may fail.

## Build
Run `gradle testDebugUnitTest assembleDebug` with JDK 17, Android SDK 35, and Gradle 8.11.1. The CI workflow installs these and publishes `app/build/outputs/apk/debug/app-debug.apk` as an artifact on push, PR, or manual dispatch. The app uses Android Material Components with a dark Material-inspired design (there is no official “Material 15” design-system release).

## Design notes
The foreground service owns a user-visible floating bubble and a movable, resizable nine-by-nine grid. For each scan it hides its own overlay before taking one MediaProjection frame, runs on-device ML Kit Latin OCR, maps digit bounding-box centers to cells, and uses a minimum-remaining-values backtracking solver. No Accessibility service or hidden permissions are used. Android may restrict capture of protected/secure content.
