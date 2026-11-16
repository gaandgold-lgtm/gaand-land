package kr.gaand.routine;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import java.util.ArrayList;
import java.util.Calendar;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import org.json.JSONObject;

/** 웹 화면과 앱 사이 통로: 남은 루틴 개수 전달, 공유, 사진 저장 */
public class Bridge {
    final Activity a;
    Bridge(Activity a) { this.a = a; }

    @JavascriptInterface public void sync(String json) {
        try {
            JSONObject o = new JSONObject(json);
            SharedPreferences p = Reminder.p(a);
            boolean on = o.optBoolean("on", true);
            String time = o.optString("time", "21:00");
            if (!time.matches("([01]?\\d|2[0-3]):[0-5]?\\d")) time = "21:00";
            boolean changed = !p.contains("time") || on != p.getBoolean("on", true) || !time.equals(p.getString("time", ""));
            int rem = o.optInt("remaining", 0);
            p.edit().putBoolean("on", on).putString("time", time)
                    .putString("date", o.optString("date", ""))
                    .putInt("remaining", rem).putInt("total", o.optInt("total", 0))
                    .putString("names", o.optString("names", ""))
                    .putBoolean("active", o.optBoolean("active", false)).apply();
            if (rem == 0) Reminder.cancelNotice(a);
            if (changed || rem == 0) Reminder.schedule(a);
        } catch (Exception ignored) { }
    }

    /** 삼성 시계 앱 등 기본 시계 앱에 매일 반복 알람 만들기 */
    @JavascriptInterface public boolean setClockAlarm(int hh, int mm, String msg) {
        try {
            ArrayList<Integer> days = new ArrayList<>();
            for (int d = Calendar.SUNDAY; d <= Calendar.SATURDAY; d++) days.add(d);
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, hh)
                    .putExtra(AlarmClock.EXTRA_MINUTES, mm)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, msg)
                    .putExtra(AlarmClock.EXTRA_DAYS, days)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            a.runOnUiThread(() -> {
                try { a.startActivity(i); }
                catch (Exception e) { Toast.makeText(a, "시계 앱을 열 수 없어요. 앱 알람은 그대로 울려요", Toast.LENGTH_LONG).show(); }
            });
            return true;
        } catch (Exception e) { return false; }
    }

    static byte[] png(String dataUrl) {
        int i = dataUrl.indexOf(',');
        return Base64.decode(i >= 0 ? dataUrl.substring(i + 1) : dataUrl, Base64.DEFAULT);
    }

    @JavascriptInterface public boolean shareImage(String dataUrl, String text) {
        try {
            File f = ShareProvider.file(a);
            try (FileOutputStream out = new FileOutputStream(f)) { out.write(png(dataUrl)); }
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("image/png");
            s.putExtra(Intent.EXTRA_STREAM, ShareProvider.uri(a));
            s.putExtra(Intent.EXTRA_TEXT, text);
            s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent c = Intent.createChooser(s, "공유하기");
            c.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            a.runOnUiThread(() -> a.startActivity(c));
            return true;
        } catch (Exception e) { return false; }
    }

    @JavascriptInterface public boolean saveImage(String dataUrl, String name) {
        try {
            byte[] b = png(dataUrl);
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                v.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/가앤루틴");
                Uri u = a.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                try (OutputStream out = a.getContentResolver().openOutputStream(u)) { out.write(b); }
                a.runOnUiThread(() -> Toast.makeText(a, "갤러리에 저장했어요", Toast.LENGTH_SHORT).show());
                return true;
            }
            return shareImage(dataUrl, "");
        } catch (Exception e) { return false; }
    }
}
