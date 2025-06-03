package net.gsantner.markor.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtils {

    public static long getCreationTime(File file) {
        try {
            Path path = file.toPath();
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
            return attr.creationTime().toMillis();
        } catch (Exception e) {
            // fallback to lastModified if creation time not available
            return file.lastModified();
        }
    }
}
