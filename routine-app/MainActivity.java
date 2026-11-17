package kr.gaand.routine;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/** 루틴 달력 화면(웹)을 앱 안에서 보여주고, 알림은 앱이 직접 울림 */
public class MainActivity extends Activity {
    static final String URL = "https://land.gaandgold.co.kr/routine.html?app=1";
    WebView w;
    boolean triedCache = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Reminder.channel(this);
        w = new WebView(this);
        setContentView(w);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setTextZoom(100);
        s.setMediaPlaybackRequiresUserGesture(true);
        w.addJavascriptInterface(new Bridge(this), "GaanApp");
        w.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return handle(r.getUrl().toString());
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame() && !triedCache) {
                    triedCache = true;
                    v.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    v.loadUrl(URL);
                }
            }
        });
        w.setWebChromeClient(new WebChromeClient());
        if (b != null) w.restoreState(b);
        else w.loadUrl(URL);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
        Reminder.schedule(this);
    }

    boolean handle(String u) {
        Uri uri = Uri.parse(u);
        String sc = uri.getScheme(), host = uri.getHost(), path = uri.getPath();
        if ("https".equals(sc) && "land.gaandgold.co.kr".equalsIgnoreCase(host)
                && path != null && path.startsWith("/routine")) return false;
        try {
            Intent i;
            if ("intent".equals(sc)) {
                i = Intent.parseUri(u, Intent.URI_INTENT_SCHEME);
                i.setComponent(null);
                i.setSelector(null);
            } else {
                i = new Intent(Intent.ACTION_VIEW, uri);
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "열 수 있는 앱이 없어요", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); w.saveState(o); }
    @Override protected void onPause() { w.onPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); w.onResume(); }
    @Override public void onBackPressed() { if (w.canGoBack()) w.goBack(); else super.onBackPressed(); }
}
