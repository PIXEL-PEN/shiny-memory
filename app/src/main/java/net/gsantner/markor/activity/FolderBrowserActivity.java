package net.gsantner.markor.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.gsantner.markor.R;
import net.gsantner.markor.adapter.FolderTreeAdapter;
import net.gsantner.markor.model.FolderNode;
import net.gsantner.markor.model.FolderTreeScanner;

import java.io.File;

public class FolderBrowserActivity extends Activity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_browser);  // ✅ Load visual layout

        Log.d("FolderTree", "📣 FolderBrowserActivity launched");

        // ✅ Get category from intent
        String selectedCategory = getIntent().getStringExtra("selectedCategory");
        Log.d("FolderTree", "📌 Selected category: " + selectedCategory);

        // ✅ Build root folder path (confirmed working location)
        File rootFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "markor default");

        if (rootFolder == null || !rootFolder.exists()) {
            Log.w("FolderTree", "⚠️ Root folder not found: " + rootFolder);
            finish();
            return;
        }

        Log.d("FolderTree", "📣 Folder scan started: " + rootFolder.getAbsolutePath());

        // ✅ Scan with category awareness
        FolderNode tree = FolderTreeScanner.scan(rootFolder, selectedCategory);

        if (tree == null) {
            Log.w("FolderTree", "⚠️ Tree is null. Aborting.");
            finish();
            return;
        }

        tree.expanded = true;  // Show year folders initially

        // ✅ Bind RecyclerView
        recyclerView = findViewById(R.id.folder_browser_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new FolderTreeAdapter(tree));

        Log.d("FolderTree", "✅ Tree scan complete. UI ready.");
    }
}
