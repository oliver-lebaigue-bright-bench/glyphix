# Glyphix TV

A standalone Android TV edition of Glyphix. Its package is `com.glyphix.tv`; it installs alongside the existing `com.glyphix.app` phone edition. The phone source, configuration, permissions and terms are unchanged.

Requirements: Android TV 8.0 (API 26) or later, a D-pad remote, and a TV/USB microphone for live microphone input. Remote voice-search microphones are not guaranteed to be available to third-party apps. The app also supports Android playback capture where the TV and source app permit it; protected content and apps that prohibit capture cannot be analysed.

The startup warning uses bundled offline narration in the Daniel, English (UK), voice. ElevenLabs was not connected for this build. Continue becomes available only after the recording finishes. TV-only terms and every pre-start confirmation have a visible three-second delay. Read the warnings, start with low intensity, keep the remote nearby, and press Back to stop the effects.

White pulses, colour washes, moving bars and rings react to live audio. Pulse frequency is limited to at most two pulses per second and brightness starts low. These limits do not guarantee safety: full-screen light, contrast, colours and patterns can still trigger symptoms. Do not use around anyone sensitive to flashing lights; stop immediately if anyone feels unwell. Warning guidance draws on the [Epilepsy Society](https://epilepsysociety.org.uk/understanding-epilepsy/photosensitive-epilepsy) and [Epilepsy Action](https://www.epilepsy.org.uk/info/seizure-triggers/photosensitive-epilepsy).

Microphone and permitted playback audio are analysed locally in memory. Audio is not saved or transmitted; this TV app does not request Internet access. Only effect preferences are stored locally, and cloud backup/device transfer are disabled. Microphone capture runs only while the visualizer is active in the foreground. Android playback capture requires a fresh system consent and stops when the visualizer stops or the app leaves the foreground.

## Build

Install JDK 17 or later and an Android SDK with `platforms;android-36` and `build-tools;35.0.0`. Set `JAVA_HOME` and `ANDROID_HOME`, then run from the repository root:

```sh
bash scripts/build-tv.sh
```

The script downloads pinned Gradle 8.13 and verifies its SHA-256 checksum, then builds and lints only `tvApp`. It never configures the mobile modules. The APK is `tvApp/build/outputs/apk/debug/GlyphixTV-debug.apk`. This is a signed debug/beta APK for sideloading, not a Play Store release. Keep `keystore-tv/debug.keystore` locally for update compatibility; it is excluded from Git.

```sh
adb install -r tvApp/build/outputs/apk/debug/GlyphixTV-debug.apk
adb shell am start -n com.glyphix.tv/.TvActivity
```

The Android TV workflow also publishes the APK as a GitHub Actions artifact on the TV branch.
