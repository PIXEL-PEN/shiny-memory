package net.gsantner.markor.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.FolderNode;
import net.gsantner.markor.model.FolderTreeScanner;
import net.gsantner.markor.model.FileNode;

import java.io.File;

public class FolderBrowserActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("FolderTree", "📣 FolderBrowserActivity launched");

        File rootFolder = new File(android.os.Environment.getExternalStorageDirectory(), "Documents/markor default");

        if (rootFolder == null || !rootFolder.exists()) {
            Log.w("FolderTree", "⚠️ Root folder not found: " + rootFolder);
            finish();
            return;
        }

        Log.d("FolderTree", "📣 Folder scan started: " + rootFolder.getAbsolutePath());
        FolderNode tree = FolderTreeScanner.scan(rootFolder);
        logTree(tree, 0);

        finish();  // optional: remove this when UI added
    }

    private void logTree(FolderNode node, int depth) {
        if (node == null) {
            Log.w("FolderTree", "⚠️ Null node encountered");
            return;
        }

        String indent = new String(new char[depth]).replace("\0", "  ");
        Log.d("FolderTree", indent + "📁 " + node.name);

        for (FileNode f : node.files) {
            Log.d("FolderTree", indent + "  📄 " + f.name);
        }

        for (FolderNode sub : node.subfolders) {
            logTree(sub, depth + 1);
        }
    }
}
