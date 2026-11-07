package com.dsh.randomalarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

public class AlarmService extends Service {
    private static final String TAG = "AlarmService";
    public static final String ACTION_START = "com.dsh.randomalarm.START";
    public static final String ACTION_STOP = "com.dsh.randomalarm.STOP";
    public static final String ACTION_SNOOZE = "com.dsh.randomalarm.SNOOZE";
    public static final String ACTION_TIMER = "com.dsh.randomalarm.TIMER";
    public static final String ACTION_SKIP = "com.dsh.randomalarm.SKIP";
    public static final String ACTION_SEEK = "com.dsh.randomalarm.SEEK";
    public static final String EXTRA_ID = "alarmId";

    /** 给 RingActivity 读取的当前状态。 */
    public static volatile String currentTitle = "";
    public static volatile String currentArtist = "";
    public static volatile android.graphics.Bitmap currentCover;
    public static volatile int currentPosition = 0;
    public static volatile int currentDuration = 0;
    public static volatile boolean playing = false;
    public static volatile long ringStartAt = 0;

    private MediaPlayer player;
    private ParcelFileDescriptor pfd;
    private Vibrator vibrator;
    private Alarm alarm;
    private boolean isTimer;
    private int snoozeOverride = -1;
    private boolean playRandomStart = true;
    private boolean playFadeIn = true;
    private int fadeDurationSec = 60;
    private float fadeStepDelta = 0.01f;
    private float maxVolume = 1f;
    private int initialVolumePercent = 15;
    private int ringSeconds = 0;
    private int playStartMin = 15;
    private int playStartMax = 90;
    private final Runnable ringTimeout = () -> onRingTimeout();
    private PowerManager.WakeLock wakeLock;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private float volume = 0.15f;
    private final Runnable fadeStep = new Runnable() {
        @Override
        public void run() {
            if (player == null || !playing) return;
            volume = Math.min(maxVolume, volume + fadeStepDelta);
            try {
                player.setVolume(volume, volume);
            } catch (Exception ignored) {
            }
            if (volume < 1f) handler.postDelayed(this, 700);
        }
    };

    private final Runnable progressTick = new Runnable() {
        @Override
        public void run() {
            if (player != null && playing) {
                try {
                    currentPosition = player.getCurrentPosition();
                    currentDuration = player.getDuration();
                } catch (Exception ignored) {
                }
            }
            handler.postDelayed(this, 500);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            stopAlarm(false);
            return START_NOT_STICKY;
        }
        if (ACTION_SNOOZE.equals(action)) {
            snoozeOverride = intent != null ? intent.getIntExtra("snoozeMinutes", -1) : -1;
            stopAlarm(true);
            return START_NOT_STICKY;
        }
        if (ACTION_SKIP.equals(action)) {
            startPlayback();
            return START_STICKY;
        }
        if (ACTION_SEEK.equals(action)) {
            int ms = intent != null ? intent.getIntExtra("seekMs", 0) : 0;
            if (player != null) {
                try {
                    player.seekTo(ms);
                    currentPosition = ms;
                } catch (Exception ignored) {
                }
            }
            return START_STICKY;
        }
        if (ACTION_TIMER.equals(action)) {
            isTimer = true;
            alarm = null;
            String tl = intent != null ? intent.getStringExtra("timerLabel") : null;
            if (tl != null) Prefs.setTimerLabel(this, tl);
            acquireWakeLock();
            startVibration();
            goForeground();
            ringStartAt = System.currentTimeMillis();
            startPlayback();
            return START_NOT_STICKY;
        }

        long id = intent != null ? intent.getLongExtra(EXTRA_ID, -1) : -1;
        alarm = AlarmStore.get(this, id);
        NotificationManager nm0 = getSystemService(NotificationManager.class);
        if (nm0 != null && id >= 0) nm0.cancel((int) (1000 + (id & 0x7FFFFFFF)));
        acquireWakeLock();
        startVibration();
        goForeground();
        ringStartAt = System.currentTimeMillis();
        startPlayback();
        return START_STICKY;
    }

    private void acquireWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) return;
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm == null) return;
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "randomalarm:ring");
        wakeLock.acquire(10 * 60 * 1000L);
    }

    private void releaseWakeLock() {
        if (wakeLock != null) {
            if (wakeLock.isHeld()) wakeLock.release();
            wakeLock = null;
        }
    }

    private void startVibration() {
        boolean vibrate = alarm != null ? alarm.vibrate : Prefs.vibrate(this);
        if (!vibrate) return;
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = {0, 600, 600};
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
        } else {
            vibrator.vibrate(pattern, 0);
        }
    }

    private void startFade() {
        int steps = Math.max(1, (int) (fadeDurationSec * 1000L / 700L));
        float startVol = maxVolume * initialVolumePercent / 100f;
        fadeStepDelta = (maxVolume - startVol) / steps;
        volume = startVol;
        handler.removeCallbacks(fadeStep);
        handler.postDelayed(fadeStep, 700);
    }

    private void stopVibration() {
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
    }

    private void createChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        try {
            nm.deleteNotificationChannel("alarm");
        } catch (Exception ignored) {
        }
        NotificationChannel ch = new NotificationChannel("alarm_silent", "闹钟",
                NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("闹钟响铃（不弹出）");
        ch.setSound(null, null);
        ch.enableVibration(false);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    private void goForeground() {
        Intent full = new Intent(this, RingActivity.class);
        full.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 1, full,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(this, "alarm_silent")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(isTimer ? "计时器" : "闹钟")
                .setContentText(isTimer ? "时间到了" : (alarm != null ? alarm.timeText() : "时间到了"))
                .setOnlyAlertOnce(true)
                .setContentIntent(pi)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, n);
        }

        // 尝试直接拉起响铃界面；被系统拦截时靠全屏通知兜底
        try {
            Intent ring = new Intent(this, RingActivity.class);
            ring.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ring.putExtra("alarmId", alarm != null ? alarm.id : -1);
            ring.putExtra("timer", isTimer);
            startActivity(ring);
        } catch (Exception ignored) {
        }
    }

    private void startPlayback() {
        stopPlayer();
        handler.removeCallbacks(ringTimeout);
        currentPosition = 0;
        handler.removeCallbacks(progressTick);
        handler.post(progressTick);
        if (alarm != null && alarm.silent) {
            currentTitle = "静音闹钟";
            currentArtist = "只震动，不发声";
            HistoryStore.add(this, "静音", "静音响铃");
            if (alarm.ringSeconds > 0) {
                handler.postDelayed(ringTimeout, alarm.ringSeconds * 1000L);
            }
            return;
        }
        playRandomStart = Prefs.randomStart(this);
        playStartMin = Prefs.randomStartMin(this);
        playStartMax = Prefs.randomStartMax(this);
        playFadeIn = alarm != null ? alarm.fadeIn : Prefs.fadeIn(this);
        fadeDurationSec = alarm != null ? alarm.fadeSeconds : Prefs.fadeSeconds(this);
        maxVolume = (alarm != null ? alarm.maxVolume : Prefs.maxVolume(this)) / 100f;
        initialVolumePercent = alarm != null ? alarm.initialVolume : Prefs.initialVolume(this);
        ringSeconds = alarm != null ? alarm.ringSeconds : Prefs.ringSeconds(this);
        String folder = alarm != null ? alarm.folderUri : Prefs.folderUri(this);
        String song = alarm != null ? alarm.songUri : Prefs.songUri(this);
        Uri uri;
        if (song != null && !song.isEmpty()) {
            uri = Uri.parse(song);
            currentTitle = MusicLibrary.displayName(this, uri);
            currentArtist = "指定歌曲";
        } else {
            MusicLibrary.Track t = MusicLibrary.random(this, folder);
            if (t != null) {
                uri = t.uri;
                currentTitle = t.title;
                currentArtist = t.artist;
            } else {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                currentTitle = "默认闹铃";
                currentArtist = "没找到音乐文件";
            }
        }
        if (uri == null) {
            HistoryStore.add(this, "失败", "没有可播放的音频");
            stopSelf();
            return;
        }
        HistoryStore.add(this, "播放", currentTitle
                + (currentArtist.isEmpty() ? "" : " / " + currentArtist));
        currentCover = null;
        loadCoverAsync(uri);
        playUri(uri, true);
        if (ringSeconds > 0) {
            handler.removeCallbacks(ringTimeout);
            handler.postDelayed(ringTimeout, ringSeconds * 1000L);
        }
    }

    /** 后台读取当前音频的内嵌封面，供响铃界面当背景。 */
    private void loadCoverAsync(Uri uri) {
        new Thread(() -> {
            android.graphics.Bitmap bmp = null;
            android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
            try {
                mmr.setDataSource(this, uri);
                byte[] data = mmr.getEmbeddedPicture();
                if (data != null) {
                    bmp = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.length);
                }
            } catch (Exception ignored) {
            } finally {
                try {
                    mmr.release();
                } catch (Exception ignored) {
                }
            }
            if (bmp != null) currentCover = bmp;
        }).start();
    }

    /** 用文件描述符打开，避免 setDataSource(Context, Uri) 在后台启动时解析失败。 */
    private void playUri(Uri uri, boolean allowFallback) {
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            pfd = getContentResolver().openFileDescriptor(uri, "r");
            if (pfd == null) throw new IllegalStateException("cannot open " + uri);
            player.setDataSource(pfd.getFileDescriptor());
            player.setOnPreparedListener(mp -> {
                playing = true;
                volume = playFadeIn ? maxVolume * 0.15f : maxVolume;
                mp.setVolume(volume, volume);
                if (playRandomStart) {
                    int start = MusicLibrary.randomStartMs(mp.getDuration(), playStartMin, playStartMax);
                    if (start > 0) mp.seekTo(start);
                }
                mp.start();
                if (playFadeIn) startFade();
            });
            player.setOnCompletionListener(mp -> startPlayback());
            player.setOnErrorListener((mp, what, extra) -> {
                startPlayback();
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "play failed: " + uri, e);
            HistoryStore.add(this, "失败", "播放出错：" + e.getClass().getSimpleName());
            stopPlayer();
            if (allowFallback) {
                Uri fb = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (fb == null) fb = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                if (fb != null) {
                    currentTitle = "默认闹铃";
                    currentArtist = "音乐文件打不开，已用系统铃声";
                    playUri(fb, false);
                    return;
                }
            }
            stopSelf();
        }
    }

    private void stopPlayer() {
        playing = false;
        handler.removeCallbacks(fadeStep);
        handler.removeCallbacks(progressTick);
        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {
            }
            player.release();
            player = null;
        }
        if (pfd != null) {
            try {
                pfd.close();
            } catch (Exception ignored) {
            }
            pfd = null;
        }
    }

    private void onRingTimeout() {
        HistoryStore.add(this, "到点", "响铃时长结束");
        if (alarm != null && alarm.timeoutAction == 1) {
            int left = Prefs.timeoutLeft(this, alarm.id);
            if (left < 0) left = alarm.timeoutSnoozeCount;
            if (left > 0) {
                left--;
                Prefs.setTimeoutLeft(this, alarm.id, left);
                AlarmScheduler.snooze(this, alarm, alarm.snoozeMinutes);
                stopPlayingOnly();
                return;
            }
        }
        stopAlarm(false);
    }

    /** 停止播放并结束服务，但不改闹钟状态（用于到点后自动贪睡）。 */
    private void stopPlayingOnly() {
        stopPlayer();
        handler.removeCallbacks(ringTimeout);
        stopVibration();
        releaseWakeLock();
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(1);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void scheduleTimerSnooze(int minutes) {
        long at = System.currentTimeMillis() + Math.max(1, minutes) * 60_000L;
        String label = Prefs.timerLabel(this);
        TimerStore.Item it = new TimerStore.Item();
        it.id = System.currentTimeMillis() * 1000L + (long) (Math.random() * 1000);
        it.at = at;
        it.label = label.isEmpty() ? "贪睡" : "贪睡 · " + label;
        TimerStore.add(this, it);
        Intent i = new Intent(this, TimerReceiver.class);
        i.setAction(ACTION_TIMER);
        i.putExtra("timerId", it.id);
        i.putExtra("timerLabel", it.label);
        PendingIntent pi = PendingIntent.getBroadcast(this, (int) (it.id ^ (it.id >>> 32)), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, null), pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
        Prefs.setTimerAt(this, at);
    }

    private void stopAlarm(boolean snooze) {
        int sm = snoozeOverride > 0 ? snoozeOverride
                : (alarm != null ? alarm.snoozeMinutes : Prefs.snooze(this));
        snoozeOverride = -1;
        String hisText = isTimer ? "计时器" : "";
        if (snooze) {
            long target = System.currentTimeMillis() + sm * 60_000L;
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat(
                    "HH:mm", java.util.Locale.getDefault());
            hisText = fmt.format(new java.util.Date(target))
                    + (isTimer ? " 计时器" : "");
        }
        HistoryStore.add(this, snooze ? "贪睡" : "停止", hisText);
        stopPlayer();
        handler.removeCallbacks(ringTimeout);
        stopVibration();
        releaseWakeLock();
        if (alarm != null) Prefs.setTimeoutLeft(this, alarm.id, -1);
        if (isTimer) {
            if (snooze) scheduleTimerSnooze(sm);
            else Prefs.setTimerLabel(this, "");
        } else if (snooze && alarm != null) {
            AlarmScheduler.snooze(this, alarm, sm);
        } else if (alarm != null) {
            if (alarm.deleteAfterRing) {
                HistoryStore.add(this, "删除", "响后删除闹钟 " + alarm.timeText());
                AlarmStore.delete(this, alarm.id);
                AlarmScheduler.cancel(this, alarm);
            } else {
                AlarmScheduler.afterFired(this, alarm);
            }
        }
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.cancel(1);
            if (alarm != null) nm.cancel((int) (1000 + (alarm.id & 0x7FFFFFFF)));
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopPlayer();
        stopVibration();
        releaseWakeLock();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
