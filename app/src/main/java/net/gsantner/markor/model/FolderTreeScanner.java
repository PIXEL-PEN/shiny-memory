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

        File[] files = dir.listFiles();
        if (files == null) return folder;

        // Sort for consistent visual order
        Arrays.sort(files, Comparator.comparing(File::getName, String::compareToIgnoreCase));

        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(".") || name.equals(".res") || name.equals("_res")) continue;

            if (file.isDirectory()) {
                FolderNode child = scanRecursive(file, depth + 1, categoryToExpand);
                folder.subfolders.add(child);

                // ✅ Auto-expand parent folders leading to the matched category
                if (child.expanded) {
                    folder.expanded = true;
                }

            } else if (file.isFile()) {
                folder.files.add(new FileNode(file.getName(), file, depth + 1));
            }
        }

        // ✅ Expand only the category folder itself (not others with same name)
        if (categoryToExpand != null && categoryToExpand.equalsIgnoreCase(folder.name) && depth == 3) {
            folder.expanded = true;
            Log.d("FolderTree", "📂 Auto-expanded category: " + folder.name);
        }

        return folder;
    }
}
