# Smart Source

Smart Source connects the existing Smart Capture prototype to the Android service and both capture menus. It uses the existing PocketBase endpoint at `https://db.glyphix.site`, not Firebase.

## Behaviour

1. Select **Smart Source** from the waveform menu. The audio page displays its controls.
2. Grant notification-listener access using **Allow song identification**. Android exposes active media sessions through this access; no root, usage-access permission or private Spotify endpoint is required.
3. The service reads title, artist, duration and playback position. It selects a playing session ahead of paused sessions and polls every 250 ms even when the activity is closed.
4. It checks a four-song memory/disk cache, then PocketBase. Complete matching sequences play through the current Glyph renderer, with seeks, pauses and resumes anchored to Android's monotonic media-session clock. Android Visualizer sampling is disabled during database playback.
5. A miss falls back to Android Visualizer. **Allow live visualiser fallback** grants its normal audio permission. No screen-capture dialog is opened by Smart Source. Some devices/players restrict Visualizer; in that case a missing song cannot be learned. Microphone recordings are never submitted.
6. Sharing is **off by default**. If enabled, numeric frequency-range magnitudes are sampled at at most approximately 30 Hz, with timestamps and a hashed identifier. Neither PCM, playable audio, artwork, account IDs nor raw artist/title text is uploaded. Existing screen-capture and microphone sources retain their current behaviour and do not contribute.

A SHA-256 `v2_` identifier includes Unicode-normalised artist/title, exact duration and the device/preset/frequency-range layout. This avoids accidental cross-song substring matches and incompatible LED layouts. Exact duration deliberately favours false misses over playing the wrong edition. Legacy records with unsafe artist/title keys are not reused. These identifiers can still reveal listening choices through dictionary matching, so they are not an anonymity guarantee.

Recordings must begin within 500 ms of the start, reach within 500 ms of the end and contain no gap greater than 500 ms. Partial recordings, backwards seeks, large forward seeks, invalid magnitudes and inconsistent frame widths are rejected. Live recording and cached replay support normal (1x) playback; other speeds use live visualisation without recording. Unknown metadata falls back to live visualisation. Local storage and downloads are bounded; songs longer than 30 minutes are not shared. Download/upload failures leave live visualisation available. Unmetered-only networking is enabled by default; cached songs work offline. Failed uploads are not queued for later retry.

## PocketBase contract and deployment boundary

The app uses the existing `lightshows` collection:

| Field | Required value |
| --- | --- |
| `songKey` | Text, `v2_` followed by 64 lowercase hex characters; unique index recommended |
| `durationMs` | Positive number, at most 1,800,000 |
| `sequenceData` | JSON frame array, or text containing that array (the existing client upload contract uses text) |

A frame is `{"timestampMs": 33, "intensities": [0.12, 0.8]}`. These are pre-renderer range magnitudes, not final LED brightness. Client validation limits sequences to 60,000 frames, 1–512 values per frame, strictly increasing timestamps, finite nonnegative values and complete coverage. HTTP response bodies are limited to 8 MB. Uploads are size-limited and skip an existing valid record. Concurrent contributions need a server unique index on `songKey` to avoid duplicates.

No server configuration or production data is changed by this PR. The deployed endpoint could not be reached from the development environment, so its collection schema and access rules remain unverified. Before release, the operator must verify the fields above and that the intended read/contribution requests are allowed. Do not put PocketBase admin credentials into the app. Public contribution endpoints need server-side validation and rate limiting; client validation alone cannot prevent poisoned submissions. If writes require user authentication, wire the operator's chosen public authentication flow before enabling shared contributions in production. Record creation returning an error is logged and falls back safely; it is never presented as a successful upload.

## Validation

Run the framework-independent regression suite with JDK 17:

```sh
mkdir -p /tmp/glyphix-smart-test
java com.sun.tools.javac.Main -d /tmp/glyphix-smart-test app/src/main/java/com/glyphix/app/logic/smartcapture/SmartSourceRules.java tests/smart-source/SmartSourceRulesTest.java
java -cp /tmp/glyphix-smart-test SmartSourceRulesTest
```

The suite covers Unicode and locale-stable song identity, edition/layout separation, invalid metadata, partial-song and gap rejection, frame search after seeks, and short/exact/oversized HTTP bodies. A PR workflow runs it automatically.

Android compilation: `bash gradlew :app:compileDebugKotlin :app:compileDebugJavaWithJavac`. This environment could not download the configured Gradle 9.6.1 distribution, so Android compilation and device testing have not been completed.

On a supported Nothing phone, verify: missing song with sharing disabled; full song with sharing enabled; same song on a second phone with matching device/preset; airplane-mode cached playback; pause/resume; forward/backwards seek; changing track, preset and source during a slow lookup; notification/audio permission denial and revocation; stop/restart; metered connection; malformed server data; and server rejection of uploads. Haptics/flashlight and full-spectrum overlays are not reconstructed from database range magnitudes; cached playback drives Glyphs only.
