package net.gsantner.markor.model;

import java.io.File;
import java.util.Date;

public class FileNode {
    public String name;
    public File file;
    public Date lastModified;

    public FileNode(String name, File file) {
        this.name = name;
        this.file = file;
        this.lastModified = new Date(file.lastModified());
    }
}
