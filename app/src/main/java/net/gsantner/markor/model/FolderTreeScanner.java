package net.gsantner.markor.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderTreeScanner {

    public static FolderNode scan(File root, String selectedCategory, String selectedMonth) {
        return scanRecursive(root, selectedCategory, selectedMonth, 0, "", "", "");
    }

    private static FolderNode scanRecursive(File folder, String selectedCategory, String selectedMonth, int depth,
                                            String currentYear, String currentMonth, String currentCategory) {
        if (folder == null || !folder.isDirectory()
                || folder.getName().startsWith(".")
                || folder.getName().endsWith("_res") || folder.getName().endsWith(".res")) {
            return null;
        }

        FolderNode node = new FolderNode(folder.getName(), folder);
        node.subfolders = new ArrayList<>();
        node.files = new ArrayList<>();

        String name = folder.getName();

        // Update path tracking
        if (depth == 1) currentYear = name;                  // e.g. "2025"
        if (depth == 2) currentMonth = name;                 // e.g. "08_August"
        if (depth == 3) currentCategory = name;              // e.g. "Writing"

        // Expansion logic
        boolean shouldExpand =
                (depth == 0) ||                                      // "markor default"
                        (depth == 1 && name.equals("2025")) ||               // year
                        (depth == 2 && name.equals(selectedMonth)) ||        // full match "08_August"
                        (depth == 3 && name.equals(selectedCategory) &&
                                currentMonth.equals(selectedMonth) &&
                                currentYear.equals("2025"));

        node.expanded = shouldExpand;

        File[] children = folder.listFiles();
        if (children == null) return node;

        for (File f : children) {
            if (f.isDirectory()) {
                FolderNode sub = scanRecursive(f, selectedCategory, selectedMonth, depth + 1,
                        currentYear, currentMonth, currentCategory);
                if (sub != null) {
                    node.subfolders.add(sub);
                }
            } else if (f.isFile() && f.getName().endsWith(".md")) {
                node.files.add(new FileNode(f.getName(), f, depth));
            }
        }

        return node;
    }
}
