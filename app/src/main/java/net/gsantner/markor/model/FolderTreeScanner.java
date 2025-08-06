package net.gsantner.markor.model;

import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class FolderTreeScanner {

    public static FolderNode scan(File root, String categoryToExpand) {
        if (!root.exists() || !root.isDirectory()) return null;
        return scanRecursive(root, 0, categoryToExpand);
    }

    private static FolderNode scanRecursive(File dir, int depth, String categoryToExpand) {
        FolderNode folder = new FolderNode(dir.getName(), dir);
        folder.depth = depth;

        // Auto-expand category folder if name matches
        if (categoryToExpand != null && categoryToExpand.equalsIgnoreCase(dir.getName())) {
            folder.expanded = true;
            Log.d("FolderTree", "📂 Auto-expanded category: " + dir.getName());
        }

        File[] files = dir.listFiles();
        if (files == null) return folder;

        // Sort for consistent visual order
        Arrays.sort(files, Comparator.comparing(File::getName, String::compareToIgnoreCase));

        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(".") || name.equals(".res") || name.equals("_res")) continue;

            if (file.isDirectory()) {
                folder.subfolders.add(scanRecursive(file, depth + 1, categoryToExpand));
            } else if (file.isFile()) {
                folder.files.add(new FileNode(file.getName(), file, depth + 1));
            }
        }

        return folder;
    }
}
