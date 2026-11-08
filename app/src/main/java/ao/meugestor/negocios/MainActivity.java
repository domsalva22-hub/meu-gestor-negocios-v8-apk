
package ao.meugestor.negocios;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private static final int FILE_CHOOSER_REQUEST = 4101;
    private static final int SAVE_FILE_REQUEST = 4102;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private String pendingFileContent = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);

        webView.addJavascriptInterface(new AndroidBridge(), "Android");
        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }

                filePathCallback = callback;

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");

                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    callback.onReceiveValue(null);
                }

                return true;
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    private class AndroidBridge {

        @JavascriptInterface
        public void saveTextFile(String name, String mime, String content) {

            runOnUiThread(() -> {
                pendingFileContent = content == null ? "" : content;

                String safeName =
                        (name == null || name.trim().isEmpty())
                        ? "backup-meu-gestor.json"
                        : name.replaceAll("[^A-Za-z0-9._-]", "_");

                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                intent.setType(
                        mime == null || mime.isEmpty()
                        ? "application/octet-stream"
                        : mime
                );

                intent.putExtra(Intent.EXTRA_TITLE, safeName);

                try {
                    startActivityForResult(intent, SAVE_FILE_REQUEST);
                } catch (Exception e) {
                    Toast.makeText(
                            MainActivity.this,
                            "Não foi possível guardar o ficheiro.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST) {

            if (filePathCallback != null) {

                Uri[] result =
                        (resultCode == RESULT_OK && data != null
                        && data.getData() != null)
                        ? new Uri[]{data.getData()}
                        : null;

                filePathCallback.onReceiveValue(result);
                filePathCallback = null;
            }

        } else if (requestCode == SAVE_FILE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            try (OutputStream out =
                    getContentResolver().openOutputStream(data.getData())) {

                if (out != null) {
                    out.write(
                            pendingFileContent.getBytes(StandardCharsets.UTF_8)
                    );
                }

                Toast.makeText(
                        this,
                        "Ficheiro guardado com sucesso.",
                        Toast.LENGTH_LONG
                ).show();

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Erro ao guardar o ficheiro.",
                        Toast.LENGTH_LONG
                ).show();
            }

            pendingFileContent = "";
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
