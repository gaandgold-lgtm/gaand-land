package kr.gaand.callback;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONObject;

/** 사용 기간: 사장님이 관리 페이지에서 정한 목록(lic.json)을 앱이 읽어서 확인 */
public class License {
    static final String URL_API = "https://api.github.com/repos/gaandgold-lgtm/gaand-land/contents/lic.json";
    static final String URL_LIST = "https://raw.githubusercontent.com/gaandgold-lgtm/gaand-land/main/lic.json";

    static String get(String u, boolean api) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(u).openConnection();
        try {
            con.setConnectTimeout(8000);
            con.setReadTimeout(8000);
            con.setUseCaches(false);
            con.setRequestProperty("Cache-Control", "no-cache");
            if (api) con.setRequestProperty("Accept", "application/vnd.github.raw");
            if (con.getResponseCode() != 200) throw new Exception("http " + con.getResponseCode());
            InputStream in = con.getInputStream();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toString("UTF-8");
        } finally {
            con.disconnect();
        }
    }

    static boolean need(Context c) { return c.getResources().getBoolean(R.bool.need_license); }

    static byte[] dev(Context c) {
        try {
            String id = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
            byte[] h = MessageDigest.getInstance("SHA-256").digest(((id == null ? "x" : id) + "|callcard").getBytes("UTF-8"));
            return new byte[]{h[0], h[1], h[2], h[3]};
        } catch (Exception e) {
            return new byte[4];
        }
    }

    static String devHex(Context c) {
        byte[] d = dev(c);
        return String.format("%02X%02X%02X%02X", d[0], d[1], d[2], d[3]);
    }

    static String devCode(Context c) {
        String s = devHex(c);
        return s.substring(0, 4) + "-" + s.substring(4);
    }

    static long epochDay(long ms) { return Math.floorDiv(ms + 9 * 3600 * 1000L, 86400000L); }

    static String dayStr(long epochDay) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(epochDay * 86400000L);
    }

    static long parseDay(String s) {
        try {
            String[] p = s.trim().replaceAll("[^0-9]+", "-").replaceAll("^-|-$", "").split("-");
            int y = Integer.parseInt(p[0]), m = Integer.parseInt(p[1]), d = Integer.parseInt(p[2]);
            java.util.Calendar cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.clear();
            cal.set(y, m - 1, d);
            return Math.floorDiv(cal.getTimeInMillis(), 86400000L);
        } catch (Exception e) {
            return -1;
        }
    }

    /** 네트워크에서 목록을 받아 이 기기의 만료일을 저장. 실패하면 예전 값 유지. */
    static synchronized boolean refresh(Context c) {
        if (!need(c)) return true;
        try {
            String body;
            try { body = get(URL_API + "?t=" + System.currentTimeMillis(), true); }
            catch (Exception ex) { body = get(URL_LIST + "?t=" + System.currentTimeMillis(), false); }
            JSONObject o = new JSONObject(body);
            JSONArray a = o.getJSONArray("list");
            String me = devHex(c);
            long exp = -1;
            for (int i = 0; i < a.length(); i++) {
                JSONObject r = a.getJSONObject(i);
                String d = r.optString("d", "").toUpperCase(Locale.ROOT).replaceAll("[^0-9A-F]", "");
                if (d.equals(me)) exp = Math.max(exp, parseDay(r.optString("e", "")));
            }
            Core.p(c).edit().putLong("licExp", exp).putLong("licChecked", System.currentTimeMillis()).putBoolean("licListed", exp >= 0).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 15분 넘게 확인 안 했으면 다시 확인 (백그라운드 스레드에서 호출) */
    static void refreshIfOld(Context c) {
        if (!need(c)) return;
        long last = Core.p(c).getLong("licChecked", 0);
        if (System.currentTimeMillis() - last > 15 * 60 * 1000L) refresh(c);
    }

    static boolean ok(Context c) {
        if (!need(c)) return true;
        SharedPreferences sp = Core.p(c);
        return sp.getLong("licExp", -1) >= epochDay(System.currentTimeMillis());
    }

    static String status(Context c) {
        SharedPreferences sp = Core.p(c);
        if (sp.getLong("licChecked", 0) == 0) return "사용 기간 확인 중… (인터넷 연결이 필요해요)";
        long ex = sp.getLong("licExp", -1);
        long today = epochDay(System.currentTimeMillis());
        if (ex >= today) return ex > today + 365 * 50 ? "✓ 사용 중 (기간 제한 없음)" : "✓ 사용 기간: " + dayStr(ex) + "까지";
        if (ex >= 0) return "사용 기간이 끝났어요 (" + dayStr(ex) + "). 아래 [구매 문의 문자 보내기]를 눌러주세요.";
        return "아직 등록되지 않은 기기예요. 아래 [구매 문의 문자 보내기]로 기기번호를 보내주세요.";
    }
}
