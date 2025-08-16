package net.gsantner.markor.model;

import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;
import java.util.Locale;

public class FolderTreeScanner {

    // Matches leading numeric prefix like "01_January" or "1-January"
    private static final Pattern LEADING_NUM = Pattern.compile("^(\\d{1,2})[_-].*");
    private static final Pattern MD_SUFFIX = Pattern.compile("\\.md$", Pattern.CASE_INSENSITIVE);

    // ---- Creation index integration ----
    private static net.gsantner.markor.model.CreationIndex sCreationIndex;

    public static void setCreationIndex(net.gsantner.markor.model.CreationIndex idx) {
        sCreationIndex = idx;
    }

    // Skip marker helper
    private static boolean shouldSkipDir(java.io.File dir) {
        return new java.io.File(dir, ".nomedia").exists()
                || new java.io.File(dir, ".skip-scan").exists();
    }

    /**
     * Lookup stable "created" time; lazily backfill (one-time) when missing.
     */
    private static long getIndexedCreated(File f) {
        if (sCreationIndex != null) {
            Long v = sCreationIndex.get(f);
            if (v != null && v > 0L) {
                return v;
            }
            long created = f.lastModified();
            sCreationIndex.put(f, created);
            return created;
        }
        return f.lastModified();
    }
    // ---- /Creation index integration ----

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
        FolderNode tree = scanRecursive(root, 0, selectedCategory, selectedMonth, false);
        if (tree != null) {
            tree.expanded = true;
        }
        if (sCreationIndex != null) {
            sCreationIndex.save();
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

        // Partition entries to dirs & files; filter hidden/.res/_res and only allow .md files for docs
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
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.endsWith(".md")) {
                    docs.add(f);
                }
            }
        }

        // Sort directories: first by leading numeric prefix (01..12), fallback alpha
        sortSubfoldersByPrefix(dirs);

        // Sort files by indexed Date Created (newest → oldest), fallback to lastModified if missing
        Collections.sort(docs, (a, b) -> Long.compare(getIndexedCreated(b), getIndexedCreated(a)));

        // Determine if this node is the selected month folder
        boolean thisIsSelectedMonth =
                (selectedMonth != null)
                        && depth == 2
                        && equalsIgnoreCaseSafe(dir.getName(), selectedMonth);

        // Recurse into children
        for (File d : dirs) {
            if (shouldSkipDir(d)) {
                continue;
            }
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

        // Expansion logic
        if (depth == 1) {
            if (containsMonth(node, selectedMonth)) {
                node.expanded = true;
            }
        } else if (depth == 2) {
            if (thisIsSelectedMonth) {
                node.expanded = true;
            }
        } else if (depth == 3) {
            if (monthMatchedAbove && selectedCategory != null
                    && equalsIgnoreCaseSafe(dir.getName(), selectedCategory)) {
                node.expanded = true;
            }
        }

        return node;
    }

    // ----- Helpers -----

    public static String formatShortDate(long timeMillis) {
        return new SimpleDateFormat("EEE | d MMM | yy", Locale.getDefault())
                .format(new Date(timeMillis));
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
        return Integer.MAX_VALUE;
    }
}
