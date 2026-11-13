package kr.gaand.callback;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        int nid = i.getIntExtra("nid", 0);
        if ("SEND".equals(i.getAction())) {
            final String num = i.getStringExtra("num");
            final int k = i.getIntExtra("kind", 0);
            final PendingResult pr = goAsync();
            final Context a = c.getApplicationContext();
            nm.cancel(nid);
            new Thread(() -> {
                boolean ok = Core.send(a, num, Core.msg(a, k));
                done(a, nid, num, ok);
                pr.finish();
            }).start();
        } else {
            nm.cancel(nid);
        }
    }

    static void done(Context c, int nid, String num, boolean ok) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        {
            Notification n = new Notification.Builder(c, Core.CH)
                    .setSmallIcon(android.R.drawable.sym_action_chat)
                    .setContentTitle(ok ? "명함 문자 보냈어요 ✓" : "문자를 보내지 못했어요")
                    .setContentText(Core.fmt(num) + (ok ? "" : " · 앱에서 문자 권한을 확인해 주세요"))
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setTimeoutAfter(ok ? 5000 : 60000)
                    .build();
            nm.notify(nid, n);
        }
    }
}
