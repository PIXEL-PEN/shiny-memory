package net.gsantner.markor.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderTreeScanner {

    public static FolderNode scan(File rootFolder, String selectedCategory, String selectedMonth) {
        if (rootFolder == null || !rootFolder.exists()) {
            return new FolderNode("Empty", rootFolder);  // fallback
        }
        FolderNode root = buildTree(rootFolder, 0, selectedCategory, selectedMonth);
        root.expanded = true;  // ✅ Always expand top-level folder
        return root;
    }

    private static FolderNode buildTree(File dir, int depth, String selectedCategory, String selectedMonth) {
        FolderNode node = new FolderNode(dir.getName(), dir);
        node.depth = depth;

        File[] files = dir.listFiles();
        if (files == null) return node;

        for (File file : files) {
            if (file.isDirectory()) {
                // Skip .res and _res folders
                String folderName = file.getName().toLowerCase();
                if (folderName.equals(".res") || folderName.equals("_res")) continue;

                FolderNode subfolder = buildTree(file, depth + 1, selectedCategory, selectedMonth);

                // Expand the folder if it matches selected category or month
                if (file.getName().equalsIgnoreCase(selectedCategory) ||
                        file.getName().equalsIgnoreCase(selectedMonth)) {
                    subfolder.expanded = true;
                    node.expanded = true;  // ensure parent is open
                }

                node.subfolders.add(subfolder);
            } else {
                node.files.add(new FileNode(file.getName(), file, depth + 1));
            }
        }
        return node;
    }
}
