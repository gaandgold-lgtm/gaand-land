package kr.gaand.callback;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/** 알림의 [보내기] → 문자 앱을 문구가 채워진 채로 열어줌 (한 통의 장문으로 가서 링크 미리보기가 뜸) */
public class SendActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent i = getIntent();
        String num = i.getStringExtra("num");
        int k = i.getIntExtra("kind", 0);
        int nid = i.getIntExtra("nid", 0);
        try { getSystemService(NotificationManager.class).cancel(nid); } catch (Exception e) { }
        if (num != null) {
            Core.p(this).edit().putLong("sent_" + num, System.currentTimeMillis()).apply();
            Core.log(this, Core.fmt(num) + " → 문자 앱 열어줌 (전송 버튼을 눌러야 가요)");
            try {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + num))
                        .putExtra("sms_body", Core.msg(this, k)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                Core.log(this, "문자 앱을 열 수 없어요: " + e);
            }
        }
        finish();
    }
}
