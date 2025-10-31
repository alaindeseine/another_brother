## 0.0.1+upgrade-sdk-4.13.0-16kb
- **16KB ALIGNMENT FIX**: Upgrade Brother SDK 4.12.0 → 4.13.0 for Android 15+ compliance
  - Android: BrotherPrintLibrary-4.13.0.aar (local, with 16KB page alignment)
  - iOS: BRLMPrinterKit ~> 4.13.0
  - Resolves Google Play Store rejection for 16KB page size requirement
  - Native libraries properly aligned: libcreatedata.so @ 16KB (0x4000)
- **ANDROID BUILD TOOLS**: Update Android Gradle Plugin 8.5.0 → 8.5.2
  - Improved NDK support and build stability
  - Bug fixes and performance improvements
- **BLUETOOTH IMPROVEMENTS**: Update flutter_blue_plus 1.12.13 → 1.35.10
  - Better Android 14+ compatibility
  - Numerous BLE stability fixes
  - Improved scanning and connection reliability
- **SDK SOURCE**: Switch from Maven to local AAR for better control
  - Removed dependency on RounInLabs Maven repository
  - Local SDK ensures consistent 16KB alignment
- **TYPEB DEPENDENCY CLEANUP**: Removed obsolete TypeB dependency
  - Removed 'com.brother.typeb:print:1.0.0' (unused, Maven-only)
  - TypeB functionality included in main SDK
  - Simplifies dependency management
- **GRADLE BUILD FIX**: Use flatDir repository for local AAR
  - Changed from 'files()' to flatDir repository approach
  - Fixes "Direct local .aar file dependencies are not supported when building an AAR"
  - Allows plugin to be built as AAR for host applications
- **CRASH FIX**: Add defensive error handling for WiFi socket NPE (Issue HACCP-SYNC-1S)
  - Fixed NullPointerException in WifiConnection.java:183
  - Added try-catch in GetPrinterStatusMethodCall.kt around startCommunication()
  - NPE returns ERROR_COMMUNICATION_ERROR instead of crashing
  - Improves reliability when WiFi socket is null
- **CRASH FIX**: Comprehensive defensive error handling for Bluetooth/WiFi connection NPE
  - Fixed NullPointerException in BluetoothConnection.getPrinterDevice() and getStream()
  - Added try-catch around startCommunication() in 36 method call handlers
  - Protected all Print*, Get*, Update*, Send*, Replace*, and Transfer operations
  - Prevents crashes when Bluetooth device Set or WiFi Socket is null
  - Returns false/error gracefully instead of crashing the app
  - Complete list of protected methods:
    - Print: PrintImageMethodCall, PrintPdfMethodCall, PrintPdfFileMethodCall, PrintFileMethodCall, PrintFileListMethodCall, StartPttPrintMethodCall, FlushPttPrintMethodCall
    - Get: All 16 Get* methods (GetPrinterStatus, GetSerialNumber, GetPrinterSettings, GetSystemReport, GetTemplateList, GetPdfPages, GetPdfFilePages, GetMediaVersion, GetMediaFileVersion, GetLabelParam, GetLabelInfo, GetFirmVersion, GetFirmFileVersion, GetBootMode, GetBluetoothPreference, GetBatteryWeak, GetBatteryInfo)
    - Update: UpdatePrinterSettings, UpdateFirm, UpdateBluetoothPreference
    - Send/Replace/Other: SendDatabase, SendBinary, SendBinaryFile, ReplaceText, ReplaceTextName, ReplaceTextIndex, RemoveTemplate, Transfer, Cancel
  - Also protects StartCommunicationMethodCall for explicit communication management
- **COMPATIBILITY**: Maintains full TD-2350D support
- **DEADLINES**: Compliant with Google Play Store requirements
  - November 1, 2025: New submissions deadline
  - May 1, 2026: All updates deadline

## 0.0.1
- Initial release with support for both Android and iOS.
- Includes support for paper bin files.
- Includes support fort TypeB printers as well.

## 0.0.7
- Updates NetPrint to use common Printer interface.

## 0.0.8
- Exposes getName from papers.

## 0.0.9
- Fixes name for QL1100's W102 paper.

## 0.0.10
- Exposes getValues for additional papers.
- Paper now return an immutable list from getValues()

## 0.0.11
- Fixes dimension calculation for Type B download Image.
- Handles missing BT address when trying to connect over BT on Type B.

## 0.0.12
- Added valueFromName to Bin papers.
- Added UNSUPPORTED to Bin papers.

## 0.0.13
- Updated the names for the RJ-4250 bin papers.

## 0.0.14
- Additional Updates for the names of the RJ-4250 bin papers.

## 0.0.15
- Discovered TypeB printers now share the common interface ABrotherTbPrinter.

## 0.0.16
- Adds support for downloadImage in iOS. This allows for printing of custom images on a TypeB printer.

## 0.0.17
- RJ-Go models now share model interface with label printers.
- RJ-Go Labels now share label interface with label printers.

## 0.0.18
- Exposes getName in common printer model interface.

## 0.0.19
- Updates TypeB unsupported label name for parity.

## 0.0.20
- Adds missing toMap/fromMap for Type B models and Type B labels.

## 0.0.21
- Fixes toMap on TbModel.

## 0.0.22
- PaperSize now shares common interface as other labels.

## 0.0.23
- Fixes issue with startPTTPrint. Special thanks to rendesg for the fixes.

## 0.0.24
- Fixes exception on iOS print. Special thanks to BentEngbers for the report.

## 0.0.25
- Moved from JCenter to MavenCentral for Android dependencies.

## 0.0.26
- Added Bluetooth permissions required by Android 12

## 0.0.27
- Replaces flutter_blue with flutter_blu_plus

## 0.0.28
- Resolves issue where app UI freezes while calling printImage on iOS.

## 0.0.29
- Upgrades Android libs to v4.6.1 of the Brother SDK.

## 0.0.30
- Adds support for getPDFFilePages on iOS.

## 0.0.31
- Adds orientation support for PT printers on iOS.

## 0.0.32
- Updates Bluetooth printer filter on Android.

## 0.0.33
- Updates USB permissions check for Android 13.

## 0.0.34
- Adds support for: RJ_3230B, RJ_3250WB, PT_D410, PT_D460BT, PT_D610BT, PJ_822, PJ_823, PJ_862, PJ_863, PJ_883, TD_2030A, TD_2125N, TD_2125NWB, TD_2135N, TD_2135NWB.
- Adds bin papers for TD_2135NWB. Special thanks to Alain Deseine for the support with this.

## 1.0.0
- Updates iOS SDK to 4.6.4
- Updates flutter_blue_plus to 1.12.13

## 1.0.1
- Adds bin papers for RJ-3230B and RJ-3250WB. Special thanks to Linus Lee for the support with this.

## 1.0.2
- Adds more bin papers for TD-2135NWB. Special thanks to Alain Deseine for the support with this.

## 1.0.3
- Fixes issue preventing printing on iOS using the TD-2125NWB and TD-2125NWB printer. 

## 2.0.0
- Updates AGP to version 7.3.0. 

## 2.1.0
- Adds bin papers for RJ-4030. Special thanks to Linus Lee for the support with this.

## 2.1.1
- Adds support for handling the case where the device name of a Bluetooth printer is null.

## 2.1.2
- Removes JFrog as a provided of Android Brother native libraries. 

## 2.1.3
- Adds support for the RJ-4030Ai and RJ-3050Ai printers. Special thanks to Linus Lee for the report.

## 2.1.4
- Fixed support for RJ-3230B and RJ-3250WB printers. Special thanks to Linus Lee for the report.

## 2.1.5
- Adds iOS support for PT-D410, PT-D460BT, PJ-822, PJ-823, PJ-862, PJ-863, PJ-883, TD-2030A, TD-2135N, TD-2135NWB printers. Special thanks to Linus Lee for the report.

## 2.1.6
- Links custom papers for RJ-4030. Special thanks to Linus Lee for the report.

## 2.1.7
- Adds custom papers for TD-2135NWB and TD-2125N. Special thanks to Linus Lee and Primael Quemerais for the papers.

## 2.1.8
- Fixes issue with the TD-2125N bin paper. Special thanks to Primael Quemerais for the support.

## 2.2.0
- Updates min Java SDK for Android.

## 2.2.1
- Updates AGP and path_provider version.

## 2.2.2
- Removes deprecated Registrar in Android. Special thanks to Mickey Cheong for the change.

## 2.2.3
- Addresses old analysis warning.