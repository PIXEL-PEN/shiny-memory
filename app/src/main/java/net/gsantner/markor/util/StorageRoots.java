package net.gsantner.markor.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import androidx.preference.PreferenceManager;

import net.gsantner.markor.BuildConfig;

import java.io.File;

public final class StorageRoots {
    private StorageRoots() {}

    // If your project uses a different key for custom root, change this:
    private static final String PREF_KEY_STORAGE_ROOT = "pref_storage_root";

    // Canonical names
    private static final String PIXELPEN_ROOT_NAME = "Markor Plus";
    private static final String ORIGINAL_ROOT_NAME = "markor default";

    public static File getWorkingRoot(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);

        // 1) User-chosen path (if any)
        String savedPath = sp.getString(PREF_KEY_STORAGE_ROOT, null);

        // 2) Default per flavor
        File docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        String defaultRootName = BuildConfig.IS_PIXELPEN_BUILD ? PIXELPEN_ROOT_NAME : ORIGINAL_ROOT_NAME;
        File defaultRoot = new File(docs, defaultRootName);

        File root = (savedPath == null || savedPath.trim().isEmpty())
                ? defaultRoot
                : new File(savedPath);

        // 3) Auto-correct legacy / messy names for PixelPen → canonical folder
        if (BuildConfig.IS_PIXELPEN_BUILD) {
            String name = root.getName();
            if (equalsIgnoreCaseTrim(name, "markor default")
                    || equalsIgnoreCaseTrim(name, "markor plus (pixelpen)")
                    || equalsIgnoreCaseTrim(name, "markor plus")) {
                root = defaultRoot; // -> /Documents/Markor Plus
            }
        }

        // 4) Persist corrected absolute path so the whole app follows it
        String abs = root.getAbsolutePath();
        if (!abs.equals(savedPath)) {
            sp.edit().putString(PREF_KEY_STORAGE_ROOT, abs).apply();
        }
        return root;
    }

    private static boolean equalsIgnoreCaseTrim(String a, String b) {
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }
}
