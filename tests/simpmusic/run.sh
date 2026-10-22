#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
output=$(mktemp -d)
trap 'rm -rf "$output"' EXIT
java com.sun.tools.javac.Main -d "$output" \
  .simpmusic-build/core/media/media3/src/main/java/com/maxrave/media3/audio/GlyphixPcmDecoder.java \
  tests/simpmusic/PcmDecoderTest.java
java -cp "$output" PcmDecoderTest
