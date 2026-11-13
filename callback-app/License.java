package kr.gaand.callback;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class License {
    static final String PUB = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEVrKwfSBMLCoz8odds0Fbh9+hvNeqITnlxNr+GEw/rZ4Y52GafTqSNx9TC81bMjW2FV76Z7FN4Lgbg5K/vFa/mg==";
    static final int TRIAL_DAYS = 7;

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

    static String devCode(Context c) {
        byte[] d = dev(c);
        String s = String.format("%02X%02X%02X%02X", d[0], d[1], d[2], d[3]);
        return s.substring(0, 4) + "-" + s.substring(4);
    }

    static long epochDay(long ms) { return Math.floorDiv(ms + 9 * 3600 * 1000L, 86400000L); }

    static final long BASE = 20454; // 2026-01-01 as days since 1970-01-01

    static String dayStr(long epochDay) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(epochDay * 86400000L);
    }

    static byte[] p1363ToDer(byte[] rs) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] r = intDer(rs, 0), s = intDer(rs, 32);
        o.write(0x30);
        o.write(r.length + s.length);
        o.write(r, 0, r.length);
        o.write(s, 0, s.length);
        return o.toByteArray();
    }

    static byte[] intDer(byte[] a, int off) {
        int i = off, end = off + 32;
        while (i < end - 1 && a[i] == 0) i++;
        boolean pad = (a[i] & 0x80) != 0;
        int len = end - i + (pad ? 1 : 0);
        byte[] out = new byte[len + 2];
        out[0] = 0x02;
        out[1] = (byte) len;
        int p = 2;
        if (pad) out[p++] = 0;
        System.arraycopy(a, i, out, p, end - i);
        return out;
    }

    /** returns expiry epochDay if valid for this device, else -1 */
    static long check(Context c, String code) {
        try {
            String t = code.trim().replaceAll("\\s", "");
            int k = t.indexOf("#");
            if (k >= 0) t = t.substring(k + 1);
            byte[] b = Base64.decode(t, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            if (b.length != 70) return -1;
            byte[] d = dev(c);
            for (int i = 0; i < 4; i++) if (b[i] != d[i]) return -1;
            byte[] msg = new byte[6];
            System.arraycopy(b, 0, msg, 0, 6);
            byte[] sig = new byte[64];
            System.arraycopy(b, 6, sig, 0, 64);
            PublicKey pk = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.decode(PUB, Base64.DEFAULT)));
            Signature v = Signature.getInstance("SHA256withECDSA");
            v.initVerify(pk);
            v.update(msg);
            if (!v.verify(p1363ToDer(sig))) return -1;
            int days = ((b[4] & 0xFF) << 8) | (b[5] & 0xFF);
            return BASE + days;
        } catch (Exception e) {
            return -1;
        }
    }

    static long trialLeft(Context c) {
        long first;
        try { first = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).firstInstallTime; }
        catch (Exception e) { first = System.currentTimeMillis(); }
        long used = epochDay(System.currentTimeMillis()) - epochDay(first);
        return TRIAL_DAYS - used;
    }

    static long expiry(Context c) {
        String code = Core.p(c).getString("lic", "");
        return code.length() == 0 ? -1 : check(c, code);
    }

    static boolean ok(Context c) {
        if (!need(c)) return true;
        long today = epochDay(System.currentTimeMillis());
        if (expiry(c) >= today) return true;
        return trialLeft(c) > 0;
    }

    static String status(Context c) {
        long today = epochDay(System.currentTimeMillis());
        long ex = expiry(c);
        if (ex >= today) return "✓ 사용 기간: " + dayStr(ex) + "까지";
        long t = trialLeft(c);
        if (t > 0) return "무료 체험 " + t + "일 남았어요";
        return ex >= 0 ? "사용 기간이 끝났어요 (" + dayStr(ex) + "). 사용 코드를 새로 받아주세요." : "무료 체험이 끝났어요. 사용 코드를 받아 넣어주세요.";
    }
}
