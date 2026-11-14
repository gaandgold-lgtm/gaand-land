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
            String body = Core.msg(this, k);
            boolean sent = false;
            if (Core.p(this).getBoolean("img_on", true) && CardProvider.file(this).exists()) {
                try {
                    Uri u = CardProvider.uri(this);
                    Intent s = new Intent(Intent.ACTION_SEND).setType("image/jpeg")
                            .putExtra(Intent.EXTRA_STREAM, u).putExtra("address", num)
                            .putExtra("sms_body", body).putExtra(Intent.EXTRA_TEXT, body)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    s.setClipData(android.content.ClipData.newRawUri("명함", u));
                    String pkg = android.provider.Telephony.Sms.getDefaultSmsPackage(this);
                    if (pkg != null) s.setPackage(pkg);
                    startActivity(s);
                    sent = true;
                    Core.log(this, "명함 이미지를 붙여서 문자 앱을 열었어요");
                } catch (Exception e) {
                    Core.log(this, "이미지 첨부 실패, 글자만 보내요: " + e);
                }
            }
            if (!sent) try {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + num))
                        .putExtra("sms_body", body).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                Core.log(this, "문자 앱을 열 수 없어요: " + e);
            }
        }
        finish();
    }
}
