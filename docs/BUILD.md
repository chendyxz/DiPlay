# Building DashFlow

Requirements: JDK 25, Android SDK 37, NDK 28.2.13676358 and the included Gradle wrapper.

## Source and CI builds

```sh
./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :mobile:lintDebug :mobile:assembleDebug
```

The resulting source-only APK contains no accessory identity. Standalone CarPlay requires runtime authentication provisioning. Tests generate synthetic identities at runtime; no test private-key files are tracked.

## Local release packaging

Provide an external asset directory using `DIPLAY_AUTH_ASSETS_DIR`. For Android Studio Run, alternatively set `diplay.auth.assets.dir=/absolute/path/to/runtime-assets` in the ignored root `local.properties` file. The environment variable takes precedence. The directory must contain exactly the intended runtime files under `offline-mfi/identity.pk8` and `offline-mfi/certificate.p7b`. Neither file belongs in Git. The build permits those two files only when this explicit input is set and rejects unexpected credential containers elsewhere in APK assets.

Set `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` locally for your Android signing key. Never commit these values or the keystore. Different signing keys cannot update an existing project-signed installation.

```sh
./gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :mobile:lintRelease :mobile:assembleRelease
```

Output: `mobile/build/outputs/apk/release/mobile-release.apk`. The release APK deliberately contains the experimental identity described in the notices; it is extractable by recipients. The separate Android signing key is not included. The retired build-beta.py helper is not used; this Gradle workflow uses explicit environment inputs.

The public release source archive corresponds to the tagged source and excludes runtime identities, signing keys, local configuration and build output.

## Standalone car-test APK

Use `:mobile:assembleStandaloneDebug` for a test APK that must connect to an iPhone:

```sh
DIPLAY_AUTH_ASSETS_DIR=/absolute/path/to/runtime-assets ./gradlew :mobile:assembleStandaloneDebug
```

This task refuses missing or empty runtime inputs. `assembleDebug` remains an identity-free
source/CI build when neither explicit asset input is present; do not install that output as a
standalone car-test package. Before delivery, verify both `assets/offline-mfi/identity.pk8`
and `assets/offline-mfi/certificate.p7b` in the APK against the selected local inputs.
The standard debug package is `com.xyz.dashflow.hudtest`; standard release uses `com.xyz.dashflow`. Both install separately from the old DiPlay packages, without migrating their settings. Future updates with the same package and signing key can preserve settings. The separate KitKat build uses `cn.manstep.phonemirrorBox` in both debug and release.

## Android 4.4.3 / ARMv7 compatibility build

The separate KitKat build reuses the native home/settings and projection screens. The default
Android 9+ build and Automotive module keep their current requirements. Do not use `-Pkitkat=true`
for Automotive tasks.

Install NDK `23.2.8568313` in addition to the normal SDK. KitKat runtime tests use JDK 17;
Gradle itself still uses JDK 25. The compatibility dependencies support API 19, and core library
desugaring plus multidex provide the Java APIs used by the protocol implementation. KitKat lint
checks API availability and missing classes across all app modules; the standard build retains its
existing full lint checks.

```sh
./gradlew -Pkitkat=true :shared:testDebugUnitTest :common:testDebugUnitTest :mobile:lintDebug :mobile:assembleDebug
```

Output: `mobile/build/kitkat/outputs/apk/debug/mobile-debug.apk` (package
`cn.manstep.phonemirrorBox`, minimum API 19, `armeabi-v7a` only). Compatibility outputs use
`build/kitkat`. Copy APKs you need to retain before switching build modes: Gradle may remove
previous task outputs when their output directory changes. The KitKat debug build excludes the
Android 13 firmware-specific HUD test activity and receiver.

Launch the native home screen with:

```sh
adb -P 5038 shell am start -n cn.manstep.phonemirrorBox/cn.manstep.phonemirrorBox.MainActivity
```

Omit `-P 5038` if your ADB server uses its default port.

The KitKat manifest aliases the HSAE launcher's fixed AutoPlay `MainActivity` entry to DashFlow's
native home screen. The original CarPlay button therefore starts DashFlow directly; no forwarding
APK is needed. The original desktop icon and text remain unchanged. A debug build can update the
previous locally signed forwarding APK with `adb install -r`. Original AutoPlay has a different
signature and must be backed up and uninstalled before installing DashFlow under this package.
The previous `com.xyz.dashflow.hudtest` installation has separate settings; changing the package
does not migrate them automatically.

The KitKat HSAE HOME service preserves short presses and opens CarPlay when `cyttsp6_btn`
HOME is held for at least 800 ms and released. Enable `DashFlow HOME 长按` in Android
accessibility settings, keeping existing vehicle services enabled. It requests key filtering
without screen-content access. The service is already enabled on the tested unit; clean
installations need to enable it again. Modern builds do not include this service.

The tested unit's original AutoPlay APK is backed up at `artifacts/hsae-launcher/AutoPlay-original.apk`;
its private settings backup is at `.private/hsae-autoplay-backup/`. To restore the original app,
uninstall `cn.manstep.phonemirrorBox` and install the backed-up AutoPlay APK. Restoring private
settings additionally requires the original files and preferences to be assigned the restored app's UID.

For a standalone APK with local runtime authentication, use the same explicit asset input described
above and replace `:mobile:assembleDebug` with `:mobile:assembleStandaloneDebug`. Release builds
use `-Pkitkat=true :mobile:assembleRelease` with the existing release-signing environment variables;
the output is `mobile/build/kitkat/outputs/apk/release/mobile-release.apk`.

KitKat uses H.264, legacy audio/notification APIs and USB descriptor-based configuration selection.
For wireless use, enable the car's hotspot in system settings and save its SSID/password in
Connection setup. Wi-Fi Direct, automatic local-only hotspots and firmware-specific DiLink 5.1
features are unavailable on Android 4.4.3. HSAE NTDA3 firmware uses its AnwPhoneLink service for
paired-phone selection and RFCOMM because its standard Android Bluetooth adapter is unavailable.
The Bluetooth settings button opens the HSAE settings screen. On the tested HSAE Android 4.4.3
unit, RFCOMM, iAP2 authentication, hotspot handoff, AirPlay pairing, picture and touch have
completed. AAC audio decoding and audible output are also confirmed. The app records
its vendor socket and checks the phone address and service UUID before cleaning up a connection
left by an interrupted process. Disconnect before updating older APKs that do not record sockets.
Force-stop followed by launch has been tested on this unit: the recorded socket is disconnected
before opening a new one, and picture and touch resume without manual Bluetooth cleanup.
Actual USB re-enumeration and hardware decoding must also be checked on the target head unit.
