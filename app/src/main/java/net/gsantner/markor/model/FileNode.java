package net.gsantner.markor.model;

import java.io.File;
import java.util.Date;

public class FileNode {
    public String name;
    public File file;
    public Date lastModified;
    public int depth;

    public FileNode(String name, File file, int depth) {
        this.name = name;
        this.file = file;
        this.depth = depth;
        this.lastModified = new Date(file.lastModified());
    }

}

