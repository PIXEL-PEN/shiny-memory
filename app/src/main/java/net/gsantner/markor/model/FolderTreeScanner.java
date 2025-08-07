package net.gsantner.markor.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FolderTreeScanner {

    public static FolderNode scan(File root, String selectedCategory, String selectedMonth) {
        return scanRecursive(root, selectedCategory, selectedMonth, 0);
    }

    private static FolderNode scanRecursive(File folder, String selectedCategory, String selectedMonth, int depth) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return null;
        }

        FolderNode folderNode = new FolderNode(folder.getName().replaceFirst("^\\d{2}_", ""), folder);
        folderNode.subfolders = new ArrayList<>();
        folderNode.files = new ArrayList<>();

        File[] children = folder.listFiles();
        if (children != null) {
            Arrays.sort(children, Comparator.comparing(File::getName));

            for (File file : children) {
                if (file.isDirectory()) {
                    FolderNode child = scanRecursive(file, selectedCategory, selectedMonth, depth + 1);
                    if (child != null) {
                        folderNode.subfolders.add(child);
                    }
                } else if (file.isFile() && file.getName().endsWith(".md")) {
                    folderNode.files.add(new FileNode(file.getName(), file, depth));
                }
            }
        }

        // Expand only relevant branches
        String currentName = folder.getName();
        if (depth == 0 || currentName.equals("markor default") ||
                currentName.equals(selectedCategory) ||
                currentName.equals(selectedMonth)) {
            folderNode.expanded = true;
        }

        return folderNode;
    }
}
