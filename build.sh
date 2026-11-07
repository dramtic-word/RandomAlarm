#!/usr/bin/env bash
# 无 Gradle 的离线构建：aapt2 + javac + d8 + zipalign + apksigner
#
# 依赖：
#   - JDK 17（设置 JAVA_HOME，或 PATH 里有 javac/java/keytool）
#   - Android SDK（设置 ANDROID_HOME / ANDROID_SDK_ROOT，或放在常见路径）
#   - Python 3（把 classes.dex 写进 APK）
#
# 用法：
#   bash build.sh
# 产物：
#   RandomAlarm-debug.apk
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

# ---------- 定位 Android SDK ----------
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ] || [ ! -d "$SDK" ]; then
  for d in "$HOME/Android/Sdk" "$HOME/Library/Android/sdk" \
           "/d/android-dev/sdk" "/d/dsh web/_tools_android/sdk" "C:/Android/Sdk"; do
    if [ -d "$d" ]; then SDK="$d"; break; fi
  done
fi
if [ -z "${SDK:-}" ] || [ ! -d "$SDK" ]; then
  echo "错误：找不到 Android SDK。请设置 ANDROID_HOME 或 ANDROID_SDK_ROOT。" >&2
  exit 1
fi

BT="$(ls -d "$SDK"/build-tools/[0-9]* 2>/dev/null | sort -V | tail -1)"
if [ -z "${BT:-}" ] || [ ! -d "$BT" ]; then
  echo "错误：$SDK/build-tools 下没有 build-tools。" >&2
  exit 1
fi
PLATFORM="$(ls -d "$SDK"/platforms/android-* 2>/dev/null | sort -V | tail -1)/android.jar"
if [ ! -f "$PLATFORM" ]; then
  echo "错误：找不到 $SDK/platforms/android-*/android.jar。" >&2
  exit 1
fi

# ---------- 定位 JDK ----------
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
  JAVAC="$JAVA_HOME/bin/javac"
  JAVA="$JAVA_HOME/bin/java"
  KEYTOOL="$JAVA_HOME/bin/keytool"
else
  JAVAC="$(command -v javac || true)"
  JAVA="$(command -v java || true)"
  KEYTOOL="$(command -v keytool || true)"
fi
if [ -z "$JAVAC" ] || [ -z "$JAVA" ]; then
  echo "错误：找不到 JDK 17。请设置 JAVA_HOME。" >&2
  exit 1
fi

# ---------- 工作目录（避免中文/空格路径） ----------
WORK="${RANDOMALARM_WORK:-${TMPDIR:-/tmp}/randomalarm-work}"
rm -rf "$WORK"
mkdir -p "$WORK"
cp -r "$ROOT/app" "$WORK/app"

# AndroidManifest 里不再写 package（Gradle 用 namespace），手工构建时补上
if ! grep -q 'package="' "$WORK/app/AndroidManifest.xml"; then
  sed -i 's|<manifest |<manifest package="com.dsh.randomalarm" |' \
    "$WORK/app/AndroidManifest.xml"
fi

BUILD="$WORK/build"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex"

# 部分环境下 aapt2 直接读写 SDK 目录会失败，复制到工作目录再运行
TOOLS="$WORK/tools"
mkdir -p "$TOOLS"
AAPT2_SRC="$BT/aapt2.exe"
[ -f "$AAPT2_SRC" ] || AAPT2_SRC="$BT/aapt2"
ZIPALIGN_SRC="$BT/zipalign.exe"
[ -f "$ZIPALIGN_SRC" ] || ZIPALIGN_SRC="$BT/zipalign"
cp "$AAPT2_SRC" "$TOOLS/"
cp "$ZIPALIGN_SRC" "$TOOLS/"
AAPT2="$TOOLS/$(basename "$AAPT2_SRC")"
ZIPALIGN="$TOOLS/$(basename "$ZIPALIGN_SRC")"

PYTHON="$(command -v python || true)"
if [ -z "$PYTHON" ] || ! "$PYTHON" --version >/dev/null 2>&1; then
  PYTHON="$(command -v python3 || true)"
fi
if [ -z "$PYTHON" ] || ! "$PYTHON" --version >/dev/null 2>&1; then
  echo "错误：找不到可用的 Python 3（用于把 classes.dex 打包进 APK）。" >&2
  exit 1
fi

echo "[1/6] aapt2 compile"
"$AAPT2" compile --dir "$WORK/app/res" -o "$BUILD/res.zip"

echo "[2/6] aapt2 link"
"$AAPT2" link -o "$BUILD/base.apk" -I "$PLATFORM" \
  --manifest "$WORK/app/AndroidManifest.xml" \
  -R "$BUILD/res.zip" --java "$BUILD/gen" --auto-add-overlay

echo "[3/6] javac"
cd "$WORK"
find app/java build/gen -name '*.java' > build/sources.txt
"$JAVAC" -encoding UTF-8 -source 8 -target 8 -classpath "$PLATFORM" \
  -d build/classes @build/sources.txt

echo "[4/6] d8"
find build/classes -name '*.class' > build/classes.txt
"$JAVA" -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 \
  --lib "$PLATFORM" --min-api 26 --output build/dex @build/classes.txt

echo "[5/6] package"
cp "$BUILD/base.apk" "$BUILD/unsigned.apk"
"$PYTHON" - "$BUILD/unsigned.apk" "$BUILD/dex/classes.dex" <<'PY'
import sys, zipfile
apk, dex = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(apk, 'a', zipfile.ZIP_DEFLATED) as z:
    z.write(dex, 'classes.dex')
PY
"$ZIPALIGN" -f 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"

echo "[6/6] sign"
KS="$ROOT/debug.keystore"
if [ ! -f "$KS" ]; then
  "$KEYTOOL" -genkeypair -keystore "$KS" -alias androiddebugkey \
    -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"
fi
"$JAVA" -jar "$BT/lib/apksigner.jar" sign --ks "$KS" \
  --ks-pass pass:android --key-pass pass:android \
  --out "$WORK/RandomAlarm-debug.apk" "$BUILD/aligned.apk"
"$JAVA" -jar "$BT/lib/apksigner.jar" verify --print-certs \
  "$WORK/RandomAlarm-debug.apk" > "$WORK/verify.txt" 2>&1 || true
head -4 "$WORK/verify.txt" || true
cp "$WORK/RandomAlarm-debug.apk" "$ROOT/RandomAlarm-debug.apk"

echo "APK: $ROOT/RandomAlarm-debug.apk"
