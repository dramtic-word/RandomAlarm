package com.dsh.randomalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.app.WallpaperColors;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Locale;

/**
 * 横 2 竖 1 的桌面时钟：显示时间与日期。
 * 点时间打开随机闹钟，点日期打开系统日历。
 *
 * RemoteViews 不会自己跳分钟，这里用精确闹钟对齐到下一个整分刷新一次；
 * 时间/时区变化、亮屏、解锁也会顺手刷新，弥补 Doze 下的延迟。
 */
public class ClockWidgetProvider extends AppWidgetProvider {
    static final String ACTION_TICK = "com.dsh.randomalarm.action.WIDGET_TICK";
    private static final int REQ_TICK = 7301;
    private static final int REQ_OPEN = 7302;
    private static final int REQ_CALENDAR = 7303;
    private static final String[] WEEKDAYS =
            {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            update(context, manager, id);
        }
        schedule(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                          int id, Bundle options) {
        update(context, manager, id);
    }

    @Override
    public void onEnabled(Context context) {
        schedule(context);
    }

    @Override
    public void onDisabled(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(tick(context));
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_TICK.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_SCREEN_ON.equals(action)
                || Intent.ACTION_USER_PRESENT.equals(action)) {
            refreshAll(context);
            schedule(context);
        }
    }

    private void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName me = new ComponentName(context, ClockWidgetProvider.class);
        for (int id : manager.getAppWidgetIds(me)) {
            update(context, manager, id);
        }
    }

    private void update(Context context, AppWidgetManager manager, int id) {
        Calendar now = Calendar.getInstance();
        boolean h24 = DateFormat.is24HourFormat(context);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        String time;
        String prefix = "";
        if (h24) {
            time = String.format(Locale.US, "%02d:%02d", hour, minute);
        } else {
            int h12 = hour % 12;
            if (h12 == 0) h12 = 12;
            time = String.format(Locale.US, "%d:%02d", h12, minute);
            prefix = hour < 12 ? "上午 " : "下午 ";
        }
        String date = prefix + (now.get(Calendar.MONTH) + 1) + "月"
                + now.get(Calendar.DAY_OF_MONTH) + "日 "
                + WEEKDAYS[now.get(Calendar.DAY_OF_WEEK) - 1];

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_clock);
        views.setTextViewText(R.id.widget_time, time);
        views.setTextViewText(R.id.widget_date, date);

        boolean darkText = prefersDarkText(context);
        views.setTextColor(R.id.widget_time, darkText ? 0xE6000000 : 0xFFFFFFFF);
        views.setTextColor(R.id.widget_date, darkText ? 0x99000000 : 0xCCFFFFFF);

        Bundle options = manager.getAppWidgetOptions(id);
        int height = 90;
        if (options != null) {
            height = Math.max(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0),
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0));
            if (height <= 0) height = 90;
        }
        float timeSp = height >= 66 ? 58f : 42f;
        views.setTextViewTextSize(R.id.widget_time, TypedValue.COMPLEX_UNIT_SP, timeSp);

        views.setOnClickPendingIntent(R.id.widget_root, openApp(context));
        views.setOnClickPendingIntent(R.id.widget_time, openApp(context));
        views.setOnClickPendingIntent(R.id.widget_date, openCalendar(context));
        manager.updateAppWidget(id, views);
    }

    /** 跟 MyOS 系统时钟一样：浅色壁纸用深色字，深色壁纸用白字。 */
    private boolean prefersDarkText(Context context) {
        if (Build.VERSION.SDK_INT >= 27) {
            try {
                WallpaperManager wm = WallpaperManager.getInstance(context);
                WallpaperColors colors = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
                if (colors != null) {
                    int hints = colors.getColorHints();
                    android.util.Log.d("ClockWidget", "wallpaper colors hints=" + hints
                            + " primary=" + Integer.toHexString(colors.getPrimaryColor().toArgb()));
                    // 实测 MyOS：hints=2 表示深色壁纸（用白字），hints=4 表示浅色壁纸（用深字）
                    if ((hints & WallpaperColors.HINT_SUPPORTS_DARK_THEME) != 0) {
                        return false;
                    }
                    if ((hints & (WallpaperColors.HINT_SUPPORTS_DARK_TEXT | 4)) != 0) {
                        return true;
                    }
                    double luminance = android.graphics.Color.luminance(colors.getPrimaryColor().toArgb());
                    return luminance > 0.5;
                }
                android.util.Log.d("ClockWidget", "wallpaper colors = null");
            } catch (Exception e) {
                android.util.Log.d("ClockWidget", "wallpaper colors failed: " + e);
            }
        }
        return false;
    }

    private PendingIntent openApp(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, REQ_OPEN, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** 有系统日历就打开日历；没有（或解析不到）就退回随机闹钟，避免点了没反应。 */
    private PendingIntent openCalendar(Context context) {
        Intent intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,
                Intent.CATEGORY_APP_CALENDAR);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (context.getPackageManager().resolveActivity(intent, 0) == null) {
            return openApp(context);
        }
        return PendingIntent.getActivity(context, REQ_CALENDAR, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent tick(Context context) {
        Intent intent = new Intent(context, ClockWidgetProvider.class).setAction(ACTION_TICK);
        return PendingIntent.getBroadcast(context, REQ_TICK, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void schedule(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long now = System.currentTimeMillis();
        long next = now - now % 60000L + 60000L;
        try {
            am.setExact(AlarmManager.RTC, next, tick(context));
        } catch (SecurityException e) {
            try {
                am.setWindow(AlarmManager.RTC, next, 120000L, tick(context));
            } catch (SecurityException ignored) {
            }
        }
    }
}
