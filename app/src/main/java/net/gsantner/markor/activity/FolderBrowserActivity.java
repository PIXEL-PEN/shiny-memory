package net.gsantner.markor.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FolderBrowserActivity extends Activity {

    private static final String TAG = "WebViewDebug";

    // Long-title & content thresholds
    private static final int TITLE_LEN_THRESHOLD = 120;           // chars (without .md)
    private static final long SIZE_THRESHOLD_BYTES = 2L * 1024L;  // 2 KB tiny-size heuristic

    private WebView webView;
    private File rootFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_browser);

        // Keep IME hidden in this activity to reduce flash on outgoing launch
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        // ---- Static header (no ActionBar) ----
        View headerTap = findViewById(R.id.header_click_target);
        if (headerTap != null) headerTap.setOnClickListener(v -> finish());
        ImageButton backBtn = findViewById(R.id.btn_back);
        if (backBtn != null) backBtn.setOnClickListener(v -> finish());
        TextView title = findViewById(R.id.title_text);
        if (title != null) title.setText("› Browser Tree");

        // ---- Date Created chronology: leave intact ----
        net.gsantner.markor.model.CreationIndex cidx = new net.gsantner.markor.model.CreationIndex(this);
        net.gsantner.markor.model.FolderTreeScanner.setCreationIndex(cidx);

        // --------------------------------------------
        webView = findViewById(R.id.folder_browser_webview);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Don’t let WebView grab focus (helps prevent IME popping)
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);
        webView.clearFocus();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                final String url = (request != null && request.getUrl() != null) ? request.getUrl().toString() : "";
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
            @SuppressWarnings("deprecation")
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

            // Open: exact path first; fallback to best same-folder match
            private void openInMarkor(String filePath) {
                try {
                    // Hide keyboard + clear focus BEFORE launching to avoid flash
                    try {
                        View focused = getCurrentFocus();
                        if (focused != null) focused.clearFocus();
                        if (webView != null) webView.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            View anchor = (focused != null) ? focused : webView;
                            if (anchor != null) imm.hideSoftInputFromWindow(anchor.getWindowToken(), 0);
                        }
                    } catch (Throwable ignored) {}

                    File requested = new File(filePath);
                    File target = requested.exists() ? requested : findBestMatch(requested);

                    if (target != null && target.exists()) {
                        Intent intent = new Intent(FolderBrowserActivity.this, DocumentActivity.class);
                        intent.putExtra(Document.EXTRA_FILE, target);
                        intent.putExtra(Document.EXTRA_DO_PREVIEW, true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                    } else {
                        android.widget.Toast.makeText(
                                FolderBrowserActivity.this,
                                "File not found:\n" + filePath,
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    android.widget.Toast.makeText(
                            FolderBrowserActivity.this,
                            "Unable to open file.",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                }
            }

            private File findBestMatch(File expectedPath) {
                if (expectedPath == null) return null;
                File dir = expectedPath.getParentFile();
                if (dir == null || !dir.isDirectory()) return null;

                String wanted = normalizeName(expectedPath.getName());
                File best = null;
                int bestScore = -1;

                File[] list = dir.listFiles();
                if (list == null) return null;

                for (File f : list) {
                    if (!f.isFile()) continue;
                    String cand = normalizeName(f.getName());

                    int score = 0;
                    if (cand.equals(wanted)) score = 1000;
                    else if (cand.startsWith(wanted) || wanted.startsWith(cand)) score = 900;
                    else if (cand.contains(wanted) || wanted.contains(cand)) score = 800;

                    if (score > bestScore) {
                        bestScore = score;
                        best = f;
                    }
                }
                return (bestScore >= 800) ? best : null;
            }

            private String normalizeName(String name) {
                String n = name;
                int dot = n.lastIndexOf('.');
                if (dot > 0) n = n.substring(0, dot);
                return n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
            }
        });

        String selectedCategory = getIntent().getStringExtra("selectedCategory");
        String selectedMonth = getIntent().getStringExtra("selectedMonth");

        rootFolder = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "markor default"
        );
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
                .append(".file .icon { width:16px; display:flex; align-items:center; justify-content:center; margin-top:1px; }")
                .append(".flag-square { width:12px; height:12px; border-radius:3px; background:#d35400; }")
                .append(".flag-square.hollow { background:transparent; border:2px solid #d35400; }")
                .append(".note-title { font-size: 17px; font-weight: 500; margin-left:2px; }")
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

            String baseName = file.name.replaceAll("\\.md$", "");
            boolean isLongTitle = baseName.length() >= TITLE_LEN_THRESHOLD;

            // Default icon
            String iconHtml = "📄";

            if (isLongTitle) {
                boolean substantial =
                        (file.file != null && file.file.length() >= SIZE_THRESHOLD_BYTES)
                                || (file.file != null && hasAnyContentBeyondSeed(file.file));

                // Hollow square for long-title; solid square if substantial content as well
                iconHtml = substantial
                        ? "<span class='flag-square' title='Long title & content present'></span>"
                        : "<span class='flag-square hollow' title='Long title'></span>";
            }

            sb.append("<div class='file' style='").append(fileIndent).append("'>")
                    .append("<div class='icon'>").append(iconHtml).append("</div>")
                    .append("<div>")
                    .append("<a href='note:").append(file.file.getAbsolutePath()).append("'>")
                    .append("<span class='note-title'>").append(baseName).append("</span>")
                    .append("</a>")
                    .append(dateStr)
                    .append("</div>")
                    .append("</div>");
        }
        sb.append("</div>");
    }

    // --- Helper: true if there is ANY non-empty content beyond the first (seed) line ---
    private boolean hasAnyContentBeyondSeed(File f) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            // skip first line (seeded datestamp)
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    return true; // any real content beyond seed
                }
            }
            return false;
        } catch (Exception e) {
            Log.w(TAG, "hasAnyContentBeyondSeed failed for " + f, e);
            return false;
        } finally {
            try { if (br != null) br.close(); } catch (Exception ignore) {}
        }
    }
}
