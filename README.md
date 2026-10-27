# QR Studio

An offline QR design editor built with Kotlin Multiplatform and Compose Multiplatform. The active project lives in this directory. The earlier Android implementation remains in `app/` for reference and is excluded from `settings.gradle.kts`.

## Modules

- `composeApp`: shared UI, immutable editor state, shared `EditorViewModel`, payload generation, pure Kotlin QR encoding, layer geometry, canvas rendering and PNG export. `commonMain` owns the entire editor interface.
- `androidApp`: Android application entry point and packaging. Platform adapters use Android Photo Picker, MediaStore, FileProvider, DataStore, haptics and saved-instance-state restoration.
- `iosApp`: SwiftUI host for the shared Compose controller. iOS adapters use PHPicker, Photos, UIActivityViewController, DataStore and native haptics. The Xcode scheme is `QRStudio`; the host Swift module is `QRStudioHost` so it can import the `QRStudio` Kotlin framework.

AGP 9 uses a dedicated Android KMP library module plus a small Android application module. Android requires API 26 or later and targets API 35. Compile SDK is 37. Use JDK 17 and the SDK path in `local.properties`. Pinned dependency versions are in `gradle/libs.versions.toml`.

## Editor

One screen contains Content, QR style, Background, Gradient and Text tools. URL, free text, Wi-Fi and phone payloads support four error-correction levels. Designs combine custom QR module and finder styles, a QR plate, solid or selected photo backgrounds, gradient overlays and independent draggable text layers. Alignment guides and optional center snapping help position layers. Theme, content and design settings persist locally; selected photos are copied to app-private cache.

The shared renderer draws both the preview and exported PNG. Selection outlines and snap guides are excluded from exports. Save writes a PNG to Photos/Gallery; Share opens the native share sheet. Android API 26–28 requests storage permission only for saving. Android API 29+ uses MediaStore without broad storage permission.

When the software keyboard opens, the preview animates to a compact size, the layer hint and bottom tabs give way to an editing row with Done, and the tool panel keeps the focused input in view. Done or the keyboard dismissal restores the normal layout. Single-line inputs also accept the IME Done action. The new editing labels have English, Russian, Spanish, Hindi, Arabic and Turkish resources; the remaining shared editor labels currently follow the English redesign brief.

## Build and run

```sh
./gradlew :androidApp:assembleDebug
./gradlew :composeApp:testAndroidHostTest
./gradlew :androidApp:lintDebug :androidApp:bundleRelease
```

The debug APK is `androidApp/build/outputs/apk/debug/androidApp-debug.apk`. Release signing must be configured with the publisher's key before Play upload; no credentials are checked in. A startup baseline profile is bundled in `androidApp/src/main/baseline-prof.txt` and ProfileInstaller is included.

To run instrumentation tests, select an isolated emulator explicitly, especially when a physical device is attached:

```sh
ANDROID_SERIAL=emulator-5554 ./gradlew :androidApp:connectedDebugAndroidTest -Pandroid.injected.device.serial=emulator-5554
```

The UI tests cover Wi-Fi entry and MediaStore saving, byte/Unicode QR decoding for all correction levels, and keyboard editing/navigation restoration. Use an API 35 or 36 emulator for the pinned Espresso runtime; API 37 currently removes an input API used by that runtime. The app itself can be manually exercised on API 37.

For iOS, open `iosApp/QRStudio.xcodeproj`, choose `QRStudio`, and select an Apple Silicon simulator or an iOS device. Xcode's build phase builds and embeds the shared Kotlin framework. Both Kotlin native targets are configured (`iosArm64`, `iosSimulatorArm64`). The project restricts architectures to arm64. Device builds require a developer signing team.

```sh
xcodebuild -project iosApp/QRStudio.xcodeproj -scheme QRStudio -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

## Data and third-party assets

No account, network permission, advertising, analytics or billing integration is included in the active Android application. Drafts may contain sensitive QR content such as Wi-Fi passwords. Clearing app data or uninstalling removes app-private settings and cached images; exported images remain in the user's photo library. Cached background images can be evicted by the operating system, in which case the user can choose the photo again.

Archivo fonts include their OFL license. Lucide icon geometry includes its license in `third_party/`. The QR encoder uses `qrcode-kotlin`; generation and design rendering run locally. Advanced visual styling can reduce scanning reliability, so test exported designs with the intended scanners, particularly with low contrast, small modules or overlaid content.
