package kr.gaand.callback;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends Activity {
    static final int PURPLE = 0xFF4A2F6B;
    EditText[] ed = new EditText[3];
    Switch[] sw = new Switch[3];
    Switch once;
    TextView st;
    EditText test;
    TextView diag;
    TextView logv;

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    TextView tv(String s, int size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(6), 0, dp(6));
        return t;
    }

    Button btn(String s, boolean main, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(18);
        b.setAllCaps(false);
        b.setTextColor(main ? 0xFFFFFFFF : PURPLE);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        g.setColor(main ? PURPLE : 0xFFFFFFFF);
        g.setStroke(dp(2), PURPLE);
        b.setBackground(g);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(58));
        lp.topMargin = dp(10);
        b.setLayoutParams(lp);
        return b;
    }

    EditText box(String s) {
        EditText e = new EditText(this);
        e.setText(s);
        e.setTextSize(16);
        e.setMinLines(4);
        e.setGravity(Gravity.TOP);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(10));
        g.setColor(0xFFFFFFFF);
        g.setStroke(dp(1), 0xFFD8D0E3);
        e.setBackground(g);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        return e;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Core.channel(this);
        LogJob.schedule(this);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFFFAF7F2);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(24), dp(18), dp(60));
        sv.addView(l);

        l.addView(tv("가앤 명함문자", 26, true, PURPLE));
        try { l.addView(tv("버전 " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName, 13, false, 0xFF888888)); } catch (Exception e) { }
        l.addView(tv("통화가 끝나면 '명함 문자 보낼까요?' 알림이 떠요.\n[보내기]를 누를 때만 문자가 갑니다.", 17, false, 0xFF333333));
        st = tv("", 17, true, 0xFFB00020);
        l.addView(st);
        l.addView(btn("① 권한 허용하기", true, v -> req()));
        l.addView(btn("② 배터리 절전에서 빼기", false, v -> batt()));

        for (int k = 0; k < 3; k++) {
            l.addView(tv("\n" + Core.label(k) + " 끝나면 보낼 문자", 19, true, PURPLE));
            sw[k] = new Switch(this);
            sw[k].setText("이 경우에 묻기");
            sw[k].setTextSize(17);
            sw[k].setChecked(Core.on(this, k));
            l.addView(sw[k]);
            ed[k] = box(Core.msg(this, k));
            l.addView(ed[k]);
        }
        l.addView(tv("", 6, false, 0));
        once = new Switch(this);
        once.setText("같은 번호는 하루에 한 번만 묻기");
        once.setTextSize(17);
        once.setChecked(Core.p(this).getBoolean("once", true));
        l.addView(once);
        l.addView(btn("저장하기", true, v -> { save(); toast("저장했어요"); }));
        l.addView(btn("문구 처음 상태로 되돌리기", false, v -> reset()));

        l.addView(tv("\n테스트", 19, true, PURPLE));
        l.addView(tv("내 번호를 적고 누르면 알림이 떠요. [보내기]를 누르면 내 폰으로 문자가 와요.", 15, false, 0xFF555555));
        test = new EditText(this);
        test.setHint("내 휴대폰 번호");
        test.setInputType(InputType.TYPE_CLASS_PHONE);
        test.setTextSize(18);
        l.addView(test);
        l.addView(btn("테스트 알림 띄우기", false, v -> {
            save();
            String n = Core.clean(test.getText().toString());
            if (n.length() < 10) { toast("번호를 적어주세요"); return; }
            if (!allOk()) { toast("먼저 ① 권한 허용하기를 눌러주세요"); return; }
            Core.p(this).edit().remove("err").apply();
            Core.ask(this, n, "테스트", 0, 99999);
            diag.setText("확인 중…");
            new Handler(Looper.getMainLooper()).postDelayed(() -> { diag.setText(Core.diag(this, 99999)); showLog(); }, 900);
        }));
        diag = tv("", 16, true, 0xFF333333);
        l.addView(diag);
        l.addView(btn("알림 설정 열기", false, v -> openNoti()));
        l.addView(tv("\n최근 기록 (통화 후 무슨 일이 있었는지)", 17, true, PURPLE));
        logv = tv("", 14, false, 0xFF444444);
        l.addView(logv);
        setContentView(sv);
    }

    String[] perms() {
        ArrayList<String> a = new ArrayList<>();
        a.add(Manifest.permission.READ_PHONE_STATE);
        a.add(Manifest.permission.READ_CALL_LOG);
        a.add(Manifest.permission.SEND_SMS);
        if (Build.VERSION.SDK_INT >= 33) a.add(Manifest.permission.POST_NOTIFICATIONS);
        return a.toArray(new String[0]);
    }

    boolean allOk() {
        for (String s : perms()) if (checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }

    void req() {
        if (allOk()) { toast("이미 모두 허용됐어요"); return; }
        requestPermissions(perms(), 1);
    }

    @Override
    public void onRequestPermissionsResult(int rc, String[] p, int[] r) {
        refresh();
        if (!allOk()) toast("허용이 안 된 권한이 있어요. 아래 안내를 보세요");
    }

    void batt() {
        try {
            PowerManager pm = getSystemService(PowerManager.class);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) { toast("이미 빠져 있어요"); refresh(); return; }
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
        }
    }

    void openNoti() {
        try {
            startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
        }
    }

    void showLog() {
        String s = Core.p(this).getString("log", "");
        logv.setText(s.length() == 0 ? "아직 기록이 없어요. 전화를 한 통 해보세요." : s);
    }

    void save() {
        SharedPreferences.Editor e = Core.p(this).edit();
        for (int k = 0; k < 3; k++) {
            e.putBoolean("on_" + Core.key(k), sw[k].isChecked());
            e.putString("m_" + Core.key(k), ed[k].getText().toString().trim());
        }
        e.putBoolean("once", once.isChecked());
        e.apply();
    }

    void reset() {
        String[] d = {Core.D_IN, Core.D_OUT, Core.D_MISS};
        for (int k = 0; k < 3; k++) ed[k].setText(d[k]);
        save();
        toast("처음 문구로 되돌렸어요");
    }

    void refresh() {
        PowerManager pm = getSystemService(PowerManager.class);
        boolean batt = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        if (allOk() && batt) {
            st.setText("✓ 준비 완료. 이제 통화가 끝나면 알림이 떠요.");
            st.setTextColor(0xFF2F7D5B);
        } else if (allOk()) {
            st.setText("권한 OK. ② 배터리 절전에서 빼기도 눌러주세요.");
            st.setTextColor(0xFFB06000);
        } else {
            st.setText("아직 준비 안 됨. ① 권한 허용하기를 눌러주세요.\n(허용 버튼이 안 뜨면: 설정 > 애플리케이션 > 가앤 명함문자 > 오른쪽 위 ⋮ > '제한된 설정 허용' 후 다시)");
            st.setTextColor(0xFFB00020);
        }
    }

    @Override
    protected void onResume() { super.onResume(); refresh(); showLog(); if (diag != null && diag.getText().length() > 0) diag.setText(Core.diag(this, 0)); }

    @Override
    protected void onPause() { super.onPause(); save(); }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
