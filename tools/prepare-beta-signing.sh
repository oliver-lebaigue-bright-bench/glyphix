#!/usr/bin/env bash
set -euo pipefail
mkdir -p keystore
if [[ -n "${DEBUG_KEYSTORE_BASE64:-}" ]]; then
  printf '%s' "$DEBUG_KEYSTORE_BASE64" | base64 --decode > keystore/debug.keystore
elif [[ ! -s keystore/debug.keystore ]]; then
  keytool -genkeypair -keystore keystore/debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Glyphix Beta,O=Glyphix,C=IE'
fi
# Public fingerprints only. Register these for com.glyphix.app in Firebase/Google Cloud.
keytool -list -v -keystore keystore/debug.keystore -storepass android -alias androiddebugkey | sed -n '/SHA1:/p; /SHA256:/p'

python3 tools/beta-signing-report.py
