#!/usr/bin/env bash
# 安装到已连接的手机并启动
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
MSYS_NO_PATHCONV=1 adb install -r "$DIR/RandomAlarm-debug.apk"
MSYS_NO_PATHCONV=1 adb shell am start -n com.dsh.randomalarm/.MainActivity
echo "已安装并启动"
