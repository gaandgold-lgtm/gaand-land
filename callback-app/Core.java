package kr.gaand.callback;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.service.notification.StatusBarNotification;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.telephony.SmsManager;
import java.util.ArrayList;

public class Core {
    static final String CH = "callback_ask2";
    static final String D_IN = "안녕하세요, 가앤부동산공인중개사사무소 가현지입니다.\n연락 주셔서 감사합니다. 명함 보내드려요.\nland.gaandgold.co.kr/card.html\n궁금하신 점은 언제든 편하게 연락 주세요.";
    static final String D_OUT = "안녕하세요, 방금 통화드린 가앤부동산공인중개사사무소 가현지입니다.\n명함 남겨드려요. 번호 저장해 두시면 편하세요.\nland.gaandgold.co.kr/card.html";
    static final String D_MISS = "안녕하세요, 가앤부동산공인중개사사무소 가현지입니다.\n지금 상담 중이라 전화를 못 받았어요. 확인하는 대로 바로 연락드릴게요.\n집을 내놓으실 거면 아래 링크로 1분이면 접수돼요.\nland.gaandgold.co.kr/sell.html";

    static SharedPreferences p(Context c) { return c.getSharedPreferences("cfg", 0); }
    static String key(int k) { return k == 0 ? "in" : k == 1 ? "out" : "miss"; }
    static String label(int k) { return k == 0 ? "받은 전화" : k == 1 ? "건 전화" : "부재중 전화"; }
    static String msg(Context c, int k) {
        String d = k == 0 ? D_IN : k == 1 ? D_OUT : D_MISS;
        return p(c).getString("m_" + key(k), d);
    }
    static boolean on(Context c, int k) { return p(c).getBoolean("on_" + key(k), true); }

    static void log(Context c, String s) {
        String t = new SimpleDateFormat("M/d HH:mm:ss", Locale.KOREA).format(new Date());
        String old = p(c).getString("log", "");
        String n = t + "  " + s + "\n" + old;
        if (n.length() > 1500) n = n.substring(0, 1500);
        p(c).edit().putString("log", n).apply();
    }

    static String diag(Context c, int nid) {
        StringBuilder s = new StringBuilder();
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            s.append(nm.areNotificationsEnabled() ? "✓ 앱 알림 허용됨\n" : "✗ 앱 알림이 꺼져 있어요 → [알림 설정 열기]\n");
            NotificationChannel ch = nm.getNotificationChannel(CH);
            if (ch == null) s.append("✗ 알림 종류가 없어요\n");
            else if (ch.getImportance() < NotificationManager.IMPORTANCE_DEFAULT) s.append("✗ '명함 문자 확인' 알림이 꺼짐/조용히로 돼 있어요 → [알림 설정 열기]\n");
            else if (ch.getImportance() < NotificationManager.IMPORTANCE_HIGH) s.append("△ 팝업 없이 알림창에만 와요 → [알림 설정 열기]에서 '팝업'으로\n");
            else s.append("✓ '명함 문자 확인' 알림 팝업 켜짐\n");
            if (nid != 0) {
                boolean on = false;
                for (StatusBarNotification sb : nm.getActiveNotifications()) if (sb.getId() == nid) on = true;
                s.append(on ? "✓ 테스트 알림이 올라갔어요 (화면 맨 위를 아래로 쓸어내려 보세요)\n" : "✗ 테스트 알림이 올라가지 않았어요\n");
            }
        } catch (Exception e) {
            s.append("오류: ").append(e.toString()).append("\n");
        }
        String err = p(c).getString("err", "");
        if (err.length() > 0) s.append("마지막 오류: ").append(err).append("\n");
        return s.toString();
    }

    static void channel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CH, "명함 문자 확인", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("통화가 끝나면 명함 문자를 보낼지 묻는 알림");
        nm.createNotificationChannel(ch);
    }

    static String clean(String n) {
        if (n == null) return "";
        String d = n.replaceAll("[^0-9]", "");
        if (d.startsWith("82") && d.length() >= 11) d = "0" + d.substring(2);
        return d;
    }

    static String fmt(String n) {
        if (n.length() == 11) return n.substring(0, 3) + "-" + n.substring(3, 7) + "-" + n.substring(7);
        if (n.length() == 10) return n.substring(0, 3) + "-" + n.substring(3, 6) + "-" + n.substring(6);
        return n;
    }

    static synchronized void check(Context c) {
        if (c.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return;
        Cursor cur = null;
        try {
            cur = c.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.TYPE,
                            CallLog.Calls.DURATION, CallLog.Calls.DATE, CallLog.Calls.CACHED_NAME},
                    null, null, CallLog.Calls.DATE + " DESC");
            if (cur == null || !cur.moveToFirst()) return;
            long id = cur.getLong(0);
            String num = clean(cur.getString(1));
            int type = cur.getInt(2);
            long dur = cur.getLong(3);
            long date = cur.getLong(4);
            String name = cur.getString(5);
            SharedPreferences sp = p(c);
            if (id <= sp.getLong("lastId", 0)) return;
            sp.edit().putLong("lastId", id).apply();
            if (System.currentTimeMillis() - (date + dur * 1000) > 10 * 60 * 1000L) return;
            int k;
            if (type == CallLog.Calls.INCOMING_TYPE) k = dur > 0 ? 0 : 2;
            else if (type == CallLog.Calls.OUTGOING_TYPE) { if (dur <= 0) return; k = 1; }
            else if (type == CallLog.Calls.MISSED_TYPE || type == 5) k = 2;
            else return;
            if (!on(c, k)) { log(c, label(k) + " → 꺼져 있어 건너뜀"); return; }
            if (!num.startsWith("01") || num.length() < 10) { log(c, label(k) + " " + num + " → 휴대폰 번호 아님, 건너뜀"); return; }
            if (sp.getBoolean("once", true)
                    && System.currentTimeMillis() - sp.getLong("sent_" + num, 0) < 24 * 3600 * 1000L) { log(c, fmt(num) + " → 오늘 이미 보냄, 건너뜀"); return; }
            ask(c, num, name, k, (int) (id % 100000) + 1);
        } catch (Exception e) {
            log(c, "통화 확인 오류: " + e);
        } finally {
            if (cur != null) cur.close();
        }
    }

    static void ask(Context c, String num, String name, int k, int nid) {
        try {
            askInner(c, num, name, k, nid);
            log(c, label(k) + " " + fmt(num) + " → 알림 띄움");
        } catch (Exception e) {
            p(c).edit().putString("err", e.toString()).apply();
            log(c, "알림 실패: " + e);
        }
    }

    @SuppressWarnings("deprecation")
    static void askInner(Context c, String num, String name, int k, int nid) {
        channel(c);
        String who = (name != null && name.length() > 0 ? name + " · " : "") + fmt(num);
        int fl = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent s = new Intent(c, ActionReceiver.class).setAction("SEND")
                .putExtra("num", num).putExtra("kind", k).putExtra("nid", nid);
        PendingIntent ps = PendingIntent.getBroadcast(c, nid * 3, s, fl);
        Intent x = new Intent(c, ActionReceiver.class).setAction("SKIP").putExtra("nid", nid);
        PendingIntent px = PendingIntent.getBroadcast(c, nid * 3 + 1, x, fl);
        Intent e = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + num))
                .putExtra("sms_body", msg(c, k)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pe = PendingIntent.getActivity(c, nid * 3 + 2, e, fl);
        Notification n = new Notification.Builder(c, CH)
                .setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle("명함 문자 보낼까요? (" + label(k) + ")")
                .setContentText(who)
                .setStyle(new Notification.BigTextStyle().bigText(who + "\n\n" + msg(c, k)))
                .setColor(0xFF4A2F6B)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setPriority(Notification.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_ALL)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentIntent(pe)
                .addAction(new Notification.Action.Builder(null, "보내기", ps).build())
                .addAction(new Notification.Action.Builder(null, "고쳐서 보내기", pe).build())
                .addAction(new Notification.Action.Builder(null, "안 보냄", px).build())
                .build();
        c.getSystemService(NotificationManager.class).notify(nid, n);
    }

    static boolean send(Context c, String num, String text) {
        try {
            SmsManager sm = null;
            if (Build.VERSION.SDK_INT >= 31) sm = c.getSystemService(SmsManager.class);
            if (sm == null) sm = SmsManager.getDefault();
            ArrayList<String> parts = sm.divideMessage(text);
            sm.sendMultipartTextMessage(num, null, parts, null, null);
            p(c).edit().putLong("sent_" + num, System.currentTimeMillis()).apply();
            log(c, fmt(num) + " → 문자 보냄 ✓");
            return true;
        } catch (Exception e) {
            p(c).edit().putString("err", e.toString()).apply();
            log(c, "문자 실패: " + e);
            return false;
        }
    }
}
