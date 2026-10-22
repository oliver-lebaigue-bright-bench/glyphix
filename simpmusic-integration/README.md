# Native SimpMusic integration

Experimental integration branch for Glyphix. This is a source integration, not an embedded website, external app launcher, or second APK. **Android compilation and device acceptance are not yet verified. Do not publish this as a working release until the build and acceptance checks pass.**

## Implemented wiring

- A Music destination in Glyphix's existing navigation, installed only by the integration build.
- SimpMusic's native Compose music experience, including its existing discovery, search, library, playlists, queue, lyrics and music settings routes. Upstream routes are reused, not reimplemented as mock data.
- Host colour scheme, typography and shapes passed into the music subtree. Direct calls to upstream typography also use the host typography.
- A shared, persistent native player with previous, play/pause and next controls outside the Music destination, backed by the same media handler as the music UI.
- An explicit built-in music visualisation source. Starting and stopping visualisation remains the user's choice and does not start or stop music playback.
- PCM taps in both standard and crossfade Media3 renderer factories, after per-track audio effects. PCM is forwarded without altering the playback buffer.
- A bounded latest-frame mailbox to the existing Glyphix FFT/rendering pipeline, keeping FFT/hardware work off the playback thread. It feeds the existing enabled Glyph, haptic, flashlight, overlay, latency compensation and PC PCM-stream outputs.
- Track metadata reaches the existing automatic preset engine without requiring notification access.
- Native music playback/download services, media buttons, Android Auto service declaration, sharing provider and supported link intent routes within the Glyphix package.
- Pinned upstream source commits, reproducible patches, PCM conversion tests and a separate Android integration build workflow.

## Build

Requirements: Git, Python 3.12+, JDK 21, Android SDK platform 37.0 and build tools 37.0.0. Keep Glyphix's existing `app/google-services.json` and `app/libs` dependencies. Set `ANDROID_HOME` or `sdk.dir` in the repository's `local.properties`.

```sh
python3 simpmusic-integration/prepare.py
bash tests/simpmusic/run.sh
mkdir -p keystore
keytool -genkeypair -keystore keystore/debug.keystore -storepass android \
  -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 \
  -validity 10000 -dname 'CN=Glyphix Development,O=Glyphix,C=IE'
bash .simpmusic-build/gradlew -p .simpmusic-build :glyphix:assembleDebug :glyphix:lintDebug
```

Create the key only if `keystore/debug.keystore` does not already exist. The APK is produced under `app/build/outputs/apk/debug/`. Normal Glyphix builds continue to use `app/build.gradle`; the integration uses `app/build-simpmusic.gradle` with the upstream toolchain and native libraries. No additional account or server is required to prepare the source. Individual upstream features can still require their normal accounts or remote services.

The preparer fetches the exact commits in `upstream.lock.json`, exports clean source snapshots and applies the checked-in patches. Existing developer checkouts can be supplied read-only with `--simpmusic-source PATH --core-source PATH`. It replaces only its own marked `.simpmusic-build` directory and never edits the source checkouts. The prepared libraries explicitly select Android 37.0 through AGP’s minor API DSL, matching Google’s published platform package. Generated code is ignored by Git. Patch updates are explicit; neither upstream's moving main branch is used implicitly.

## Validation and outstanding work

Local checks: source preparation with both pinned commits and patch application passed; seven PCM decoder checks passed. A local APK build initially could not reach Gradle directly. Retrying through the environment proxy resolved Gradle and the native module configuration, but the local Java 17 runtime lacks the compiler toolchain required by the integrated app. The build now explicitly selects JDK 21. Android compilation has not yet passed locally. The GitHub workflow is intended to validate the complete Android graph with JDK 21 and SDK 37.0; a successful workflow and device tests are still required.

This branch is not a claim of complete feature parity. In particular:

- Android compilation, manifest merging and runtime dependency compatibility have not yet passed verification.
- Real devices must verify notifications/background playback, search/playback, downloads, authentication, lyrics, navigation, theme switching, recreation and background Glyph output.
- The default integration uses upstream's FOSS modules. Google Cast, Last.fm credentials and crash reporting are not enabled. No upstream secrets are copied.
- SimpMusic's launcher widgets and Android-app-only automatic backup scheduler are not ported into Glyphix yet. Account migration/import and backup restore require explicit testing; no existing SimpMusic app data is silently imported.
- Crossfade taps observe each player after its gain stages; they are not a sample-accurate sum of both output streams. Hardware offload, remote casting and rendered-ahead audio timing require device validation. Existing latency compensation remains available.
- Smart Source's notification-based recording/cache orchestration is not rewritten to use the built-in playback clock in this change. Built-in music has its own direct PCM source; the existing Smart Source remains separately selectable.
- Music settings still contain upstream-specific settings and some upstream artwork-led layouts. A complete Glyphix-specific restyling of every secondary screen remains outstanding.

## Device acceptance

1. Install the integrated debug APK, verify Glyphix identity, and open Music. Discover a track, search, play an album and manage a playlist.
2. Start built-in music visualisation. Verify Glyphs, haptics, flashlight, overlays and optional PC streaming independently, including sample-rate changes and Bluetooth latency calibration.
3. Navigate to Audio/Settings and use the persistent player. Confirm music and visualisation controls remain independent.
4. Pause, seek, skip, stop, switch audio sources and rotate/recreate the activity. Check that stale PCM does not resume a stopped visualiser.
5. Test background/lock-screen playback, notifications, downloaded playback while offline, lyrics and authenticated features.
6. Review unsupported or upstream-specific settings, crossfade behaviour and licence notices before removing experimental status.

## Source and licensing

- Glyphix: Oliver Lebaigue and contributors, original MIT notice retained at `../LICENSE`.
- SimpMusic: maxrave-dev and contributors, pinned source `d5842c65cddbe7522553a1d1ff1fabf52d3be7bc`, original GPLv3 text at `licenses/SimpMusic-GPL-3.0.txt`.
- SimpMusic core: maxrave-dev and contributors, pinned source `2894e549763b8a68225b4456d3290f3227bdbb01`, original licence at `licenses/Core-LICENSE.txt`.

Integration-specific source and upstream patches are provided under GPL-3.0-only. The integrated distribution includes GPL-covered code and must carry its corresponding source, licence text, original notices and modifications. The original Glyphix files retain their MIT grant. This branch does not represent itself as an official SimpMusic build. The build includes licence texts as Android assets and adds native music credits with an expandable GPL text to Glyphix’s existing licence screen.
