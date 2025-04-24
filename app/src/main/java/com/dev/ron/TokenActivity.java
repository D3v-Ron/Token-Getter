package com.dev.ron;

import android.annotation.SuppressLint;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.util.Base64;
import android.webkit.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TokenActivity extends AppCompatActivity {
    WebView webView;
    TextView tokenView;
    boolean tokenFetched = false;
    Handler handler = new Handler();
    String currentToken = "";
    String encodedText = "djEuMCDCtyBRdWFudGhlb24gTGFicywgQnk6IFJvbiDwn5GR";

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        webView = findViewById(R.id.webView);
        tokenView = findViewById(R.id.tokenView);
        Button copyTokenBtn = findViewById(R.id.copyTokenBtn);
        Button viewHistoryBtn = findViewById(R.id.viewHistoryBtn);
        TextView footerText = findViewById(R.id.footerText);

        // Decode footer
        try {
            byte[] decodedBytes = Base64.decode(encodedText, Base64.DEFAULT);
            String decodedText = new String(decodedBytes, "UTF-8");
            footerText.setText(decodedText);
        } catch (Exception e) {
            footerText.setText("v1.0 Quantheon Labs, By: Ron ❤");
        }

        // Footer click
        footerText.setOnClickListener(v -> {
            String url = "https://discord.gg/3RENVFehFW";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

        // WebView cleanup
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        WebStorage.getInstance().deleteAllData();
        webView.clearCache(true);
        webView.clearFormData();
        webView.clearHistory();

        // WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setBlockNetworkImage(true); // Block images for speed
        webView.setInitialScale(1);

        // WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Clean UI - remove images, SVGs, QR, etc.
                view.evaluateJavascript(
                    "(function() {" +
                    "document.querySelectorAll('img, svg, canvas, picture, source').forEach(e => e.remove());" +
                    "document.querySelectorAll('[class*=qr], [class*=Qr], [class*=QR]').forEach(e => e.remove());" +
                    "document.body.style.backgroundColor='black';" +
                    "document.body.style.zoom='0.8';" +
                    "})()", null
                );

                if (url.contains("discord.com/login") || url.contains("discord.com")) {
                    injectTokenGrabberScript();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });

        webView.addJavascriptInterface(new JSInterface(), "Android");
        webView.loadUrl("https://discord.com/login");

        copyTokenBtn.setOnClickListener(v -> {
            if (!currentToken.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Discord Token", currentToken);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Token copied to clipboard!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No token found to copy", Toast.LENGTH_SHORT).show();
            }
        });

        viewHistoryBtn.setOnClickListener(v -> {
            Intent i = new Intent(TokenActivity.this, HistoryActivity.class);
            startActivity(i);
        });
    }

    private void injectTokenGrabberScript() {
        String script =
            "function grabToken() {" +
            "   window.webpackChunkdiscord_app.push([" +
            "       [Math.random()], {}, req => {" +
            "           if (!req.c) return;" +
            "           for (const m of Object.keys(req.c)" +
            "               .map(x => req.c[x].exports)" +
            "               .filter(x => x)) {" +
            "               if (m.default && m.default.getToken !== undefined) {" +
            "                   Android.showToken(m.default.getToken()); return;" +
            "               }" +
            "               if (m.getToken !== undefined) {" +
            "                   Android.showToken(m.getToken()); return;" +
            "               }" +
            "           }" +
            "       }" +
            "   ]);" +
            "   window.webpackChunkdiscord_app.pop();" +
            "}" +
            "setInterval(grabToken, 2000); grabToken();";

        webView.evaluateJavascript(script, null);
    }

    class JSInterface {
        @JavascriptInterface
        public void showToken(String token) {
            if (!tokenFetched && token != null && !token.isEmpty()) {
                tokenFetched = true;
                currentToken = token;
                runOnUiThread(() -> {
                    webView.loadData(
    "<body style='background-color:black;'>" +
    "<h2 style='color:Cyan;text-align:center;font-size:80px;font-family:monospace;'>LOGIN SUCCESS!</h2>" +
    "</body>",
    "text/html",
    "UTF-8"
);
                    new FetchUserInfoTask().execute(token);
                });
            }
        }
    }

    private class FetchUserInfoTask extends AsyncTask<String, Void, String> {
        String token;

        @Override
        protected String doInBackground(String... tokens) {
            token = tokens[0];
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL("https://discord.com/api/v9/users/@me").openConnection();
                conn.setRequestProperty("Authorization", token);
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) result.append(line);
                return result.toString();
            } catch (Exception e) {
                return "error";
            }
        }

        @Override
        protected void onPostExecute(String res) {
            if (!res.equals("error")) {
                try {
                    JSONObject user = new JSONObject(res);
                    String username = user.getString("username") + "#" + user.getString("discriminator");
                    String email = user.optString("email", "N/A");
                    String id = user.getString("id");

                    String display = "Username: " + username + "\nEmail: " + email + "\nID: " + id + "\nTOKEN: " + token;
                    tokenView.setText(display);
                    saveTokenToFile(username, token);
                } catch (Exception e) {
                    tokenView.setText("TOKEN: " + token + "\n(User info parse error)");
                }
            } else {
                tokenView.setText("TOKEN: " + token + "\n(User info fetch failed)");
            }
        }
    }

    private void saveTokenToFile(String username, String token) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String entry = "Time: " + time + "\nUser: " + username + "\nToken: " + token + "\n\n";
        try {
            FileOutputStream fos = openFileOutput("token_history.txt", MODE_APPEND);
            fos.write(entry.getBytes());
            fos.close();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save history!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
