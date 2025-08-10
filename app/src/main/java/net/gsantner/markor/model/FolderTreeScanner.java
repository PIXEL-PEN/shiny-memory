package net.gsantner.markor.model;

import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;
import java.util.Locale;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

public class FolderTreeScanner {

    // Matches leading numeric prefix like "01_January" or "1-January"
    private static final Pattern LEADING_NUM = Pattern.compile("^(\\d{1,2})[_-].*");
    private static final Pattern MD_SUFFIX   = Pattern.compile("\\.md$", Pattern.CASE_INSENSITIVE);

    /**
     * Public entry: scan the tree and mark expansions so UI can show
     * root > year > selectedMonth > selectedCategory expanded.
     *
     * @param root             the Markor root folder (e.g., /Documents/markor default)
     * @param selectedCategory e.g., "General"
     * @param selectedMonth    e.g., "08_August" (case-insensitive compare)
     */
    public static FolderNode scan(File root, String selectedCategory, String selectedMonth) {
        if (root == null || !root.exists() || !root.isDirectory()) {
            Log.w("FolderTree", "scan(): invalid root: " + root);
            return null;
        }
        // Build the tree
        FolderNode tree = scanRecursive(root, 0, selectedCategory, selectedMonth, false);
        if (tree != null) {
            // Root always expanded so the user sees content immediately
            tree.expanded = true;
        }
        return tree;
    }

    /**
     * Recursively build the tree, sorting directories by numeric prefix (01..12) first,
     * and expanding only along the path (year -> month -> category).
     *
     * @param dir               current directory
     * @param depth             0=root, 1=year, 2=month, 3=category, ...
     * @param selectedCategory  category name to expand inside the selectedMonth
     * @param selectedMonth     month folder name like "08_August"
     * @param monthMatchedAbove true if an ancestor already matched the selectedMonth
     */
    private static FolderNode scanRecursive(File dir,
                                            int depth,
                                            String selectedCategory,
                                            String selectedMonth,
                                            boolean monthMatchedAbove) {
        FolderNode node = new FolderNode(dir.getName(), dir);
        node.depth = depth;

        File[] entries = dir.listFiles();
        if (entries == null) {
            return node;
        }

        // Partition entries to dirs & files; filter hidden/.res/_res
        List<File> dirs = new ArrayList<>();
        List<File> docs = new ArrayList<>();
        for (File f : entries) {
            String name = f.getName();
            if (name.startsWith(".") || name.equals(".res") || name.equals("_res")) {
                continue;
            }
            if (f.isDirectory()) {
                dirs.add(f);
            } else if (f.isFile()) {
                docs.add(f);
            }
        }

        // Sort directories: first by leading numeric prefix (01..12), fallback alpha
        sortSubfoldersByPrefix(dirs);

        // Sort files by Date Created (newest → oldest)
        Collections.sort(docs, (a, b) -> Long.compare(getCreationTimeMillis(b), getCreationTimeMillis(a)));

        // Determine if this node is the selected month folder
        boolean thisIsSelectedMonth =
                (selectedMonth != null)
                        && depth == 2
                        && equalsIgnoreCaseSafe(dir.getName(), selectedMonth);

        // Recurse into children
        for (File d : dirs) {
            FolderNode child = scanRecursive(
                    d,
                    depth + 1,
                    selectedCategory,
                    selectedMonth,
                    monthMatchedAbove || thisIsSelectedMonth
            );
            node.subfolders.add(child);
        }

        // Attach files (strip ".md" for display)
        for (File f : docs) {
            String displayName = MD_SUFFIX.matcher(f.getName()).replaceAll("");
            node.files.add(new FileNode(displayName, f, depth + 1));
        }

        // Expansion logic:
        // - Root (depth 0) expanded by caller (post-processing).
        // - Year (depth 1) expands only if it contains the selected month.
        // - Month (depth 2) expands if its name == selectedMonth.
        // - Category (depth 3) expands only if its name == selectedCategory and ancestor is the selected month.
        if (depth == 1) {
            // Expand year only if any child month equals selectedMonth
            if (containsMonth(node, selectedMonth)) {
                node.expanded = true;
            }
        } else if (depth == 2) {
            // Expand the selected month folder itself
            if (thisIsSelectedMonth) {
                node.expanded = true;
            }
        } else if (depth == 3) {
            // Expand the selected category only if we're already inside the selected month path
            if (monthMatchedAbove && selectedCategory != null
                    && equalsIgnoreCaseSafe(dir.getName(), selectedCategory)) {
                node.expanded = true;
            }
        }

        return node;
    }

    // ----- Helpers -----

    public static String formatShortDate(long timeMillis) {
        // Spaces around pipes are baked in
        return new SimpleDateFormat("EEE | d MMM | yy", Locale.ENGLISH)
                .format(new Date(timeMillis));
    }

    // Creation time with safe fallback to lastModified()
    public static long getCreationTimeMillis(File f) {
        try {
            BasicFileAttributes a = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
            return a.creationTime().toMillis();
        } catch (Exception ignored) {
            return f.lastModified();
        }
    }

    private static boolean containsMonth(FolderNode yearNode, String selectedMonth) {
        if (selectedMonth == null) return false;
        for (FolderNode m : yearNode.subfolders) {
            if (equalsIgnoreCaseSafe(m.name, selectedMonth)) {
                return true;
            }
        }
        return false;
    }

    private static boolean equalsIgnoreCaseSafe(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private static void sortSubfoldersByPrefix(List<File> dirs) {
        Collections.sort(dirs, (a, b) -> {
            int na = extractLeadingNumber(a.getName());
            int nb = extractLeadingNumber(b.getName());

            // If either has a number, sort by that number; if both or neither, fall back to alpha
            if (na != Integer.MAX_VALUE || nb != Integer.MAX_VALUE) {
                int cmp = Integer.compare(na, nb);
                if (cmp != 0) return cmp;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });
    }

    private static int extractLeadingNumber(String name) {
        Matcher m = LEADING_NUM.matcher(name);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {
            }
        }
        return Integer.MAX_VALUE; // Non-numbered go after numbered folders
    }
}
