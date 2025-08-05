package net.gsantner.markor.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.gsantner.markor.R;
import net.gsantner.markor.model.FolderNode;
import net.gsantner.markor.model.FolderTreeScanner;

import java.io.File;
import net.gsantner.markor.adapter.FolderTreeAdapter;
import android.os.Environment;




public class FolderBrowserActivity extends Activity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_browser);  // ✅ Load visual layout

        Log.d("FolderTree", "📣 FolderBrowserActivity launched");

        File rootFolder = new File(Environment.getExternalStorageDirectory(), "Documents/markor default");


        if (rootFolder == null || !rootFolder.exists()) {
            Log.w("FolderTree", "⚠️ Root folder not found: " + rootFolder);
            finish();
            return;
        }

        Log.d("FolderTree", "📣 Folder scan started: " + rootFolder.getAbsolutePath());
        FolderNode tree = FolderTreeScanner.scan(rootFolder);

        // ✅ Bind RecyclerView
        recyclerView = findViewById(R.id.folder_browser_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new FolderTreeAdapter(tree));  // ✅ ACTIVE adapter line

        Log.d("FolderTree", "✅ Tree scan complete. UI ready.");
    }
}
