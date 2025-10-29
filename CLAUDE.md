# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Flutter plugin that wraps the Brother SDK for printing. It supports 87+ Brother printer models across label printers (PT/QL series), mobile printers (RJ/PJ series), and desktop printers (TD series). The plugin provides both standard image/PDF printing and TypeB direct command printing.

**Platform Support:**
- Android: Bluetooth/BLE, WiFi, USB (minSdkVersion 19)
- iOS: Bluetooth/BLE, WiFi (iOS 13.0+, requires MFi PPID for App Store)

## Development Commands

### Testing
```bash
# Run plugin tests (limited coverage)
flutter test

# Run example app (requires physical Brother printer)
cd example
flutter run
```

### Building
```bash
# Analyze code
flutter analyze

# Format code
dart format .

# Get dependencies
flutter pub get

# Build Android AAR
cd android && ./gradlew build

# Validate iOS podspec
pod lib lint another_brother.podspec
```

### Android Specific
```bash
# Build Android example APK
cd example/android
./gradlew assembleDebug

# Check native library alignment (for Google Play 16KB requirement)
unzip -p build/app/outputs/bundle/release/app-release.aab base/lib/arm64-v8a/libanother_brother.so | hexdump -C | head -1
```

### iOS Specific
```bash
# Update CocoaPods
cd example/ios && pod install

# Build iOS example
cd example && flutter build ios
```

## Architecture

### Platform Channel Communication

**Single MethodChannel:** `another_brother`

All communication between Dart and native code flows through one channel with 58+ method names:
- Standard printing: `printImage`, `printFile`, `printPdfFile`, `printFileList`
- Printer control: `startCommunication`, `endCommunication`, `getPrinterStatus`, `cancel`
- Discovery: `getNetPrinters`, `getBlePrinters`, `getBluetoothPrinters`
- Settings: `getPrinterSettings`, `updatePrinterSettings`, `getBluetoothPreference`
- TypeB commands: `typeB-startCommunication`, `typeB-setup`, `typeB-printLabel`, etc.

### Native Implementation Pattern

Each method call is implemented as a separate class:

**Android (Kotlin):**
```kotlin
class PrintImageMethodCall(
    val call: MethodCall,
    val result: MethodChannel.Result
) {
    companion object { const val METHOD_NAME = "printImage" }
    fun execute() { /* implementation with coroutines */ }
}
```

**iOS (Objective-C):**
Similar pattern with individual method classes in `ios/Classes/Method/`

### Brother SDK Versions

**CRITICAL:** SDK 4.13.0 has a `WifiConnection.closeConnection()` crash bug. Always use 4.12.0.

**Current versions:**
- Android: `BrotherPrintLibrary-4.12.0.aar` (local AAR in `android/libs/`)
- iOS: `BRLMPrinterKit ~> 4.12.0` (CocoaPods)
- TypeB SDK: `com.brother.typeb:print:1.0.0`

**Upgrading SDKs:**
1. Replace AAR file in `android/libs/`
2. Update version in `android/build.gradle`
3. Update iOS version in `ios/another_brother.podspec`
4. Test thoroughly - especially WiFi disconnect scenarios
5. Document in CHANGELOG.md with SDK version

### Crash Prevention Architecture

**5-second timeout protection** (added after SIGABRT crash issues):
- Native Android: 1.5s timeout with coroutine cancellation
- Dart layer: 3s timeout (backup)
- AtomicBoolean prevents double responses on closed channels
- Critical for apps with periodic status polling

### Core Dart Classes

**`lib/printer_info.dart` (3511 lines)** - The heart of the plugin:
- `Printer` class - Main printing API
- `PrinterInfo` class - Complete printer configuration
- `Model` enum - 87+ printer models
- `PrinterStatus` class - Operation results with 40+ error codes
- All printing enums (Port, Orientation, PrintMode, Halftone, etc.)

**`lib/type_b_printer.dart`** - TypeB printer API:
- `TbPrinter` class - Buffer-based TSPL printing
- Command builder pattern for TD series printers

**`lib/label_info.dart`** - Label definitions:
- `PT`, `PT3` classes - P-touch label sizes
- `QL700`, `QL1100`, `QL1115` - QL series labels
- `LabelInfo`, `LabelColor` - 45+ colors

**`lib/custom_paper.dart`** - Custom paper (RJ/TD series):
- 16 printer-specific classes (`BinPaper_RJ2050`, `BinPaper_TD2350D`, etc.)
- References `.bin` files in `custom_paper/` directory (180+ files)

### Two Printing Workflows

**1. Standard Printing (QL/PT/RJ/PJ series):**
```dart
var printer = Printer();
var info = PrinterInfo();
info.printerModel = Model.QL_1100;
info.port = Port.NET;
info.ipAddress = "192.168.1.100";
await printer.setPrinterInfo(info);
var status = await printer.printImage(myImage);
```

**2. TypeB Printing (TD series):**
```dart
var printer = TbPrinter();
await printer.startCommunication();
await printer.setup(width: 50, height: 50);
await printer.barcode("123456", type: BarcodeType.Code128);
await printer.printerFont("Hello", x: 10, y: 10);
await printer.printLabel(count: 1);
await printer.endCommunication();
```

### Session Management

Two modes supported:
1. **One-time:** Print methods auto-connect and disconnect
2. **Persistent:** Call `startCommunication()`, print multiple times, then `endCommunication()`

Persistent sessions improve performance for batch printing but require manual cleanup.

## Platform-Specific Notes

### Android

**Build Configuration:**
- Kotlin 1.8.22, Gradle 8.5.0
- compileSdkVersion 34, minSdkVersion 19
- JVM target 17
- Coroutines for async operations

**USB Printing:**
Android-only feature. Requires permission handling via BroadcastReceiver in `BrotherManager.kt`.

**PDF Printing:**
Android 5.0+ only (uses `android.graphics.pdf.PdfRenderer`).

### iOS

**Required Info.plist entries:**
- `NSBluetoothAlwaysUsageDescription`
- `NSLocalNetworkUsageDescription`
- `NSBonjourServices` array: `_ipp._tcp`, `_printer._tcp`, `_pdl-datastream._tcp`
- `UISupportedExternalAccessoryProtocols`: `com.brother.ptcbp`, `com.issc.datapath`

**App Store Requirements:**
- Must obtain PPID from Brother: https://secure6.brother.co.jp/mfi/Top.aspx
- Without PPID: "App has not been authorized by the accessory manufacturer"
- Remove `com.issc.datapath` for TypeB printers to avoid rejection

**Xcode Setup:**
- Mark "Allow non-modular includes" as YES in Runner
- Ensure `libBROTHERSDK.a` belongs to another_brother in Xcode

### 16KB Page Alignment (Android 15+)

**Google Play requirement** (deadline: Nov 1, 2025):
Brother SDK native libraries must be 16KB-aligned for Android 15+ devices.

**Current status:** SDK 4.12.0 uses 4KB alignment (Brother has not released 16KB update yet).

**Check alignment:**
```bash
unzip -p your-app.aab base/lib/arm64-v8a/libbrotherprinter.so | hexdump -C | head -1
# 00004000 = 4KB aligned, 00010000 = 16KB aligned
```

**Workaround options:**
1. Wait for Brother SDK 4.14.0+ with 16KB support
2. Use `-Wl,-z,max-page-size=16384` linker flag (limited success)
3. Recompile Brother SDK from source (not recommended - closed source)

See `REPORT_16KB_ANALYSIS.md` for detailed analysis.

## Code Organization

### Method Call Classes (Android)

All in `android/src/main/kotlin/com/rouninlabs/another_brother/method/`:
- `PrintImageMethodCall.kt` - Image printing
- `StartCommunicationMethodCall.kt` - Open connection
- `GetNetPrintersMethodCall.kt` - Network discovery
- `PrintPdfFileMethodCall.kt` - PDF printing
- 50+ other method implementations

TypeB methods in `method/typeb/` subdirectory.

### Plugin Entry Points

**Android:** `AnotherBrotherPlugin.kt`
- Registers method channel
- Routes method calls to appropriate handler classes
- Manages FlutterPluginBinding

**iOS:** `AnotherBrotherPlugin.m`
- Objective-C implementation
- Similar routing to Android but fewer methods (no USB/PDF)

### Printer Instance Management

**`BrotherManager`** (Android/iOS):
- Singleton that tracks active printer instances by ID
- Manages USB permission handling (Android)
- Prevents resource leaks from abandoned connections

## Key Considerations

### Error Handling

`PrinterStatus.errorCode` returns one of 40+ error types:
- `ERROR_NONE` - Success
- `ERROR_WRONG_LABEL` - Wrong label installed
- `ERROR_COVER_OPEN` - Printer cover open
- `ERROR_HIGH_VOLTAGE_ADAPTER` - Wrong power adapter
- And 35+ more...

Always check `errorCode` after print operations.

### Timeout Handling

All print methods accept optional `Duration timeout`:
```dart
var status = await printer.printImage(image, timeout: Duration(seconds: 10));
```

Default: 3 seconds (with native 1.5s timeout underneath).

### Asset Management

Custom paper `.bin` files are Flutter assets declared in `pubspec.yaml`:
```yaml
assets:
  - custom_paper/CustomRJ2050Paper/
  - custom_paper/CustomTD2350DPaper/
  # 16 printer types total
```

Reference via asset paths in Dart code.

### Bluetooth Discovery

Uses `flutter_blue_plus: ^1.12.13` for BLE scanning. Bluetooth Classic discovery uses native APIs.

## Common Issues

### "SIGABRT crash during status check"
- **Cause:** Brother SDK 4.13.0 WiFi bug or missing timeout
- **Fix:** Use SDK 4.12.0 + ensure timeouts enabled (already implemented)

### "Printer not found via network"
- **Cause:** Missing Info.plist permissions (iOS) or wrong network
- **Fix:** Add NSLocalNetworkUsageDescription + NSBonjourServices

### "USB printer not detected" (Android)
- **Cause:** Missing USB permission
- **Fix:** BroadcastReceiver in BrotherManager handles this automatically

### "App Store rejection for MFi accessory"
- **Cause:** Missing PPID from Brother
- **Fix:** Request PPID at https://secure6.brother.co.jp/mfi/Top.aspx

### "Wrong label installed error"
- **Cause:** PrinterInfo.labelSize doesn't match physical label
- **Fix:** Call `printer.getLabelInfo()` first to detect installed label

## Testing Notes

**Physical hardware required** - No printer emulators available.

**Test coverage gaps:**
- No unit tests for native code
- No mock printer for CI/CD
- Example app serves as integration test

**Testing checklist for SDK upgrades:**
1. Test all connection types (BT/BLE/NET/USB)
2. Verify WiFi disconnect doesn't crash
3. Test status polling every 30s for 5 minutes
4. Print 50+ labels in succession
5. Test all supported printer models if possible

## Publishing

**Pub.dev:** Standard Flutter plugin publishing (`flutter pub publish`)

**Version bumping:**
1. Update `version` in `pubspec.yaml`
2. Add entry to `CHANGELOG.md` with SDK versions
3. Tag git commit: `git tag v2.2.4`

## Resources

- Brother SDK documentation: `bpsdkaall4130/` directory
- Demo app: https://github.com/CodeMinion/Demo-Another-Brother-Prime
- iOS setup video: https://www.youtube.com/watch?v=AcFnd-6hSew
- Brother MFi PPID request: https://secure6.brother.co.jp/mfi/Top.aspx
- Brother Hackathon: https://brotherhackathon.com
