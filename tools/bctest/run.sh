#!/bin/sh
# Run a Beach Craft in-game test script (see README.md). Usage: tools/bctest/run.sh <script.txt> <outdir>
# Serialised with the build lock: it uses two JVMs (Gradle + Minecraft) while it runs.
SCRIPT=$(readlink -f "$1"); OUT=$(readlink -f "$2")
[ -f "$SCRIPT" ] || { echo "no script $1"; exit 1; }
mkdir -p "$OUT"
cd "$(dirname "$0")/../.." || exit 1
LOCK=${BC_LOCK:-/tmp/claude-0/-home-user/4ca9508b-4f8c-5a8e-91dc-6059d6cc887c/scratchpad/ref/build.lock}
export JAVA_HOME=/opt/jdk25/jdk-25.0.5+8
flock "$LOCK" sh -c "
  Xvfb ${BC_DISPLAY:-:97} -screen 0 1920x1080x24 >/dev/null 2>&1 &
  XPID=\$!
  sleep 1
  DISPLAY=${BC_DISPLAY:-:97} SDL_VIDEO_FORCE_EGL=1 timeout ${BC_TIMEOUT:-900} sh ./gradlew -q runBctest -PbcScript='$SCRIPT' -PbcOut='$OUT' -Pxmx=${BC_XMX:-3G} ${BC_JFR:+-PbcJfr=$BC_JFR} ${BC_JEI:+-Pjei=1} ${BC_LOOTR:+-PwithLootr} ${BC_JAR:+-PbcJar=$BC_JAR} > '$OUT/gradle.log' 2>&1
  echo \"exit \$?\" >> '$OUT/gradle.log'
  kill \$XPID 2>/dev/null
"
cp build/run/bctest/logs/latest.log "$OUT/latest.log" 2>/dev/null
grep -E "\[bctest\]|ERROR|Exception|Caused by" "$OUT/latest.log" | grep -v "Realms\|CraftPresence" | head -60
tail -2 "$OUT/gradle.log"
ls "$OUT" | grep -c png | sed 's/^/screenshots: /'
