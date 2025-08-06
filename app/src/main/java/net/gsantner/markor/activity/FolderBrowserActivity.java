package net.gsantner.markor.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.gsantner.markor.R;
import net.gsantner.markor.model.FileNode;
import net.gsantner.markor.model.FolderNode;
import net.gsantner.markor.model.FolderTreeScanner;

import java.io.File;

public class FolderBrowserActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_browser);

        webView = findViewById(R.id.folder_browser_webview);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient());

        // Get selected category and month
        String selectedCategory = getIntent().getStringExtra("selectedCategory");
        String selectedMonth = getIntent().getStringExtra("selectedMonth");

        File rootFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "markor default");
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
                .append(".folder { font-weight: bold; margin: 10px 0; cursor: pointer; font-size: 18px; }")
                .append(".file { display: flex; align-items: flex-start; margin-left: 40px; margin-top: 10px; font-size: 16px; gap: 10px; }")
                .append(".file .icon { font-size: 20px; }")
                .append(".file .title { flex: 1; word-break: break-word; }")
                .append("a { text-decoration: none; color: #222; }")
                .append("a:hover { text-decoration: underline; color: #000; }")
                .append("</style>")
                .append("<script>")
                .append("function toggle(id) {")
                .append("  var e = document.getElementById(id);")
                .append("  if (e.style.display === 'none') { e.style.display = 'block'; } else { e.style.display = 'none'; }")
                .append("}")
                .append("</script>")
                .append("</head><body>");

        appendFolderHtml(sb, root, 0, selectedCategory, selectedMonth);

        sb.append("</body></html>");
        return sb.toString();
    }

    private void appendFolderHtml(StringBuilder sb, FolderNode folder, int depth, String selectedCategory, String selectedMonth) {
        String id = "f" + folder.hashCode();
        String indent = "margin-left: " + (depth * 20) + "px;";
        String expandByDefault = folder.expanded ? "block" : "none";

        // Remove number prefixes like "01_January"
        String displayName = folder.name.replaceFirst("^\\d{2}_", "");

        sb.append("<div class='folder' style='").append(indent).append("' onclick=\"toggle('").append(id).append("')\">")
                .append(displayName).append("</div>");

        sb.append("<div id='").append(id).append("' style='display:").append(expandByDefault).append(";'>");

        for (FolderNode sub : folder.subfolders) {
            appendFolderHtml(sb, sub, depth + 1, selectedCategory, selectedMonth);
        }

        for (FileNode file : folder.files) {
            sb.append("<div class='file'>")
                    .append("<div class='icon'>📄</div>")
                    .append("<div class='title'><a href='file://").append(file.file.getAbsolutePath()).append("'>")
                    .append(file.name).append("</a></div></div>");
        }

        sb.append("</div>");
    }
}
