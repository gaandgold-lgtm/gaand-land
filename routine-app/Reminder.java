package kr.gaand.routine;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** 매일 정한 시간에 남은 루틴을 알리고, 다 끝낼 때까지 1시간마다 다시 알림 (밤 11시까지) */
public class Reminder {
    static final String CH = "routine";
    static final String FIRE = "kr.gaand.routine.FIRE";

    static SharedPreferences p(Context c) { return c.getSharedPreferences("routine", Context.MODE_PRIVATE); }

    static void channel(Context c) {
        NotificationChannel ch = new NotificationChannel(CH, "루틴 알림", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("오늘 남은 루틴을 알려줘요");
        c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    static String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }

    static PendingIntent pi(Context c) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(FIRE);
        return PendingIntent.getBroadcast(c, 7, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static long mainTime(Context c, boolean allowToday) {
        String[] t = p(c).getString("time", "21:00").split(":");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(t[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(t[1]));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (!allowToday || cal.getTimeInMillis() <= System.currentTimeMillis() + 5000) cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }

    static void schedule(Context c) {
        try { setAt(c, mainTime(c, true)); } catch (Exception ignored) { }
    }

    static void setAt(Context c, long at) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = pi(c);
        am.cancel(pi);
        if (!p(c).getBoolean("on", true)) return;
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
    }

    static void cancelNotice(Context c) { c.getSystemService(NotificationManager.class).cancel(1); }

    static void fire(Context c) {
        SharedPreferences sp = p(c);
        boolean isToday = today().equals(sp.getString("date", ""));
        int n = isToday ? sp.getInt("remaining", 0) : sp.getInt("total", 0);
        if (sp.getBoolean("on", true) && sp.getBoolean("active", false) && n > 0) {
            String text = isToday && !sp.getString("names", "").isEmpty()
                    ? sp.getString("names", "") : "오늘 루틴을 체크해 주세요";
            Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent op = PendingIntent.getActivity(c, 1, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification nt = new Notification.Builder(c, CH)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle("아직 " + n + "개 남았어요")
                    .setContentText(text)
                    .setStyle(new Notification.BigTextStyle().bigText(text))
                    .setContentIntent(op)
                    .setAutoCancel(true)
                    .build();
            try { c.getSystemService(NotificationManager.class).notify(1, nt); } catch (Exception ignored) { }
            long nag = System.currentTimeMillis() + 60 * 60 * 1000L;
            Calendar lim = Calendar.getInstance();
            lim.set(Calendar.HOUR_OF_DAY, 23);
            lim.set(Calendar.MINUTE, 0);
            lim.set(Calendar.SECOND, 0);
            if (nag < lim.getTimeInMillis()) { setAt(c, nag); return; }
        }
        setAt(c, mainTime(c, false));
    }
}
