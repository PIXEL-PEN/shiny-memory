package net.gsantner.markor.model;

import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class FolderTreeScanner {

    public static FolderNode scan(File root, String targetMonth, String targetCategory) {
        if (!root.exists() || !root.isDirectory()) return null;
        return scanRecursive(root, 0, targetMonth, targetCategory);
    }

    private static FolderNode scanRecursive(File dir, int depth, String targetMonth, String targetCategory) {
        FolderNode folder = new FolderNode(dir.getName(), dir);
        folder.depth = depth;

        File[] files = dir.listFiles();
        if (files == null) return folder;

        // Sort for consistent output
        Arrays.sort(files, Comparator.comparing(File::getName, String::compareToIgnoreCase));

        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(".") || name.equals(".res") || name.equals("_res")) continue;

            if (file.isDirectory()) {
                FolderNode child = scanRecursive(file, depth + 1, targetMonth, targetCategory);
                folder.subfolders.add(child);

                // ✅ Expand path leading to exact match
                if (child.expanded) {
                    folder.expanded = true;
                }
            } else if (file.isFile()) {
                folder.files.add(new FileNode(file.getName(), file, depth + 1));
            }
        }

        // ✅ Expand only when both month and category match
        if (depth == 2 && folder.name.equalsIgnoreCase(targetMonth)) {
            Log.d("FolderTree", "📂 Found matching month: " + folder.name);
        }

        if (depth == 3 &&
                folder.name.equalsIgnoreCase(targetCategory) &&
                folder.file.getParentFile().getName().equalsIgnoreCase(targetMonth)) {
            folder.expanded = true;
            Log.d("FolderTree", "📂 Auto-expanded category: " + folder.name);
        }

        return folder;
    }
}
