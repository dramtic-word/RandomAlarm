# 项目约定（给后续会话）

## 整数版本自动发布

当版本号到达整数（如 7.0、8.0）时，直接执行完整发布流程，不需要再问用户：

1. 升版本号（两处必须一致）：
   - `app/AndroidManifest.xml`：`android:versionCode` +1，`android:versionName="X.0"`
   - `app/build.gradle`：`versionCode` +1，`versionName "X.0"`
2. 构建：`JAVA_HOME=/c/jdk17 bash build.sh`（产物 `RandomAlarm-debug.apk`）
3. 提交并推送：`git add -A && git commit -m "release: X.0" && git push`
4. 打标签并推送：`git tag -a vX.0 -m "RandomAlarm X.0" && git push origin vX.0`
5. 发 Release 并上传 APK：
   ```bash
   GH_CONFIG_DIR='D:\dsh web\_tools_gh\config' "/d/dsh web/_tools_gh/bin/gh.exe" \
     release create vX.0 RandomAlarm-debug.apk --title "RandomAlarm X.0" --notes-file <notes>
   ```
6. 用 `gh release view vX.0 --json assets` 确认附件已上传。

## 环境

- GitHub CLI：`/d/dsh web/_tools_gh/bin/gh.exe`，配置目录 `D:\dsh web\_tools_gh\config`（必须通过 `GH_CONFIG_DIR` 指定）
- git 推拉走 SSH 443（`~/.ssh/config` 里 `Host github.com` → `ssh.github.com:443`）
- 远端：`git@github.com:dramtic-word/RandomAlarm.git`
- 手工构建脚本 `build.sh` 需要 `JAVA_HOME` 与 `ANDROID_HOME`（或常见路径）

## 不要提交

- `RandomAlarm-debug.apk`、`*.keystore`、`build/`、`local.properties`（`.gitignore` 已排除）
- 任何 token / 凭据
