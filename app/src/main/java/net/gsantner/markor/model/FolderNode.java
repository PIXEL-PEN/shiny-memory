package net.gsantner.markor.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderNode {
    public String name;
    public File file;
    public List<FolderNode> subfolders = new ArrayList<>();
    public List<FileNode> files = new ArrayList<>();

    public FolderNode(String name, File file) {
        this.name = name;
        this.file = file;
    }
}
