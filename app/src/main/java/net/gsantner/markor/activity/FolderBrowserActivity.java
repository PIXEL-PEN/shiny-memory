package net.gsantner.markor.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.model.FileNode;
import net.gsantner.markor.model.FolderNode;
import net.gsantner.markor.model.FolderTreeScanner;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FolderBrowserActivity extends Activity {

    private static final String TAG = "WebViewDebug";

    private WebView webView;
    private File rootFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_browser);

        // ---- Static header (no ActionBar) ----
        View headerTap = findViewById(R.id.header_click_target);
        if (headerTap != null) {
            headerTap.setOnClickListener(v -> finish());
        }
        ImageButton backBtn = findViewById(R.id.btn_back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }
        TextView title = findViewById(R.id.title_text);
        if (title != null) {
            title.setText("› Browser Tree"); // or "Browser Tree"
        }



        // --------------------------------------------

        webView = findViewById(R.id.folder_browser_webview);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                final String url = request.getUrl() != null ? request.getUrl().toString() : "";
                Log.d(TAG, "Tapped URL: " + url);

                if (url.startsWith("note:")) {
                    String filePath = android.net.Uri.decode(url.substring("note:".length()));
                    openInMarkor(filePath);
                    return true;
                }
                if (url.startsWith("file://")) {
                    String filePath = android.net.Uri.parse(url).getPath();
                    openInMarkor(filePath);
                    return true;
                }
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "Tapped URL (legacy): " + url);
                if (url != null && url.startsWith("note:")) {
                    String filePath = android.net.Uri.decode(url.substring("note:".length()));
                    openInMarkor(filePath);
                    return true;
                }
                if (url != null && url.startsWith("file://")) {
                    String filePath = android.net.Uri.parse(url).getPath();
                    openInMarkor(filePath);
                    return true;
                }
                return false;
            }

            private void openInMarkor(String filePath) {
                try {
                    File file = new File(filePath);
                    if (file.exists()) {
                        Intent intent = new Intent(FolderBrowserActivity.this, DocumentActivity.class);
                        intent.putExtra(Document.EXTRA_FILE, file);
                        intent.putExtra(Document.EXTRA_DO_PREVIEW, true);
                        startActivity(intent);
                    } else {
                        android.widget.Toast.makeText(
                                FolderBrowserActivity.this,
                                "File not found:\n" + filePath,
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        String selectedCategory = getIntent().getStringExtra("selectedCategory");
        String selectedMonth = getIntent().getStringExtra("selectedMonth");

        rootFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "markor default");
        FolderNode tree = FolderTreeScanner.scan(rootFolder, selectedCategory, selectedMonth);

        String html = buildHtml(tree, selectedCategory, selectedMonth);
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    private String buildHtml(FolderNode root, String selectedCategory, String selectedMonth) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
                .append("<style>")
                .append("body { font-family: sans-serif; padding: 16px; font-size: 17px; }")
                .append(".folder { cursor: pointer; display: flex; align-items: center; margin: 10px 0; }")
                .append(".arrow { display: inline-block; width: 0.80em; transition: transform 0.2s; margin-right: 6px; color: #d35400; }")
                .append(".folder.collapsed .arrow { transform: rotate(0deg); }")
                .append(".folder.expanded .arrow { transform: rotate(90deg); }")
                .append(".file { display: flex; align-items: flex-start; margin: 8px 0 0; font-size: 16px; gap: 10px; padding-bottom: 6px; border-bottom: 1px solid #ddd; }")
                .append(".file .icon { font-size: 18px; flex-shrink: 0; margin-top: 1px; }")
                .append(".note-title { font-size: 17px; font-weight: 500; }")
                .append(".note-meta  { font-size: 12px; color: #6e6e6e; margin-left: 0; white-space: nowrap; display: block; }")
                .append(".note-meta span.sep { padding: 0 2px; }")
                .append(".year { font-size: 19px; font-weight: bold; }")
                .append(".month { font-size: 18px; font-weight: bold; }")
                .append(".category { font-size: 16px; font-weight: bold; }")
                .append("a { text-decoration: none; color: #222; }")
                .append("a:hover { text-decoration: underline; color: #000; }")
                .append("</style>")
                .append("<script>")
                .append("function toggle(id, labelId) {")
                .append("  var e = document.getElementById(id);")
                .append("  var label = document.getElementById(labelId);")
                .append("  if (e.style.display === 'none') {")
                .append("    e.style.display = 'block';")
                .append("    label.classList.add('expanded');")
                .append("    label.classList.remove('collapsed');")
                .append("  } else {")
                .append("    e.style.display = 'none';")
                .append("    label.classList.add('collapsed');")
                .append("    label.classList.remove('expanded');")
                .append("  }")
                .append("}")
                .append("</script>")
                .append("</head><body>");

        appendFolderHtml(sb, root, 0);
        sb.append("</body></html>");
        return sb.toString();
    }

    private void appendFolderHtml(StringBuilder sb, FolderNode folder, int depth) {
        String id = "f" + folder.hashCode();
        String folderIndent = "margin-left: " + (depth * 20) + "px;";
        String expandByDefault = folder.expanded ? "block" : "none";

        String levelClass = "";
        if (depth == 1) levelClass = "year";
        else if (depth == 2) levelClass = "month";
        else if (depth == 3) levelClass = "category";

        String labelClass = "folder " + levelClass + (folder.expanded ? " expanded" : " collapsed");
        String displayName = folder.name.replaceFirst("^\\d{2}_", "");

        sb.append("<div id='label_").append(id).append("' class='").append(labelClass)
                .append("' style='").append(folderIndent).append("' onclick=\"toggle('").append(id).append("','label_").append(id).append("')\">")
                .append("<span class='arrow'>▶</span>")
                .append(displayName)
                .append("</div>");

        sb.append("<div id='").append(id).append("' style='display:").append(expandByDefault).append(";'>");

        for (FolderNode sub : folder.subfolders) {
            appendFolderHtml(sb, sub, depth + 1);
        }

        String fileIndent = "margin-left: " + ((depth * 20) + 20) + "px;";
        SimpleDateFormat sdfWk = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat sdfDay = new SimpleDateFormat("d",   Locale.getDefault());
        SimpleDateFormat sdfMon = new SimpleDateFormat("MMM", Locale.getDefault());
        SimpleDateFormat sdfYr  = new SimpleDateFormat("yy",  Locale.getDefault());

        for (FileNode file : folder.files) {
            Date t = new Date(file.file.lastModified());
            String dateStr = "<div class='note-meta'>"
                    + sdfWk.format(t) + "<span class='sep'>|</span>"
                    + sdfDay.format(t) + " " + sdfMon.format(t) + "<span class='sep'>|</span>"
                    + sdfYr.format(t)
                    + "</div>";

            sb.append("<div class='file' style='").append(fileIndent).append("'>")
                    .append("<div class='icon'>📄</div>")
                    .append("<div>")
                    .append("<a href='note:").append(file.file.getAbsolutePath()).append("'>")
                    .append("<span class='note-title'>").append(file.name.replaceAll("\\.md$", "")).append("</span>")
                    .append("</a>")
                    .append(dateStr)
                    .append("</div>")
                    .append("</div>");
        }
        sb.append("</div>");
    }
}
