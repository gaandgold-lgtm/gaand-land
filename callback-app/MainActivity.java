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
    int PURPLE = 0xFF4A2F6B;
    EditText[] ed = new EditText[3];
    Switch[] sw = new Switch[3];
    Switch once;
    Switch viaApp;
    Switch imgOn;
    android.widget.ImageView cardImg;
    TextView st;
    EditText test;
    TextView diag;
    TextView logv;
    TextView lic;
    EditText code;
    EditText[] info = new EditText[4];
    static final String[] INFO_KEY = {"biz", "name", "phone", "link"};
    static final String[] INFO_HINT = {"상호 (예: 행복부동산)", "이름 (예: 홍길동)", "연락처 (예: 010-1234-5678)", "링크 - 선택 (예: 블로그·스마트스토어 주소)"};

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
        PURPLE = getColor(R.color.brand);
        Core.channel(this);
        LogJob.schedule(this);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFFFAF7F2);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(24), dp(18), dp(60));
        sv.addView(l);

        l.addView(tv(getString(R.string.app_name), 26, true, PURPLE));
        try { l.addView(tv("버전 " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName, 13, false, 0xFF888888)); } catch (Exception e) { }
        l.addView(tv("통화가 끝나면 '명함 문자 보낼까요?' 알림이 떠요.\n[보내기]를 누를 때만 문자가 갑니다.", 17, false, 0xFF333333));
        st = tv("", 17, true, 0xFFB00020);
        l.addView(st);
        l.addView(btn("① 권한 허용하기", true, v -> req()));
        l.addView(btn("② 배터리 절전에서 빼기", false, v -> batt()));

        if (License.need(this)) {
            l.addView(tv("\n사용 기간", 19, true, PURPLE));
            lic = tv("", 17, true, 0xFF333333);
            l.addView(lic);
            l.addView(tv("내 기기번호: " + License.devCode(this), 18, true, 0xFF111111));
            l.addView(tv("사용할 기간을 고르고 신청 문자를 보내 주세요", 15, false, 0xFF555555));
            android.widget.RadioGroup rg = new android.widget.RadioGroup(this);
            for (int i = 0; i < PERIODS.length; i++) {
                android.widget.RadioButton rb = new android.widget.RadioButton(this);
                rb.setId(1000 + i); rb.setText(PERIODS[i]); rb.setTextSize(17);
                rg.addView(rb);
            }
            rg.check(1000);
            rg.setOnCheckedChangeListener((g, id) -> period = PERIODS[id - 1000]);
            l.addView(rg);
            l.addView(btn("사용 신청 문자 보내기", false, v -> buy()));
            l.addView(btn("사용 기간 다시 확인", false, v -> recheck(true)));

        }
        if (getPackageName().endsWith("callcard")) {
            l.addView(tv("\n내 정보 (문자에 자동으로 들어가요)", 19, true, PURPLE));
            for (int i = 0; i < 4; i++) {
                info[i] = new EditText(this);
                info[i].setHint(INFO_HINT[i]);
                info[i].setTextSize(17);
                info[i].setText(Core.p(this).getString(INFO_KEY[i], ""));
                if (i == 2) info[i].setInputType(InputType.TYPE_CLASS_PHONE);
                if (i == 3) info[i].setInputType(InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_CLASS_TEXT);
                l.addView(info[i]);
            }
            l.addView(tv("문구 안의 {상호} {이름} {연락처} {링크} 자리에 위 정보가 들어가요. 링크를 비우면 그 줄은 빠져요.", 14, false, 0xFF666666));
        }

        for (int k = 0; k < 3; k++) {
            l.addView(tv("\n" + Core.label(k) + " 끝나면 보낼 문자", 19, true, PURPLE));
            sw[k] = new Switch(this);
            sw[k].setText("이 경우에 묻기");
            sw[k].setTextSize(17);
            sw[k].setChecked(Core.on(this, k));
            l.addView(sw[k]);
            ed[k] = box(Core.raw(this, k));
            l.addView(ed[k]);
        }
        l.addView(tv("", 6, false, 0));
        once = new Switch(this);
        once.setText("같은 번호는 하루에 한 번만 묻기");
        once.setTextSize(17);
        once.setChecked(Core.p(this).getBoolean("once", true));
        l.addView(once);
        viaApp = new Switch(this);
        viaApp.setText("문자 앱에서 확인하고 보내기 (추천: 한 통으로 가고 링크 미리보기 그림이 떠요)");
        viaApp.setTextSize(16);
        viaApp.setChecked(Core.p(this).getBoolean("viaApp", true));
        l.addView(viaApp);
        l.addView(tv("\n명함 이미지", 19, true, PURPLE));
        l.addView(tv("등록해 두면 [보내기]를 눌렀을 때 문자에 명함 그림이 같이 붙어요. (위 '문자 앱에서 확인하고 보내기'가 켜져 있어야 해요. 그림이 붙으면 MMS로 가요)", 14, false, 0xFF666666));
        cardImg = new android.widget.ImageView(this);
        cardImg.setAdjustViewBounds(true);
        cardImg.setMaxHeight(600);
        l.addView(cardImg);
        imgOn = new Switch(this);
        imgOn.setText("문자에 명함 이미지 붙이기");
        imgOn.setTextSize(16);
        imgOn.setChecked(Core.p(this).getBoolean("img_on", true));
        imgOn.setOnCheckedChangeListener((cb, on) -> Core.p(this).edit().putBoolean("img_on", on).apply());
        l.addView(imgOn);
        l.addView(btn("명함 이미지 고르기 (갤러리)", false, v -> {
            Intent pick = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
            try { startActivityForResult(Intent.createChooser(pick, "명함 이미지 고르기"), 77); } catch (Exception e) { toast("갤러리를 열 수 없어요"); }
        }));
        l.addView(btn("명함 이미지 빼기", false, v -> { CardProvider.file(this).delete(); showCard(); toast("이미지를 뺐어요"); }));
        showCard();
        l.addView(tv("끄면 [보내기] 한 번에 바로 가지만, 짧은 문자 여러 통으로 나눠서 가요.", 14, false, 0xFF666666));
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
            if (!License.ok(this)) { toast("사용 기간이 아니에요. 위 [사용 기간] 안내를 봐주세요"); return; }
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
        if (getPackageName().endsWith("callcard")) {
            TextView mk = tv("\nGA& 가앤이 만든 앱 · 문의 010-8186-8816", 14, true, PURPLE);
            mk.setGravity(Gravity.CENTER);
            l.addView(mk);
        }
        setContentView(sv);
        recheck(false);
    }

    void recheck(boolean show) {
        if (!License.need(this)) return;
        if (lic != null && show) lic.setText("확인 중…");
        new Thread(() -> {
            boolean ok = License.refresh(getApplicationContext());
            runOnUiThread(() -> { refresh(); if (show) toast(ok ? "확인했어요" : "인터넷 연결을 확인해 주세요"); });
        }).start();
    }

    static final String[] PERIODS = {"한시 무료", "1개월", "2개월", "3개월", "6개월", "1년"};
    String period = PERIODS[0];

    void showCard() {
        java.io.File f = CardProvider.file(this);
        if (f.exists()) { cardImg.setImageBitmap(android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath())); cardImg.setVisibility(View.VISIBLE); }
        else { cardImg.setImageDrawable(null); cardImg.setVisibility(View.GONE); }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != 77 || res != RESULT_OK || data == null || data.getData() == null) return;
        try {
            android.graphics.Bitmap b;
            try (java.io.InputStream in = getContentResolver().openInputStream(data.getData())) { b = android.graphics.BitmapFactory.decodeStream(in); }
            if (b == null) { toast("이미지를 읽을 수 없어요"); return; }
            int max = 1280, w = b.getWidth(), h = b.getHeight();
            if (Math.max(w, h) > max) { float r = (float) max / Math.max(w, h); b = android.graphics.Bitmap.createScaledBitmap(b, Math.round(w * r), Math.round(h * r), true); }
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(CardProvider.file(this))) { b.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out); }
            showCard();
            toast("명함 이미지를 등록했어요");
        } catch (Exception e) { toast("이미지 저장 실패: " + e.getMessage()); }
    }

    void buy() {
        String body = getString(R.string.app_name) + " 사용 신청\n신청 기간: " + period + "\n기기번호: " + License.devCode(this);
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + getString(R.string.seller_phone))).putExtra("sms_body", body);
        try { startActivity(i); } catch (Exception e) { toast("문자 앱을 열 수 없어요"); }
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
        e.putBoolean("viaApp", viaApp.isChecked());
        if (info[0] != null) for (int i = 0; i < 4; i++) e.putString(INFO_KEY[i], info[i].getText().toString().trim());
        e.apply();
    }

    void reset() {
        for (int k = 0; k < 3; k++) ed[k].setText(Core.def(this, k));
        save();
        toast("처음 문구로 되돌렸어요");
    }

    void refresh() {
        if (lic != null) {
            lic.setText(License.status(this));
            lic.setTextColor(License.ok(this) ? 0xFF2F7D5B : 0xFFB00020);
        }
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
    protected void onResume() { super.onResume(); refresh(); showLog(); recheck(false); if (diag != null && diag.getText().length() > 0) diag.setText(Core.diag(this, 0)); }

    @Override
    protected void onPause() { super.onPause(); save(); }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
