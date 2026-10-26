#!/usr/bin/env bash
set -euo pipefail
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
mkdir -p "$out/src/android/os"
# Build is the only Android API read by the pure model matcher.
cat > "$out/src/android/os/Build.java" <<'JAVA'
package android.os;
public class Build {
    public static String MODEL = "";
    public static class VERSION { public static int SDK_INT = 35; }
}
JAVA
java com.sun.tools.javac.Main -d "$out/classes" "$out/src/android/os/Build.java" app/src/main/java/com/glyphix/app/model/DeviceProfile.java app/src/main/java/com/glyphix/app/util/PreferenceValues.java tests/startup/DeviceProfileTest.java
java -cp "$out/classes" DeviceProfileTest
java com.sun.tools.javac.Main -d "$out/classes" app/src/main/java/com/glyphix/app/logic/smartcapture/SmartSourceRules.java tests/smart-source/SmartSourceRulesTest.java
java -cp "$out/classes" SmartSourceRulesTest
