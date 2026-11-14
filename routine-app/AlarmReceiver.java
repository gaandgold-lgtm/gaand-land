package kr.gaand.routine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 알림 시간이 되면 알리고, 폰을 다시 켜거나 앱을 업데이트하면 알림을 다시 걸어둠 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (Reminder.FIRE.equals(i.getAction())) Reminder.fire(c);
        else Reminder.schedule(c);
    }
}
