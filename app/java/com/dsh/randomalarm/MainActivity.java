package com.dsh.randomalarm;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_PERMS = 100;
    private LinearLayout listBox;
    private View permissionBox;
    private TextView permissionText;
    private View nextBox;
    private TextView nextTime, nextInfo, nextCountdown;
    private TextView tvNow;
    private TextView btnSkipNext;
    private View timerBox;
    private android.widget.LinearLayout timerList;
    private long nextAlarmId = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateNext();
            updateTimer();
            updateNow();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.applyOrientation(this);
        setContentView(R.layout.activity_main);
        listBox = findViewById(R.id.alarm_list);
        permissionBox = findViewById(R.id.permission_box);
        permissionText = findViewById(R.id.permission_text);
        nextBox = findViewById(R.id.next_box);
        nextTime = findViewById(R.id.next_time);
        nextInfo = findViewById(R.id.next_info);
        nextCountdown = findViewById(R.id.next_countdown);
        tvNow = findViewById(R.id.tv_now);
        timerBox = findViewById(R.id.timer_box);
        timerList = findViewById(R.id.timer_list);

        findViewById(R.id.btn_add).setOnClickListener(v ->
                startActivity(new Intent(this, EditActivity.class)));
        findViewById(R.id.btn_permission).setOnClickListener(v -> requestMissing());
        findViewById(R.id.btn_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        btnSkipNext = findViewById(R.id.btn_skip_next);
        findViewById(R.id.btn_timer).setOnClickListener(v -> showTimerDialog());
        cancelOldJobs();
        handleTest(getIntent());
    }

    /** 移除旧版本注册的周期自检任务。 */
    private void cancelOldJobs() {
        try {
            android.app.job.JobScheduler js =
                    (android.app.job.JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            if (js != null) {
                js.cancel(1001);
                js.cancel(1002);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleTest(intent);
    }

    /** 调试入口：am start ... --ez testAlarm true，1 分钟后响一次 */
    private void handleTest(Intent intent) {
        if (intent == null || !intent.getBooleanExtra("testAlarm", false)) return;
        Alarm a = new Alarm();
        a.id = 9999;
        java.util.Calendar c = java.util.Calendar.getInstance();
        int minutes = intent.getIntExtra("testMinutes", 1);
        c.add(java.util.Calendar.MINUTE, Math.max(1, minutes));
        a.hour = c.get(java.util.Calendar.HOUR_OF_DAY);
        a.minute = c.get(java.util.Calendar.MINUTE);
        a.daysMask = 0;
        a.enabled = true;
        a.label = "测试";
        AlarmStore.save(this, a);
        AlarmScheduler.schedule(this, a);
        Toast.makeText(this, "测试闹钟将在 " + minutes + " 分钟后响", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AlarmScheduler.rescheduleAll(this);
        render();
        checkPermissions();
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    private void updateNext() {
        long now = System.currentTimeMillis();
        Alarm best = null;
        long bestAt = Long.MAX_VALUE;
        for (Alarm a : AlarmStore.list(this)) {
            if (!a.enabled) continue;
            long at = AlarmScheduler.nextTrigger(a, now);
            if (a.snoozeAt > now && (at <= 0 || a.snoozeAt < at)) at = a.snoozeAt;
            if (at > now && at < bestAt) {
                bestAt = at;
                best = a;
            }
        }
        if (best == null) {
            nextBox.setVisibility(View.GONE);
            nextAlarmId = -1;
            return;
        }
        nextAlarmId = best.id;
        nextBox.setVisibility(View.VISIBLE);
        nextTime.setText(best.timeText());
        boolean snoozing = best.snoozeAt > now && best.snoozeAt == bestAt;
        String info = best.daysText();
        if (!best.label.isEmpty()) info += " · " + best.label;
        nextInfo.setText(info);
        if (snoozing) {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat(
                    "HH:mm", java.util.Locale.getDefault());
            nextCountdown.setText("贪睡中 · " + fmt.format(new java.util.Date(bestAt))
                    + " 再响（还有 " + formatDuration(bestAt - now) + "）");
        } else {
            nextCountdown.setText("还有 " + formatDuration(bestAt - now) + "响铃");
        }
        nextBox.setOnClickListener(v -> {
            Intent i = new Intent(this, EditActivity.class);
            i.putExtra("alarmId", nextAlarmId);
            startActivity(i);
        });
        final boolean skipped = best.skipAt > now;
        if (snoozing) {
            btnSkipNext.setText("取消贪睡");
            btnSkipNext.setOnClickListener(v -> {
                Alarm a = AlarmStore.get(this, nextAlarmId);
                AlarmScheduler.cancelSnooze(this, a);
                render();
                updateNext();
            });
        } else {
            btnSkipNext.setText(skipped ? "恢复本次" : "跳过本次");
            btnSkipNext.setOnClickListener(v -> {
                if (skipped) restore(nextAlarmId);
                else skipNext();
            });
        }
    }

    private void skipNext() {
        Alarm a = AlarmStore.get(this, nextAlarmId);
        if (a == null) return;
        long at = AlarmScheduler.nextTrigger(a, System.currentTimeMillis());
        a.skipAt = at;
        a.enabled = true;
        AlarmStore.save(this, a);
        AlarmScheduler.schedule(this, a);
        HistoryStore.add(this, "跳过", a.timeText() + " 本次");
        render();
        updateNext();
    }

    private void restore(long id) {
        Alarm a = AlarmStore.get(this, id);
        if (a == null) return;
        a.skipAt = 0;
        a.enabled = true;
        AlarmStore.save(this, a);
        AlarmScheduler.schedule(this, a);
        HistoryStore.add(this, "恢复", a.timeText());
        render();
        updateNext();
    }

    private String formatDuration(long ms) {
        long s = Math.max(0, ms / 1000);
        long d = s / 86400;
        s %= 86400;
        long h = s / 3600;
        s %= 3600;
        long m = s / 60;
        s %= 60;
        if (d > 0) return d + " 天 " + h + " 小时";
        if (h > 0) return h + " 小时 " + m + " 分钟";
        if (m > 0) return m + " 分钟 " + s + " 秒";
        return s + " 秒";
    }

    private void render() {
        List<Alarm> all = AlarmStore.list(this);
        // 时间升序；时间相同按周一→周日；仅一次的排在同时间最后
        all.sort((a, b) -> {
            int ta = a.hour * 60 + a.minute;
            int tb = b.hour * 60 + b.minute;
            if (ta != tb) return Integer.compare(ta, tb);
            int da = firstWeekday(a.daysMask);
            int db = firstWeekday(b.daysMask);
            if (da != db) return Integer.compare(da, db);
            return Long.compare(a.id, b.id);
        });
        listBox.removeAllViews();
        if (all.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_alarm);
            empty.setTextColor(0xFF9A9AA5);
            empty.setTextSize(15);
            empty.setPadding(0, 24, 0, 24);
            listBox.addView(empty);
            return;
        }
        for (Alarm a : all) {
            View item = LayoutInflater.from(this).inflate(R.layout.item_alarm, listBox, false);
            TextView time = item.findViewById(R.id.item_time);
            TextView days = item.findViewById(R.id.item_days);
            TextView label = item.findViewById(R.id.item_label);
            Switch sw = item.findViewById(R.id.item_switch);

            time.setText(a.timeText());
            days.setText(a.daysText()
                    + (a.skipAt > System.currentTimeMillis() ? " · 已跳过本次" : ""));
            if (!a.label.isEmpty()) {
                label.setText(a.label);
                label.setVisibility(View.VISIBLE);
            }
            time.setTextColor(a.enabled ? 0xFFF2F2F7 : 0xFF6E6E78);
            sw.setChecked(a.enabled);
            sw.setOnCheckedChangeListener((buttonView, checked) -> {
                a.enabled = checked;
                AlarmStore.save(this, a);
                if (checked) AlarmScheduler.schedule(this, a);
                else AlarmScheduler.cancel(this, a);
                time.setTextColor(checked ? 0xFFF2F2F7 : 0xFF6E6E78);
            });
            item.setOnClickListener(v -> {
                Intent i = new Intent(this, EditActivity.class);
                i.putExtra("alarmId", a.id);
                startActivity(i);
            });
            item.setOnLongClickListener(v -> {
                final boolean skipped = a.skipAt > System.currentTimeMillis();
                java.util.List<String> opts = new java.util.ArrayList<>();
                if (skipped) opts.add("恢复响铃");
                opts.add("删除闹钟");
                new android.app.AlertDialog.Builder(this)
                        .setTitle(a.timeText())
                        .setItems(opts.toArray(new String[0]), (d, which) -> {
                            if (opts.get(which).startsWith("恢复")) {
                                restore(a.id);
                            } else {
                                confirmDelete(a);
                            }
                        })
                        .show();
                return true;
            });
            listBox.addView(item);
        }
    }

    private void confirmDelete(Alarm a) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("删除闹钟")
                .setMessage("确定删除 " + a.timeText() + " 这个闹钟吗？")
                .setPositiveButton("删除", (d, w) -> {
                    AlarmStore.delete(this, a.id);
                    AlarmScheduler.cancel(this, a);
                    render();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showTimerDialog() {
        final java.util.List<Prefs.TimerPreset> presets = Prefs.timerPresets(this);
        String[] items = new String[presets.size() + 2];
        for (int i = 0; i < presets.size(); i++) {
            Prefs.TimerPreset p = presets.get(i);
            items[i] = formatPreset(p.seconds) + (p.label.isEmpty() ? "" : " · " + p.label);
        }
        items[presets.size()] = "自定义…";
        items[presets.size() + 1] = "管理快捷时间…";
        new android.app.AlertDialog.Builder(this)
                .setTitle("计时器")
                .setItems(items, (d, which) -> {
                    if (which < presets.size()) {
                        Prefs.TimerPreset p = presets.get(which);
                        startTimer(p.seconds, p.label);
                    } else if (which == presets.size()) {
                        showCustomTimerDialog();
                    } else {
                        startActivity(new Intent(this, SettingsActivity.class));
                    }
                })
                .show();
    }

    private void showCustomTimerDialog() {
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        final android.widget.EditText eh = new android.widget.EditText(this);
        eh.setHint("0~99");
        eh.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        final android.widget.EditText em = new android.widget.EditText(this);
        em.setHint("0~59");
        em.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        final android.widget.EditText es = new android.widget.EditText(this);
        es.setHint("0~59");
        es.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        row.addView(eh);
        row.addView(em);
        row.addView(es);
        final android.widget.EditText el = new android.widget.EditText(this);
        el.setHint("备注（可选）");
        box.addView(row);
        box.addView(el);
        new android.app.AlertDialog.Builder(this)
                .setTitle("自定义计时")
                .setView(box)
                .setPositiveButton("开始", (d, w) -> {
                    int total = parseInt(eh.getText().toString()) * 3600
                            + parseInt(em.getText().toString()) * 60
                            + parseInt(es.getText().toString());
                    if (total > 0) startTimer(total, el.getText().toString().trim());
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatPreset(int sec) {
        if (sec < 60) return sec + " 秒";
        int h = sec / 3600;
        int m = (sec % 3600) / 60;
        int s = sec % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append(" 小时");
        if (m > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(m).append(" 分");
        }
        if (s > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(s).append(" 秒");
        }
        return sb.toString();
    }

    private void startTimer(int seconds, String note) {
        long at = System.currentTimeMillis() + seconds * 1000L;
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        TimerStore.Item it = new TimerStore.Item();
        it.id = System.currentTimeMillis() * 1000L + (long) (Math.random() * 1000);
        it.at = at;
        it.label = note;
        Intent i = new Intent(this, TimerReceiver.class);
        i.setAction(TimerReceiver.ACTION_TIMER);
        i.putExtra("timerId", it.id);
        i.putExtra("timerLabel", note);
        PendingIntent pi = PendingIntent.getBroadcast(this, timerRequestCode(it.id), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, null), pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
        TimerStore.add(this, it);
        renderTimers();
        Toast.makeText(this, "计时器：" + formatPreset(seconds) + "后响铃", Toast.LENGTH_SHORT).show();
    }

    private int timerRequestCode(long id) {
        return (int) (id ^ (id >>> 32));
    }

    private void updateNow() {
        if (tvNow != null) {
            tvNow.setText(new java.text.SimpleDateFormat("HH:mm:ss",
                    java.util.Locale.getDefault()).format(new java.util.Date()));
        }
    }

    private void updateTimer() {
        renderTimers();
    }

    private void renderTimers() {
        List<TimerStore.Item> list = TimerStore.list(this);
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).at <= now) {
                list.remove(i);
                changed = true;
            }
        }
        if (changed) TimerStore.saveList(this, list);
        timerList.removeAllViews();
        if (list.isEmpty()) {
            timerBox.setVisibility(View.GONE);
            return;
        }
        timerBox.setVisibility(View.VISIBLE);
        for (TimerStore.Item it : list) {
            final long id = it.id;
            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView tv = new TextView(this);
            tv.setTextColor(0xFFBFE8C9);
            tv.setTextSize(15);
            String note = it.label.isEmpty() ? "计时器" : "计时器 · " + it.label;
            tv.setText(note + " · 还有 " + formatClock(it.at - now) + " 响铃");
            android.widget.LinearLayout.LayoutParams lp =
                    new android.widget.LinearLayout.LayoutParams(0,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(tv, lp);
            android.widget.Button btn = new android.widget.Button(this);
            btn.setText("取消");
            btn.setAllCaps(false);
            btn.setTextColor(0xFFBFE8C9);
            btn.setBackgroundColor(0xFF2E3D31);
            btn.setTextSize(14);
            btn.setOnClickListener(v -> cancelTimer(id));
            row.addView(btn);
            timerList.addView(row);
        }
    }

    private String formatClock(long ms) {
        long s = Math.max(0, ms / 1000);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0) return String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", h, m, sec);
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", m, sec);
    }

    private void cancelTimer(long id) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(this, TimerReceiver.class);
        i.setAction(TimerReceiver.ACTION_TIMER);
        PendingIntent pi = PendingIntent.getBroadcast(this, timerRequestCode(id), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am != null) am.cancel(pi);
        TimerStore.remove(this, id);
        renderTimers();
        Toast.makeText(this, "计时器已取消", Toast.LENGTH_SHORT).show();
    }

    /** 最早出现的星期：周一=0…周日=6；仅一次返回 7。 */
    private static int firstWeekday(int mask) {
        if (mask == 0) return 7;
        for (int i = 0; i < 7; i++) {
            if ((mask & (1 << i)) != 0) return i;
        }
        return 7;
    }

    private boolean granted(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkPermissions() {
        List<String> problems = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 33) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            boolean notifOk = granted(Manifest.permission.POST_NOTIFICATIONS)
                    && (nm == null || nm.areNotificationsEnabled());
            if (!notifOk) problems.add("通知权限");
            if (!granted(Manifest.permission.READ_MEDIA_AUDIO)) problems.add("读取本地音乐");
        } else {
            if (!granted(Manifest.permission.READ_EXTERNAL_STORAGE)) problems.add("读取本地音乐");
        }

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 31 && am != null && !am.canScheduleExactAlarms()) {
            problems.add("精确闹钟");
        }
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            problems.add("电池优化白名单");
        }
        if (!Settings.canDrawOverlays(this)) {
            problems.add("后台弹出界面（可选）");
        }

        if (problems.isEmpty()) {
            permissionBox.setVisibility(View.GONE);
        } else {
            permissionBox.setVisibility(View.VISIBLE);
            permissionText.setText("还差：" + String.join("、", problems) + "。点下面的按钮逐项处理。");
        }
    }

    private void requestMissing() {
        if (Build.VERSION.SDK_INT >= 33) {
            List<String> perms = new ArrayList<>();
            if (!granted(Manifest.permission.POST_NOTIFICATIONS)) perms.add(Manifest.permission.POST_NOTIFICATIONS);
            if (!granted(Manifest.permission.READ_MEDIA_AUDIO)) perms.add(Manifest.permission.READ_MEDIA_AUDIO);
            if (!perms.isEmpty()) {
                requestPermissions(perms.toArray(new String[0]), REQ_PERMS);
                return;
            }
        } else if (!granted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERMS);
            return;
        }

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 31 && am != null && !am.canScheduleExactAlarms()) {
            try {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
            return;
        }

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            try {
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                try {
                    startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                } catch (Exception ignored) {
                }
            }
            return;
        }

        if (!Settings.canDrawOverlays(this)) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {
            }
            return;
        }

        Toast.makeText(this, "权限都齐了", Toast.LENGTH_SHORT).show();
        checkPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissions();
    }
}
