package net.gsantner.markor.model;

import java.io.File;

public class FolderTreeScanner {

    public static FolderNode scan(File root) {
        if (!root.exists() || !root.isDirectory()) return null;
        return scanRecursive(root);
    }

    private static FolderNode scanRecursive(File dir) {
        FolderNode folder = new FolderNode(dir.getName(), dir);
        File[] files = dir.listFiles();
        if (files == null) return folder;

        for (File file : files) {
            if (file.isDirectory()) {
                folder.subfolders.add(scanRecursive(file));
            } else if (file.isFile()) {
                folder.files.add(new FileNode(file.getName(), file));
            }
        }
        return folder;
    }
}
