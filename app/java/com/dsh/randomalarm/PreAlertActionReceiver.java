package com.dsh.randomalarm;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 处理“提前通知”上的“停止 / 贪睡”按钮。 */
public class PreAlertActionReceiver extends BroadcastReceiver {
    public static final String ACTION_CANCEL = "com.dsh.randomalarm.PRE_CANCEL";
    public static final String ACTION_SNOOZE = "com.dsh.randomalarm.PRE_SNOOZE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        Context app = context.getApplicationContext();
        long id = intent.getLongExtra("alarmId", -1);
        Alarm a = AlarmStore.get(app, id);
        NotificationManager nm = (NotificationManager) app.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel((int) (1000 + (id & 0x7FFFFFFF)));
        if (a == null) return;

        long at = AlarmScheduler.nextTrigger(a, System.currentTimeMillis());
        a.skipAt = at;
        AlarmStore.save(app, a);

        if (ACTION_SNOOZE.equals(intent.getAction())) {
            AlarmScheduler.snooze(app, a, a.snoozeMinutes);
            HistoryStore.add(app, "贪睡", a.timeText() + " 提前");
        } else {
            AlarmScheduler.schedule(app, a);
            HistoryStore.add(app, "停止", a.timeText() + " 提前取消");
        }
    }
}
