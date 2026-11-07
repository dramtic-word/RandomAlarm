package com.dsh.randomalarm;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    private static final int REQ_FOLDER = 300;
    private static final int REQ_SONG = 301;
    private static final int[] SNOOZE = {5, 10, 15, 20, 30};
    private static final int[] FADE_SEC = {30, 60, 120, 300, 600};
    private static final String[] FADE_TEXT = {"30 秒", "1 分钟", "2 分钟", "5 分钟", "10 分钟"};
    private static final int[] VOL = {5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
    private static final int[] INIT_VOL = {5, 10, 15, 20, 30, 40, 50};
    private static final int[] RING_SEC = {0, 60, 120, 300, 600, 900};
    private static final String[] RING_TEXT = {"一直响", "1 分钟", "2 分钟", "5 分钟", "10 分钟", "15 分钟"};
    private static final String[] TIMEOUT_TEXT = {"到点停止", "到点贪睡"};
    private static final int[] TIMEOUT_COUNT = {1, 2, 3, 5};
    private static final int[] START_MIN = {0, 5, 10, 15, 20, 25, 30, 40, 50};
    private static final int[] START_MAX = {50, 60, 70, 75, 80, 85, 90, 95, 100};
    private static final int[] COVER = {20, 40, 60, 80, 100};
    private static final int[] MIN_RING = {0, 3, 5, 10, 15};
    private static final String[] MIN_RING_TEXT = {"不限制", "3 秒", "5 秒", "10 秒", "15 秒"};
    private static final String[] SWIPE_TEXT = {"禁止", "关闭", "贪睡", "切歌"};
    private static final String[] ORIENT_TEXT = {"自由（跟随系统）", "强制竖屏", "强制横屏"};
    private Switch swRandom, swFade, swVibrate;
    private Spinner spSnooze, spFadeSec, spMaxVolume, spInitVolume, spRingTime, spTimeoutAction, spTimeoutCount, spStartMin, spStartMax, spCover, spMinRing;
    private Spinner spSwipeUp, spSwipeDown, spSwipeLeft, spSwipeRight, spOrientation;
    private android.widget.LinearLayout presetBox;
    private TextView tvFolder;
    private String folderUri = "";
    private String defSongUri = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.applyOrientation(this);
        setContentView(R.layout.activity_settings);
        swRandom = findViewById(R.id.sw_def_random);
        swFade = findViewById(R.id.sw_def_fade);
        swVibrate = findViewById(R.id.sw_def_vibrate);
        spSnooze = findViewById(R.id.sp_def_snooze);
        spFadeSec = findViewById(R.id.sp_def_fade_sec);
        spMaxVolume = findViewById(R.id.sp_def_volume);
        spInitVolume = findViewById(R.id.sp_def_init);
        spRingTime = findViewById(R.id.sp_def_ring);
        spMinRing = findViewById(R.id.sp_min_ring);
        spTimeoutAction = findViewById(R.id.sp_def_timeout_action);
        spTimeoutCount = findViewById(R.id.sp_def_timeout_count);
        spStartMin = findViewById(R.id.sp_def_start_min);
        spStartMax = findViewById(R.id.sp_def_start_max);
        spCover = findViewById(R.id.sp_def_cover);
        spSwipeUp = findViewById(R.id.sp_swipe_up);
        spSwipeDown = findViewById(R.id.sp_swipe_down);
        spSwipeLeft = findViewById(R.id.sp_swipe_left);
        spSwipeRight = findViewById(R.id.sp_swipe_right);
        spOrientation = findViewById(R.id.sp_orientation);
        presetBox = findViewById(R.id.preset_box);
        tvFolder = findViewById(R.id.tv_def_folder);

        String[] snoozeText = new String[SNOOZE.length];
        for (int i = 0; i < SNOOZE.length; i++) snoozeText[i] = SNOOZE[i] + " 分钟";
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, snoozeText);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSnooze.setAdapter(adapter);

        ArrayAdapter<String> fadeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, FADE_TEXT);
        fadeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFadeSec.setAdapter(fadeAdapter);

        String[] volText = new String[VOL.length];
        for (int i = 0; i < VOL.length; i++) volText[i] = VOL[i] + "%";
        ArrayAdapter<String> volAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, volText);
        volAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMaxVolume.setAdapter(volAdapter);

        String[] initText = new String[INIT_VOL.length];
        for (int i = 0; i < INIT_VOL.length; i++) initText[i] = INIT_VOL[i] + "%";
        ArrayAdapter<String> initAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, initText);
        initAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spInitVolume.setAdapter(initAdapter);

        ArrayAdapter<String> ringAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, RING_TEXT);
        ringAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRingTime.setAdapter(ringAdapter);

        ArrayAdapter<String> minRingAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, MIN_RING_TEXT);
        minRingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMinRing.setAdapter(minRingAdapter);

        ArrayAdapter<String> timeoutAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, TIMEOUT_TEXT);
        timeoutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTimeoutAction.setAdapter(timeoutAdapter);

        String[] countText = new String[TIMEOUT_COUNT.length];
        for (int i = 0; i < TIMEOUT_COUNT.length; i++) countText[i] = TIMEOUT_COUNT[i] + " 次";
        ArrayAdapter<String> countAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, countText);
        countAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTimeoutCount.setAdapter(countAdapter);

        String[] startMinText = new String[START_MIN.length];
        for (int i = 0; i < START_MIN.length; i++) startMinText[i] = START_MIN[i] + "%";
        ArrayAdapter<String> startMinAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, startMinText);
        startMinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStartMin.setAdapter(startMinAdapter);

        String[] startMaxText = new String[START_MAX.length];
        for (int i = 0; i < START_MAX.length; i++) startMaxText[i] = START_MAX[i] + "%";
        ArrayAdapter<String> startMaxAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, startMaxText);
        startMaxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStartMax.setAdapter(startMaxAdapter);

        String[] coverText = new String[COVER.length];
        for (int i = 0; i < COVER.length; i++) coverText[i] = COVER[i] + "%";
        ArrayAdapter<String> coverAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, coverText);
        coverAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCover.setAdapter(coverAdapter);

        for (Spinner sp : new Spinner[]{spSwipeUp, spSwipeDown, spSwipeLeft, spSwipeRight}) {
            ArrayAdapter<String> swipeAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, SWIPE_TEXT);
            swipeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sp.setAdapter(swipeAdapter);
        }

        ArrayAdapter<String> orientAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ORIENT_TEXT);
        orientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOrientation.setAdapter(orientAdapter);

        swRandom.setChecked(Prefs.randomStart(this));
        swFade.setChecked(Prefs.fadeIn(this));
        swVibrate.setChecked(Prefs.vibrate(this));
        folderUri = Prefs.folderUri(this);
        tvFolder.setText(folderLabel(folderUri));
        defSongUri = Prefs.songUri(this);
        updateDefSongText();
        findViewById(R.id.btn_def_pick_song).setOnClickListener(v -> pickDefSong());
        findViewById(R.id.btn_def_clear_song).setOnClickListener(v -> {
            defSongUri = "";
            Prefs.setSongUri(this, "");
            updateDefSongText();
        });
        for (int i = 0; i < SNOOZE.length; i++) {
            if (SNOOZE[i] == Prefs.snooze(this)) spSnooze.setSelection(i);
        }
        for (int i = 0; i < FADE_SEC.length; i++) {
            if (FADE_SEC[i] == Prefs.fadeSeconds(this)) spFadeSec.setSelection(i);
        }
        for (int i = 0; i < VOL.length; i++) {
            if (VOL[i] == Prefs.maxVolume(this)) spMaxVolume.setSelection(i);
        }
        for (int i = 0; i < INIT_VOL.length; i++) {
            if (INIT_VOL[i] == Prefs.initialVolume(this)) spInitVolume.setSelection(i);
        }
        for (int i = 0; i < RING_SEC.length; i++) {
            if (RING_SEC[i] == Prefs.ringSeconds(this)) spRingTime.setSelection(i);
        }
        for (int i = 0; i < MIN_RING.length; i++) {
            if (MIN_RING[i] == Prefs.minRingSeconds(this)) spMinRing.setSelection(i);
        }
        spTimeoutAction.setSelection(Prefs.timeoutAction(this) == 1 ? 1 : 0);
        for (int i = 0; i < TIMEOUT_COUNT.length; i++) {
            if (TIMEOUT_COUNT[i] == Prefs.timeoutSnoozeCount(this)) spTimeoutCount.setSelection(i);
        }
        for (int i = 0; i < START_MIN.length; i++) {
            if (START_MIN[i] == Prefs.randomStartMin(this)) spStartMin.setSelection(i);
        }
        for (int i = 0; i < START_MAX.length; i++) {
            if (START_MAX[i] == Prefs.randomStartMax(this)) spStartMax.setSelection(i);
        }
        for (int i = 0; i < COVER.length; i++) {
            if (COVER[i] == Prefs.coverBrightness(this)) spCover.setSelection(i);
        }
        spSwipeUp.setSelection(Prefs.swipeAction(this, "up"));
        spSwipeDown.setSelection(Prefs.swipeAction(this, "down"));
        spSwipeLeft.setSelection(Prefs.swipeAction(this, "left"));
        spSwipeRight.setSelection(Prefs.swipeAction(this, "right"));
        spOrientation.setSelection(Prefs.orientation(this));

        swRandom.setOnCheckedChangeListener((v, checked) -> {
            Prefs.setRandomStart(this, checked);
            updateRandomVisibility(checked);
        });
        swFade.setOnCheckedChangeListener((v, checked) -> {
            Prefs.setFadeIn(this, checked);
            updateFadeVisibility(checked);
        });
        swVibrate.setOnCheckedChangeListener((v, checked) -> Prefs.setVibrate(this, checked));
        spSnooze.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                Prefs.setSnooze(SettingsActivity.this, SNOOZE[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        spFadeSec.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                Prefs.setFadeSeconds(SettingsActivity.this, FADE_SEC[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        spMaxVolume.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                Prefs.setMaxVolume(SettingsActivity.this, VOL[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        spInitVolume.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                Prefs.setInitialVolume(SettingsActivity.this, INIT_VOL[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        spRingTime.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                Prefs.setRingSeconds(SettingsActivity.this, RING_SEC[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        spMinRing.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                Prefs.setMinRingSeconds(SettingsActivity.this, MIN_RING[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        spTimeoutAction.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                Prefs.setTimeoutAction(SettingsActivity.this, position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        spTimeoutCount.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                Prefs.setTimeoutSnoozeCount(SettingsActivity.this, TIMEOUT_COUNT[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        spStartMin.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                Prefs.setRandomStartMin(SettingsActivity.this, START_MIN[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        spStartMax.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                Prefs.setRandomStartMax(SettingsActivity.this, START_MAX[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        spCover.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                Prefs.setCoverBrightness(SettingsActivity.this, COVER[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        spOrientation.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                Prefs.setOrientation(SettingsActivity.this, position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        bindSwipe(spSwipeUp, "up");
        bindSwipe(spSwipeDown, "down");
        bindSwipe(spSwipeLeft, "left");
        bindSwipe(spSwipeRight, "right");

        findViewById(R.id.btn_add_preset).setOnClickListener(v -> addPresetDialog());
        renderPresets();
        bindGroup(R.id.hdr_default, R.id.group_default, false);
        bindGroup(R.id.hdr_global, R.id.group_global, false);
        bindGroup(R.id.hdr_timer, R.id.group_timer, false);

        NumberSpinner.bind(this, spSnooze, SNOOZE, "分钟", 1, 120,
                Prefs.snooze(this), v -> Prefs.setSnooze(this, v));
        NumberSpinner.bind(this, spFadeSec, FADE_SEC, "秒", 5, 600,
                Prefs.fadeSeconds(this), v -> Prefs.setFadeSeconds(this, v));
        NumberSpinner.bind(this, spMaxVolume, VOL, "%", 5, 100,
                Prefs.maxVolume(this), v -> Prefs.setMaxVolume(this, v));
        NumberSpinner.bind(this, spInitVolume, INIT_VOL, "%", 5, 100,
                Prefs.initialVolume(this), v -> Prefs.setInitialVolume(this, v));
        NumberSpinner.bind(this, spRingTime, RING_SEC, "秒", 0, 3600,
                Prefs.ringSeconds(this), v -> Prefs.setRingSeconds(this, v));
        NumberSpinner.bind(this, spMinRing, MIN_RING, "秒", 0, 60,
                Prefs.minRingSeconds(this), v -> Prefs.setMinRingSeconds(this, v));
        NumberSpinner.bind(this, spTimeoutCount, TIMEOUT_COUNT, "次", 1, 10,
                Prefs.timeoutSnoozeCount(this), v -> Prefs.setTimeoutSnoozeCount(this, v));
        NumberSpinner.bind(this, spStartMin, START_MIN, "%", 0, 95,
                Prefs.randomStartMin(this), v -> Prefs.setRandomStartMin(this, v));
        NumberSpinner.bind(this, spStartMax, START_MAX, "%", 5, 100,
                Prefs.randomStartMax(this), v -> Prefs.setRandomStartMax(this, v));
        NumberSpinner.bind(this, spCover, COVER, "%", 10, 100,
                Prefs.coverBrightness(this), v -> Prefs.setCoverBrightness(this, v));

        findViewById(R.id.btn_def_pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            try {
                startActivityForResult(i, REQ_FOLDER);
            } catch (Exception ignored) {
            }
        });
        findViewById(R.id.btn_def_clear).setOnClickListener(v -> {
            folderUri = "";
            Prefs.setFolderUri(this, "");
            tvFolder.setText("全部本地音乐");
        });
        updateFadeVisibility(swFade.isChecked());
        updateRandomVisibility(swRandom.isChecked());
        findViewById(R.id.btn_apply_all).setOnClickListener(v -> confirmApplyAll());
        findViewById(R.id.btn_history).setOnClickListener(v -> showHistory());
    }

    private void showHistory() {
        java.util.List<HistoryStore.Event> list = HistoryStore.list(this);
        if (list.isEmpty()) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("响铃历史")
                    .setMessage("还没有记录。闹钟触发、播放、失败、停止都会记在这里。")
                    .setPositiveButton("知道了", null)
                    .show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat(
                "MM-dd HH:mm:ss", java.util.Locale.getDefault());
        int n = Math.min(list.size(), 50);
        for (int i = 0; i < n; i++) {
            HistoryStore.Event e = list.get(i);
            sb.append(fmt.format(new java.util.Date(e.time)))
                    .append("  ").append(e.type);
            if (!e.detail.isEmpty()) sb.append("  ").append(e.detail);
            sb.append('\n');
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle("响铃历史（最近 " + n + " 条）")
                .setMessage(sb.toString())
                .setPositiveButton("关闭", null)
                .setNeutralButton("清空", (d, w) -> {
                    HistoryStore.clear(this);
                    android.widget.Toast.makeText(this, "已清空",
                            android.widget.Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void bindSwipe(Spinner sp, String dir) {
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                Prefs.setSwipeAction(SettingsActivity.this, dir, position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void updateRandomVisibility(boolean on) {
        findViewById(R.id.random_group_def).setVisibility(on ? android.view.View.VISIBLE
                : android.view.View.GONE);
    }

    private void updateFadeVisibility(boolean on) {
        findViewById(R.id.fade_group_def).setVisibility(on ? android.view.View.VISIBLE
                : android.view.View.GONE);
    }

    private void confirmApplyAll() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("应用到所有闹钟")
                .setMessage("将当前默认设置（随机起播、渐强、音量、响铃时长、到点行为、贪睡、震动、音乐来源）覆盖到全部已创建的闹钟？\n\n时间和重复星期保持不变。")
                .setPositiveButton("应用", (d, w) -> applyAll())
                .setNegativeButton("取消", null)
                .show();
    }

    private void applyAll() {
        java.util.List<Alarm> all = AlarmStore.list(this);
        for (Alarm a : all) {
            a.randomStart = Prefs.randomStart(this);
            a.randomStartMin = Prefs.randomStartMin(this);
            a.randomStartMax = Prefs.randomStartMax(this);
            a.fadeIn = Prefs.fadeIn(this);
            a.fadeSeconds = Prefs.fadeSeconds(this);
            a.initialVolume = Prefs.initialVolume(this);
            a.maxVolume = Prefs.maxVolume(this);
            a.ringSeconds = Prefs.ringSeconds(this);
            a.timeoutAction = Prefs.timeoutAction(this);
            a.timeoutSnoozeCount = Prefs.timeoutSnoozeCount(this);
            a.vibrate = Prefs.vibrate(this);
            a.folderUri = Prefs.folderUri(this);
            a.snoozeMinutes = Prefs.snooze(this);
            AlarmStore.save(this, a);
            if (a.enabled) AlarmScheduler.schedule(this, a);
        }
        android.widget.Toast.makeText(this, "已应用到 " + all.size() + " 个闹钟",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private void bindGroup(int headerId, int groupId, boolean open) {
        TextView hdr = findViewById(headerId);
        final View group = findViewById(groupId);
        group.setVisibility(open ? View.VISIBLE : View.GONE);
        hdr.setText((open ? "▼ " : "▶ ") + baseTitle(hdr));
        hdr.setOnClickListener(v -> {
            boolean visible = group.getVisibility() == View.VISIBLE;
            group.setVisibility(visible ? View.GONE : View.VISIBLE);
            hdr.setText((visible ? "▶ " : "▼ ") + baseTitle(hdr));
        });
    }

    private String baseTitle(TextView hdr) {
        String t = hdr.getText().toString();
        if (t.startsWith("▼ ") || t.startsWith("▶ ")) return t.substring(2);
        return t;
    }

    private void renderPresets() {
        presetBox.removeAllViews();
        final java.util.List<Prefs.TimerPreset> presets = Prefs.timerPresets(this);
        for (int i = 0; i < presets.size(); i++) {
            final int idx = i;
            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            android.widget.TextView tv = new android.widget.TextView(this);
            String text = formatPreset(presets.get(i).seconds);
            if (!presets.get(i).label.isEmpty()) text += " · " + presets.get(i).label;
            tv.setText(text);
            tv.setTextColor(0xFFF2F2F7);
            tv.setTextSize(15);
            row.addView(tv, new android.widget.LinearLayout.LayoutParams(0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            android.widget.Button edit = new android.widget.Button(this);
            edit.setText("编辑");
            edit.setTextColor(0xFFB9A6FF);
            edit.setTextSize(13);
            edit.setBackgroundColor(0x00000000);
            edit.setOnClickListener(v -> editPresetDialog(idx));
            row.addView(edit);
            android.widget.Button del = new android.widget.Button(this);
            del.setText("删除");
            del.setTextColor(0xFFFF6B81);
            del.setTextSize(13);
            del.setBackgroundColor(0x00000000);
            del.setOnClickListener(v -> {
                java.util.List<Prefs.TimerPreset> list = Prefs.timerPresets(this);
                if (idx < list.size()) list.remove(idx);
                Prefs.setTimerPresets(this, list);
                renderPresets();
            });
            row.addView(del);
            presetBox.addView(row);
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

    private void editPresetDialog(int index) {
        java.util.List<Prefs.TimerPreset> list = Prefs.timerPresets(this);
        if (index < 0 || index >= list.size()) return;
        Prefs.TimerPreset p = list.get(index);
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        final android.widget.EditText eh = new android.widget.EditText(this);
        eh.setHint("0~99");
        eh.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        eh.setText(String.valueOf(p.seconds / 3600));
        final android.widget.EditText em = new android.widget.EditText(this);
        em.setHint("0~59");
        em.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        em.setText(String.valueOf((p.seconds % 3600) / 60));
        final android.widget.EditText es = new android.widget.EditText(this);
        es.setHint("0~59");
        es.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        es.setText(String.valueOf(p.seconds % 60));
        row.addView(eh);
        row.addView(em);
        row.addView(es);
        final android.widget.EditText el = new android.widget.EditText(this);
        el.setHint("备注");
        el.setText(p.label);
        box.addView(row);
        box.addView(el);
        new android.app.AlertDialog.Builder(this)
                .setTitle("编辑快捷时间")
                .setView(box)
                .setPositiveButton("保存", (d, w) -> {
                    int total = parseInt(eh.getText().toString()) * 3600
                            + parseInt(em.getText().toString()) * 60
                            + parseInt(es.getText().toString());
                    if (total <= 0) return;
                    java.util.List<Prefs.TimerPreset> cur = Prefs.timerPresets(this);
                    if (index < cur.size()) {
                        cur.set(index, new Prefs.TimerPreset(total, el.getText().toString().trim()));
                    }
                    java.util.Collections.sort(cur, (a, b) -> Integer.compare(a.seconds, b.seconds));
                    Prefs.setTimerPresets(this, cur);
                    renderPresets();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void addPresetDialog() {
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
        el.setHint("备注（如：泡面、煮蛋）");
        box.addView(row);
        box.addView(el);
        new android.app.AlertDialog.Builder(this)
                .setTitle("添加快捷时间")
                .setView(box)
                .setPositiveButton("添加", (d, w) -> {
                    int total = parseInt(eh.getText().toString()) * 3600
                            + parseInt(em.getText().toString()) * 60
                            + parseInt(es.getText().toString());
                    if (total <= 0) return;
                    java.util.List<Prefs.TimerPreset> list = Prefs.timerPresets(this);
                    list.add(new Prefs.TimerPreset(total, el.getText().toString().trim()));
                    java.util.Collections.sort(list,
                            (a, b) -> Integer.compare(a.seconds, b.seconds));
                    Prefs.setTimerPresets(this, list);
                    renderPresets();
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

    private String folderLabel(String uriStr) {
        if (uriStr == null || uriStr.isEmpty()) return "全部本地音乐";
        try {
            Uri uri = Uri.parse(uriStr);
            String docId = DocumentsContract.getTreeDocumentId(uri);
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId);
            try (Cursor c = getContentResolver().query(docUri,
                    new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    String name = c.getString(0);
                    if (name != null && !name.isEmpty()) return name;
                }
            }
            return docId;
        } catch (Exception e) {
            return "已选择文件夹";
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FOLDER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }
            folderUri = uri.toString();
            Prefs.setFolderUri(this, folderUri);
            tvFolder.setText(folderLabel(folderUri));
        }
        if (requestCode == REQ_SONG && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }
            defSongUri = uri.toString();
            Prefs.setSongUri(this, defSongUri);
            updateDefSongText();
        }
    }

    private void pickDefSong() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(i, REQ_SONG);
        } catch (Exception ignored) {
        }
    }

    private void updateDefSongText() {
        TextView tv = findViewById(R.id.tv_def_song);
        if (tv == null) return;
        if (defSongUri == null || defSongUri.isEmpty()) {
            tv.setText("指定歌曲：未指定（随机播放）");
        } else {
            tv.setText("指定歌曲：" + MusicLibrary.displayName(this, Uri.parse(defSongUri)));
        }
    }
}
