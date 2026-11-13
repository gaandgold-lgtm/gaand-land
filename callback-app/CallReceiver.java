package kr.gaand.callback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class CallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String act = i.getAction();
        final Context a = c.getApplicationContext();
        if (Intent.ACTION_BOOT_COMPLETED.equals(act) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(act)) {
            LogJob.schedule(a);
            return;
        }
        if (TelephonyManager.EXTRA_STATE_IDLE.equals(i.getStringExtra(TelephonyManager.EXTRA_STATE))) {
            final PendingResult r = goAsync();
            new Thread(() -> {
                try { Thread.sleep(2500); } catch (InterruptedException e) { }
                Core.check(a);
                r.finish();
            }).start();
        }
    }
}
