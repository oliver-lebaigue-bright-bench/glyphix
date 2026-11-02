#!/usr/bin/env bash
set -euo pipefail

# Independent TV build: no phone modules, root Gradle plugins or phone signer.
tv_root="$(cd "$(dirname "$0")/.." && pwd)"
tv_gradle_version=8.13
tv_gradle_sha=20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78
tv_tools="$tv_root/.gradle/tv-toolchain"
tv_gradle="${TV_GRADLE_HOME:-$tv_tools/gradle-$tv_gradle_version}/bin/gradle"

if [[ -z "${JAVA_HOME:-}" ]]; then
    echo 'Set JAVA_HOME to a JDK 17 or later installation.' >&2
    exit 1
fi
if [[ -z "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]]; then
    echo 'Set ANDROID_HOME to an Android SDK with platforms;android-36 and build-tools;35.0.0 installed.' >&2
    exit 1
fi
export ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"

if [[ ! -x "$tv_gradle" ]]; then
    mkdir -p "$tv_tools"
    tv_archive="$tv_tools/gradle-$tv_gradle_version-bin.zip"
    curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$tv_gradle_version-bin.zip" -o "$tv_archive"
    tv_actual_sha="$(shasum -a 256 "$tv_archive" | awk '{print $1}')"
    if [[ "$tv_actual_sha" != "$tv_gradle_sha" ]]; then
        echo 'Gradle archive checksum mismatch.' >&2
        exit 1
    fi
    unzip -q -o "$tv_archive" -d "$tv_tools"
fi

if [[ ! -f "$tv_root/keystore-tv/debug.keystore" ]]; then
    mkdir -p "$tv_root/keystore-tv"
    "$JAVA_HOME/bin/keytool" -genkeypair -noprompt \
        -keystore "$tv_root/keystore-tv/debug.keystore" \
        -storepass android -keypass android -alias androiddebugkey \
        -dname 'CN=Glyphix TV Debug,O=Glyphix,C=GB' \
        -keyalg RSA -keysize 2048 -validity 10000
fi

if [[ $# -eq 0 ]]; then set -- assembleDebug lintDebug; fi
exec "$tv_gradle" -p "$tv_root/tvApp" -g "$tv_root/.gradle/tv-user" \
    --no-daemon --console=plain -Dorg.gradle.jvmargs=-Xmx2g --max-workers=2 "$@"
