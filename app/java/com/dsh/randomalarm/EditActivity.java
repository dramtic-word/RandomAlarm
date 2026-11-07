package com.dsh.randomalarm;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

public class EditActivity extends Activity {
    private static final int REQ_FOLDER = 200;
    private static final int REQ_SONG = 201;
    private long alarmId = -1;
    private Alarm alarm;
    private TimePicker picker;
    private final CheckBox[] dayBoxes = new CheckBox[7];
    private Switch swFade, swVibrate, swDelete, swSilent;
    private Spinner spSnooze, spFadeSec, spMaxVolume, spInitVolume, spRingTime, spTimeoutAction, spTimeoutCount, spMinRing;
    private EditText etLabel;
    private TextView tvFolder, tvSong;
    private String folderUri = "";
    private String songUri = "";
    private int snoozeVal = 5, fadeVal = 60, maxVolVal = 100, initVolVal = 15,
            ringVal = 0, minRingVal = 5, countVal = 1, timeoutActionVal = 0;
    private static final String[] DAY_NAMES = {"一", "二", "三", "四", "五", "六", "日"};
    private static final int[] SNOOZE = {5, 10, 15, 20, 30};
    private static final int[] FADE_SEC = {30, 60, 120, 300, 600};
    private static final String[] FADE_TEXT = {"30 秒", "1 分钟", "2 分钟", "5 分钟", "10 分钟"};
    private static final int[] VOL = {5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
    private static final int[] INIT_VOL = {5, 10, 15, 20, 30, 40, 50};
    private static final int[] RING_SEC = {0, 60, 120, 300, 600, 900};
    private static final String[] RING_TEXT = {"一直响", "1 分钟", "2 分钟", "5 分钟", "10 分钟", "15 分钟"};
    private static final String[] TIMEOUT_TEXT = {"到点停止", "到点贪睡"};
    private static final int[] TIMEOUT_COUNT = {1, 2, 3, 5};
    private static final int[] MIN_RING = {0, 3, 5, 10, 15};
    private static final String[] MIN_RING_TEXT = {"不限制", "3 秒", "5 秒", "10 秒", "15 秒"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.applyOrientation(this);
        setContentView(R.layout.activity_edit);
        picker = findViewById(R.id.picker);
        picker.setIs24HourView(true);
        swSilent = findViewById(R.id.sw_silent);
        swFade = findViewById(R.id.sw_fade);
        swVibrate = findViewById(R.id.sw_vibrate);
        swDelete = findViewById(R.id.sw_delete);
        spSnooze = findViewById(R.id.sp_snooze);
        spFadeSec = findViewById(R.id.sp_fade_sec);
        spMaxVolume = findViewById(R.id.sp_max_volume);
        spInitVolume = findViewById(R.id.sp_init_volume);
        spRingTime = findViewById(R.id.sp_ring_time);
        spTimeoutAction = findViewById(R.id.sp_timeout_action);
        spTimeoutCount = findViewById(R.id.sp_timeout_count);
        spMinRing = findViewById(R.id.sp_min_ring);
        etLabel = findViewById(R.id.et_label);
        tvFolder = findViewById(R.id.tv_folder);
        tvSong = findViewById(R.id.tv_song);
        findViewById(R.id.btn_pick_song).setOnClickListener(v -> pickSong());
        findViewById(R.id.btn_clear_song).setOnClickListener(v -> {
            songUri = "";
            updateSongText();
        });

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

        ArrayAdapter<String> minRingAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, MIN_RING_TEXT);
        minRingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMinRing.setAdapter(minRingAdapter);

        LinearLayout daysBox = findViewById(R.id.days_box);
        for (int i = 0; i < 7; i++) {
            CheckBox cb = new CheckBox(this);
            cb.setText(DAY_NAMES[i]);
            cb.setTextColor(0xFFF2F2F7);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFF7C4DFF));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            daysBox.addView(cb, lp);
            dayBoxes[i] = cb;
        }

        findViewById(R.id.btn_pick_folder).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            try {
                startActivityForResult(i, REQ_FOLDER);
            } catch (Exception ignored) {
            }
        });
        findViewById(R.id.btn_clear_folder).setOnClickListener(v -> {
            folderUri = "";
            tvFolder.setText("全部本地音乐");
        });

        alarmId = getIntent().getLongExtra("alarmId", -1);
        alarm = alarmId >= 0 ? AlarmStore.get(this, alarmId) : null;
        Button delete = findViewById(R.id.btn_delete);

        if (alarm != null) {
            snoozeVal = alarm.snoozeMinutes;
            fadeVal = alarm.fadeSeconds;
            maxVolVal = alarm.maxVolume;
            initVolVal = alarm.initialVolume;
            ringVal = alarm.ringSeconds;
            minRingVal = alarm.minRingSeconds;
            countVal = alarm.timeoutSnoozeCount;
            timeoutActionVal = alarm.timeoutAction;
            picker.setHour(alarm.hour);
            picker.setMinute(alarm.minute);
            for (int i = 0; i < 7; i++) dayBoxes[i].setChecked((alarm.daysMask & (1 << i)) != 0);
            swSilent.setChecked(alarm.silent);
            swFade.setChecked(alarm.fadeIn);
            swVibrate.setChecked(alarm.vibrate);
            swDelete.setChecked(alarm.deleteAfterRing);
            etLabel.setText(alarm.label);
            folderUri = alarm.folderUri == null ? "" : alarm.folderUri;
            tvFolder.setText(folderLabel(folderUri));
            songUri = alarm.songUri == null ? "" : alarm.songUri;
            updateSongText();
            for (int i = 0; i < SNOOZE.length; i++) {
                if (SNOOZE[i] == alarm.snoozeMinutes) spSnooze.setSelection(i);
            }
            for (int i = 0; i < FADE_SEC.length; i++) {
                if (FADE_SEC[i] == alarm.fadeSeconds) spFadeSec.setSelection(i);
            }
            for (int i = 0; i < VOL.length; i++) {
                if (VOL[i] == alarm.maxVolume) spMaxVolume.setSelection(i);
            }
            for (int i = 0; i < INIT_VOL.length; i++) {
                if (INIT_VOL[i] == alarm.initialVolume) spInitVolume.setSelection(i);
            }
            for (int i = 0; i < RING_SEC.length; i++) {
                if (RING_SEC[i] == alarm.ringSeconds) spRingTime.setSelection(i);
            }
            spTimeoutAction.setSelection(alarm.timeoutAction == 1 ? 1 : 0);
            for (int i = 0; i < TIMEOUT_COUNT.length; i++) {
                if (TIMEOUT_COUNT[i] == alarm.timeoutSnoozeCount) spTimeoutCount.setSelection(i);
            }
            for (int i = 0; i < MIN_RING.length; i++) {
                if (MIN_RING[i] == alarm.minRingSeconds) spMinRing.setSelection(i);
            }
            delete.setVisibility(View.VISIBLE);
            delete.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("删除闹钟")
                        .setMessage("确定删除 " + alarm.timeText() + " 这个闹钟吗？")
                        .setPositiveButton("删除", (d, w) -> {
                            AlarmStore.delete(this, alarmId);
                            AlarmScheduler.cancel(this, alarm);
                            finish();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
        } else {
            alarm = new Alarm();
            alarm.id = System.currentTimeMillis();
            alarm.hour = 7;
            alarm.minute = 0;
            snoozeVal = Prefs.snooze(this);
            fadeVal = Prefs.fadeSeconds(this);
            maxVolVal = Prefs.maxVolume(this);
            initVolVal = Prefs.initialVolume(this);
            ringVal = Prefs.ringSeconds(this);
            minRingVal = Prefs.minRingSeconds(this);
            countVal = Prefs.timeoutSnoozeCount(this);
            timeoutActionVal = Prefs.timeoutAction(this);
            picker.setHour(7);
            picker.setMinute(0);
            swSilent.setChecked(false);
            swFade.setChecked(Prefs.fadeIn(this));
            swVibrate.setChecked(Prefs.vibrate(this));
            swDelete.setChecked(false);
            int defSnooze = Prefs.snooze(this);
            for (int i = 0; i < SNOOZE.length; i++) {
                if (SNOOZE[i] == defSnooze) spSnooze.setSelection(i);
            }
            int defFade = Prefs.fadeSeconds(this);
            for (int i = 0; i < FADE_SEC.length; i++) {
                if (FADE_SEC[i] == defFade) spFadeSec.setSelection(i);
            }
            int defVol = Prefs.maxVolume(this);
            for (int i = 0; i < VOL.length; i++) {
                if (VOL[i] == defVol) spMaxVolume.setSelection(i);
            }
            int defInit = Prefs.initialVolume(this);
            for (int i = 0; i < INIT_VOL.length; i++) {
                if (INIT_VOL[i] == defInit) spInitVolume.setSelection(i);
            }
            int defRing = Prefs.ringSeconds(this);
            for (int i = 0; i < RING_SEC.length; i++) {
                if (RING_SEC[i] == defRing) spRingTime.setSelection(i);
            }
            spTimeoutAction.setSelection(Prefs.timeoutAction(this) == 1 ? 1 : 0);
            int defCount = Prefs.timeoutSnoozeCount(this);
            for (int i = 0; i < TIMEOUT_COUNT.length; i++) {
                if (TIMEOUT_COUNT[i] == defCount) spTimeoutCount.setSelection(i);
            }
            int defMinRing = Prefs.minRingSeconds(this);
            for (int i = 0; i < MIN_RING.length; i++) {
                if (MIN_RING[i] == defMinRing) spMinRing.setSelection(i);
            }
            folderUri = Prefs.folderUri(this);
            tvFolder.setText(folderLabel(folderUri));
            songUri = Prefs.songUri(this);
            updateSongText();
            delete.setVisibility(View.GONE);
        }

        swFade.setOnCheckedChangeListener((v, checked) -> updateFadeVisibility(checked));
        swSilent.setOnCheckedChangeListener((v, checked) -> updateSoundVisibility(checked));
        spTimeoutAction.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                updateTimeoutCountVisibility(position == 1);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        updateFadeVisibility(swFade.isChecked());
        updateSoundVisibility(swSilent.isChecked());
        updateTimeoutCountVisibility(spTimeoutAction.getSelectedItemPosition() == 1);

        NumberSpinner.bind(this, spSnooze, SNOOZE, "分钟", 1, 120, snoozeVal, v -> snoozeVal = v);
        NumberSpinner.bind(this, spFadeSec, FADE_SEC, "秒", 5, 600, fadeVal, v -> fadeVal = v);
        NumberSpinner.bind(this, spMaxVolume, VOL, "%", 5, 100, maxVolVal, v -> maxVolVal = v);
        NumberSpinner.bind(this, spInitVolume, INIT_VOL, "%", 5, 100, initVolVal, v -> initVolVal = v);
        NumberSpinner.bind(this, spRingTime, RING_SEC, "秒", 0, 3600, ringVal, v -> ringVal = v);
        NumberSpinner.bind(this, spMinRing, MIN_RING, "秒", 0, 60, minRingVal, v -> minRingVal = v);
        NumberSpinner.bind(this, spTimeoutCount, TIMEOUT_COUNT, "次", 1, 10, countVal, v -> countVal = v);

        findViewById(R.id.btn_save).setOnClickListener(v -> save());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFadeVisibility(swFade.isChecked());
        updateSoundVisibility(swSilent.isChecked());
        updateTimeoutCountVisibility(spTimeoutAction.getSelectedItemPosition() == 1);
    }

    private void updateSoundVisibility(boolean silent) {
        findViewById(R.id.sound_group).setVisibility(silent ? View.GONE : View.VISIBLE);
    }

    private void updateFadeVisibility(boolean on) {
        findViewById(R.id.fade_group).setVisibility(on ? View.VISIBLE : View.GONE);
    }

    private void updateTimeoutCountVisibility(boolean snooze) {
        int v = snooze ? View.VISIBLE : View.GONE;
        findViewById(R.id.label_timeout_count).setVisibility(v);
        findViewById(R.id.sp_timeout_count).setVisibility(v);
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
            tvFolder.setText(folderLabel(folderUri));
        }
        if (requestCode == REQ_SONG && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }
            songUri = uri.toString();
            updateSongText();
        }
    }

    private void pickSong() {
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

    private void updateSongText() {
        if (tvSong == null) return;
        if (songUri == null || songUri.isEmpty()) {
            tvSong.setText("指定歌曲：未指定（随机播放）");
        } else {
            tvSong.setText("指定歌曲：" + MusicLibrary.displayName(this, Uri.parse(songUri)));
        }
    }

    private void save() {
        alarm.hour = picker.getHour();
        alarm.minute = picker.getMinute();
        int mask = 0;
        for (int i = 0; i < 7; i++) if (dayBoxes[i].isChecked()) mask |= (1 << i);
        alarm.daysMask = mask;
        alarm.silent = swSilent.isChecked();
        alarm.minRingSeconds = minRingVal;
        alarm.fadeIn = swFade.isChecked();
        alarm.vibrate = swVibrate.isChecked();
        alarm.deleteAfterRing = swDelete.isChecked();
        alarm.fadeSeconds = fadeVal;
        alarm.maxVolume = maxVolVal;
        alarm.initialVolume = initVolVal;
        alarm.ringSeconds = ringVal;
        alarm.timeoutAction = timeoutActionVal;
        alarm.timeoutSnoozeCount = countVal;
        alarm.folderUri = folderUri;
        alarm.songUri = songUri;
        alarm.label = etLabel.getText().toString().trim();
        alarm.snoozeMinutes = snoozeVal;
        alarm.enabled = true;

        AlarmStore.save(this, alarm);
        AlarmScheduler.schedule(this, alarm);
        finish();
    }
}
