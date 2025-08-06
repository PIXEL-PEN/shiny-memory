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
        setContentView(R.layout.activity_folder_browser);

        Log.d("FolderTree", "📣 FolderBrowserActivity launched");

        // ✅ Read selected category and month from intent
        String selectedCategory = getIntent().getStringExtra("selectedCategory");
        String selectedMonth = getIntent().getStringExtra("selectedMonth");

        Log.d("FolderTree", "📌 Category: " + selectedCategory);
        Log.d("FolderTree", "📌 Month: " + selectedMonth);

        // ✅ Root = Markor default
        File rootFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "markor default");

        if (rootFolder == null || !rootFolder.exists()) {
            Log.w("FolderTree", "⚠️ Root folder not found: " + rootFolder);
            finish();
            return;
        }

        /// ✅ Scan tree — expand specific month + category
        FolderNode tree = FolderTreeScanner.scan(rootFolder, selectedMonth, selectedCategory);

        if (tree == null) {
            Log.w("FolderTree", "⚠️ Tree is null. Aborting.");
            finish();
            return;
        }

        // ✅ Display result
        recyclerView = findViewById(R.id.folder_browser_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new FolderTreeAdapter(tree));

        Log.d("FolderTree", "✅ Tree scan complete. UI ready.");
    }
}
