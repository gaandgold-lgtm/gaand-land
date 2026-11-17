package kr.gaand.routine;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
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
    static final String CH = "routine_alarm";
    static final String DISMISS = "kr.gaand.routine.DISMISS";
    static final String FIRE = "kr.gaand.routine.FIRE";

    static SharedPreferences p(Context c) { return c.getSharedPreferences("routine", Context.MODE_PRIVATE); }

    static void channel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        try { nm.deleteNotificationChannel("routine"); } catch (Exception ignored) { }
        NotificationChannel ch = new NotificationChannel(CH, "루틴 알람", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("남은 루틴이 있으면 알람처럼 울려요");
        ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 800, 400, 800, 400, 800});
        ch.setBypassDnd(true);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    static String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }

    static PendingIntent pi(Context c) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(FIRE);
        return PendingIntent.getBroadcast(c, 7, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static long mainTime(Context c, boolean allowToday) {
        int hh = 21, mm = 0;
        try {
            String[] t = p(c).getString("time", "21:00").split(":");
            hh = Integer.parseInt(t[0].trim());
            mm = Integer.parseInt(t[1].trim());
        } catch (Exception ignored) { hh = 21; mm = 0; }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hh);
        cal.set(Calendar.MINUTE, mm);
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
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            return;
        }
        Intent open = new Intent(c, MainActivity.class);
        PendingIntent show = PendingIntent.getActivity(c, 2, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show), pi);
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
            PendingIntent off = PendingIntent.getBroadcast(c, 8, new Intent(c, AlarmReceiver.class).setAction(DISMISS),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification nt = new Notification.Builder(c, CH)
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .addAction(new Notification.Action.Builder(null, "알람 끄기", off).build())
                    .addAction(new Notification.Action.Builder(null, "체크하러 가기", op).build())
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle("아직 " + n + "개 남았어요")
                    .setContentText(text)
                    .setStyle(new Notification.BigTextStyle().bigText(text))
                    .setContentIntent(op)
                    .setAutoCancel(true)
                    .build();
            nt.flags |= Notification.FLAG_INSISTENT;
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
