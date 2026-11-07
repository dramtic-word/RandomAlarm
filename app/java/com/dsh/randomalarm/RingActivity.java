package com.dsh.randomalarm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class RingActivity extends Activity {
    private long alarmId = -1;
    private Alarm alarm;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView timeView, songView, artistView;
    private android.widget.ImageView ringBg;
    private android.graphics.Bitmap lastCover;
    private View root;
    private View contentView;
    private android.widget.SeekBar seekBar;
    private TextView progressText;
    private View seekRow;
    private TextView lockHint;
    private boolean dragging;
    private boolean downSeek;
    private float downY, downX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.applyOrientation(this);
        setContentView(R.layout.activity_ring);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        android.app.KeyguardManager km =
                (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (km != null && km.isKeyguardLocked()) {
            if (Build.VERSION.SDK_INT >= 26) {
                km.requestDismissKeyguard(this, null);
            }
        }

        timeView = findViewById(R.id.ring_time);
        songView = findViewById(R.id.ring_song);
        artistView = findViewById(R.id.ring_artist);
        ringBg = findViewById(R.id.ring_bg);
        root = findViewById(R.id.ring_move);
        contentView = findViewById(R.id.ring_content);
        setupHints();
        if (getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            timeView.setTextSize(44);
            songView.setTextSize(17);
            artistView.setTextSize(13);
            ((TextView) findViewById(R.id.ring_label)).setTextSize(14);
        }
        ringBg.setAlpha(Prefs.coverBrightness(this) / 100f);

        seekBar = findViewById(R.id.ring_seek);
        progressText = findViewById(R.id.ring_progress_text);
        seekRow = findViewById(R.id.seek_row);
        lockHint = findViewById(R.id.lock_hint);
        seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar bar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar bar) {
                dragging = true;
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar bar) {
                dragging = false;
                Intent i = new Intent(RingActivity.this, AlarmService.class);
                i.setAction(AlarmService.ACTION_SEEK);
                i.putExtra("seekMs", bar.getProgress());
                try {
                    startService(i);
                } catch (Exception ignored) {
                }
            }
        });

        alarmId = getIntent().getLongExtra("alarmId", -1);
        alarm = AlarmStore.get(this, alarmId);
        boolean isTimer = getIntent().getBooleanExtra("timer", false);
        if (isTimer) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm",
                    java.util.Locale.getDefault());
            timeView.setText(sdf.format(new java.util.Date()));
            TextView timerLabel = findViewById(R.id.ring_label);
            String note = Prefs.timerLabel(this);
            timerLabel.setText(note.isEmpty() ? "计时器" : "计时器 · " + note);
            timerLabel.setVisibility(View.VISIBLE);
            TextView timerSnooze = findViewById(R.id.btn_snooze);
            timerSnooze.setText(getString(R.string.snooze, Prefs.snooze(this)));
        } else if (alarm != null) {
            timeView.setText(alarm.timeText());
            if (!alarm.label.isEmpty()) {
                ((TextView) findViewById(R.id.ring_label)).setText(alarm.label);
                findViewById(R.id.ring_label).setVisibility(View.VISIBLE);
            }
            TextView snooze = findViewById(R.id.btn_snooze);
            snooze.setText(getString(R.string.snooze, alarm.snoozeMinutes));
        }

        boolean silentRing = alarm != null ? alarm.silent : Prefs.timerSilent(this);
        if (silentRing) {
            seekRow.setVisibility(View.GONE);
        }

        findViewById(R.id.btn_stop).setOnClickListener(v -> {
            if (canOperate()) stopAlarm(false, 0);
        });
        android.widget.Button snoozeBtn = findViewById(R.id.btn_snooze);
        snoozeBtn.setOnClickListener(v -> {
            if (canOperate()) stopAlarm(true, defaultSnoozeMinutes());
        });
        snoozeBtn.setOnLongClickListener(v -> {
            if (canOperate()) showSnoozePicker();
            return true;
        });

        handler.post(refresh);
    }

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            songView.setText(AlarmService.currentTitle.isEmpty() ? getString(R.string.song_unknown) : AlarmService.currentTitle);
            artistView.setText(AlarmService.currentArtist);
            android.graphics.Bitmap cover = AlarmService.currentCover;
            if (cover != null && cover != lastCover) {
                ringBg.setImageBitmap(cover);
                lastCover = cover;
            }
            if (!dragging) {
                int dur = AlarmService.currentDuration;
                int pos = AlarmService.currentPosition;
                progressText.setText(formatTime(pos) + " / " + formatTime(dur));
                if (dur > 0) {
                    seekBar.setMax(dur);
                    seekBar.setProgress(pos);
                }
            }
            boolean can = canOperate();
            seekBar.setEnabled(can);
            long start = AlarmService.ringStartAt;
            int minSec = alarm != null ? alarm.minRingSeconds
                    : Prefs.minRingSeconds(RingActivity.this);
            if (start > 0 && minSec > 0) {
                long left = minSec * 1000L - (System.currentTimeMillis() - start);
                if (left > 0) {
                    lockHint.setVisibility(View.VISIBLE);
                    lockHint.setText("响铃 " + ((left + 999) / 1000) + " 秒后可操作");
                } else {
                    lockHint.setVisibility(View.GONE);
                }
            } else {
                lockHint.setVisibility(View.GONE);
            }
            handler.postDelayed(this, 600);
        }
    };

    private void stopAlarm(boolean snooze, int snoozeMinutes) {
        Intent svc = new Intent(this, AlarmService.class);
        svc.setAction(snooze ? AlarmService.ACTION_SNOOZE : AlarmService.ACTION_STOP);
        if (snooze) svc.putExtra("snoozeMinutes", snoozeMinutes);
        try {
            startService(svc);
        } catch (Exception ignored) {
        }
        if (snooze && alarm != null) {
            AlarmScheduler.snooze(this, alarm, snoozeMinutes);
        }
        finish();
    }

    private int defaultSnoozeMinutes() {
        return alarm != null ? alarm.snoozeMinutes : Prefs.snooze(this);
    }

    private void showSnoozePicker() {
        final int[] mins = {5, 10, 15, 20, 30};
        String[] items = new String[mins.length];
        for (int i = 0; i < mins.length; i++) items[i] = mins[i] + " 分钟";
        new android.app.AlertDialog.Builder(this)
                .setTitle("本次贪睡时长")
                .setItems(items, (d, which) -> stopAlarm(true, mins[which]))
                .show();
    }

    private boolean canOperate() {
        long start = AlarmService.ringStartAt;
        if (start <= 0) return true;
        int minSec = alarm != null ? alarm.minRingSeconds : Prefs.minRingSeconds(this);
        return System.currentTimeMillis() - start >= minSec * 1000L;
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                downY = ev.getY();
                downX = ev.getX();
                downSeek = inSeekBar(ev.getX(), ev.getY());
                break;
            case android.view.MotionEvent.ACTION_MOVE: {
                if (downSeek) break;
                float dy = ev.getY() - downY;
                float dxRaw = ev.getX() - downX;
                float dx = Math.abs(dxRaw);
                if (dx > Math.abs(dy) * 1.2f && dx > dp(8)) {
                    contentView.setTranslationX(dxRaw * 0.9f);
                    contentView.setAlpha(Math.max(0.4f,
                            1f - dx / (Math.max(1, root.getWidth()) * 1.2f)));
                } else if (Math.abs(dy) > dp(8) && dx < Math.abs(dy) * 1.2f) {
                    root.setTranslationY(dy * 0.9f);
                    int h = Math.max(1, root.getHeight());
                    root.setAlpha(Math.max(0.25f,
                            1f - Math.abs(dy) / (h * 0.9f)));
                }
                break;
            }
            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_CANCEL: {
                if (!canOperate()) {
                    rebound();
                    break;
                }
                float dy = ev.getY() - downY;
                float dx = ev.getX() - downX;
                boolean horizontal = Math.abs(dx) > dp(100)
                        && Math.abs(dx) > Math.abs(dy) * 1.2f;
                boolean vertical = Math.abs(dy) > dp(100)
                        && Math.abs(dy) > Math.abs(dx) * 1.2f;
                if (!downSeek && horizontal) {
                    performSwipe(Prefs.swipeAction(this, dx < 0 ? "left" : "right"),
                            dx, true);
                    return true;
                }
                if (!downSeek && vertical) {
                    performSwipe(Prefs.swipeAction(this, dy < 0 ? "up" : "down"),
                            dy, false);
                    return true;
                }
                rebound();
                break;
            }
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void setupHints() {
        ((TextView) findViewById(R.id.hint_up)).setText(
                "↑ 上滑" + actionName(Prefs.swipeAction(this, "up")));
        ((TextView) findViewById(R.id.hint_down)).setText(
                "↓ 下滑" + actionName(Prefs.swipeAction(this, "down")));
        ((TextView) findViewById(R.id.hint_left)).setText(
                "← 左滑" + actionName(Prefs.swipeAction(this, "left")));
        ((TextView) findViewById(R.id.hint_right)).setText(
                "右滑" + actionName(Prefs.swipeAction(this, "right")) + " →");
    }

    private String actionName(int a) {
        switch (a) {
            case Prefs.SWIPE_STOP:
                return "关闭";
            case Prefs.SWIPE_SNOOZE:
                return "贪睡";
            case Prefs.SWIPE_SKIP:
                return "切歌";
            default:
                return "禁止";
        }
    }

    private void performSwipe(int action, float delta, boolean horizontal) {
        switch (action) {
            case Prefs.SWIPE_STOP:
                animateClose(horizontal ? 0f : Math.signum(delta));
                break;
            case Prefs.SWIPE_SNOOZE:
                animateSnooze();
                break;
            case Prefs.SWIPE_SKIP:
                skipWithAnimation(horizontal ? delta
                        : (delta < 0 ? -dp(120) : dp(120)));
                break;
            default:
                rebound();
                break;
        }
    }

    private void animateClose(float dir) {
        int h = Math.max(1, root.getHeight());
        float ty = dir > 0 ? h : -h;
        root.animate().translationY(ty).alpha(0f).setDuration(220)
                .withEndAction(() -> stopAlarm(false, 0)).start();
    }

    private void animateSnooze() {
        root.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).setDuration(200)
                .withEndAction(() -> stopAlarm(true, defaultSnoozeMinutes())).start();
    }

    private void rebound() {
        root.animate().translationY(0f).alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(150).start();
        contentView.animate().translationX(0f).alpha(1f).setDuration(150).start();
    }

    private String formatTime(int ms) {
        int s = Math.max(0, ms / 1000);
        return String.format(java.util.Locale.getDefault(), "%d:%02d", s / 60, s % 60);
    }

    private void skipWithAnimation(float dx) {
        if (contentView == null) {
            sendSkip();
            return;
        }
        float w = Math.max(1, root.getWidth());
        float out = dx < 0 ? -w * 0.6f : w * 0.6f;
        contentView.animate().translationX(out).alpha(0f).setDuration(160)
                .withEndAction(() -> {
                    sendSkip();
                    contentView.setTranslationX(-out);
                    contentView.setAlpha(0f);
                    contentView.animate().translationX(0f).alpha(1f)
                            .setDuration(200).start();
                }).start();
    }

    private void sendSkip() {
        Intent i = new Intent(this, AlarmService.class);
        i.setAction(AlarmService.ACTION_SKIP);
        try {
            startService(i);
        } catch (Exception ignored) {
        }
    }

    private boolean inSeekBar(float x, float y) {
        if (seekBar == null) return false;
        int[] loc = new int[2];
        seekBar.getLocationInWindow(loc);
        return x >= loc[0] - dp(8) && x <= loc[0] + seekBar.getWidth() + dp(8)
                && y >= loc[1] - dp(8) && y <= loc[1] + seekBar.getHeight() + dp(8);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        // 必须按“停止”或“贪睡”，防止误触退出继续响
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(refresh);
        super.onDestroy();
    }
}
