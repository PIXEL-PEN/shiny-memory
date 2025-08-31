package net.gsantner.markor.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * Phase 1: lightweight, local-only index for "Date Created".
 * - Persists timestamps for new files (SharedPreferences).
 * - Lazily infers for existing files via NIO creationTime or falls back to lastModified.
 * - No UI changes; intended for sorting + later display.
 */
public final class CreationIndex {
    private static volatile CreationIndex sInstance;
    private static final String PREFS = "creation_index_v1";
    private static final String KEY_PREFIX = "p:";

    private final SharedPreferences prefs;

    private CreationIndex(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Get singleton instance. */
    public static CreationIndex get(Context ctx) {
        if (sInstance == null) {
            synchronized (CreationIndex.class) {
                if (sInstance == null) {
                    sInstance = new CreationIndex(ctx.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    /** Record created timestamp as "now" for a brand-new file. */
    public void putNow(File file) {
        if (file != null) put(file, System.currentTimeMillis());
    }

    /** Record a specific created timestamp (millis since epoch). */
    public void put(File file, long millis) {
        if (file == null) return;
        prefs.edit().putLong(KEY_PREFIX + file.getAbsolutePath(), millis).apply();
    }

    /**
     * Return created time if known; otherwise infer once and cache.
     * Inference: NIO creationTime → lastModified → now (as a last resort).
     */
    public long getOrInfer(File file) {
        if (file == null) return 0L;
        final String key = KEY_PREFIX + file.getAbsolutePath();
        if (prefs.contains(key)) {
            return prefs.getLong(key, file.lastModified());
        }
        long inferred = inferCreated(file);
        prefs.edit().putLong(key, inferred).apply();
        return inferred;
    }

    /** True if we already have a stored timestamp for this file. */
    public boolean has(File file) {
        return file != null && prefs.contains(KEY_PREFIX + file.getAbsolutePath());
    }

    /** Remove stored entry (e.g., on delete). */
    public void remove(File file) {
        if (file == null) return;
        prefs.edit().remove(KEY_PREFIX + file.getAbsolutePath()).apply();
    }

    /** Call when a file is renamed/moved to preserve its created timestamp. */
    public void rename(File from, File to) {
        if (from == null || to == null) return;
        final String fromKey = KEY_PREFIX + from.getAbsolutePath();
        if (prefs.contains(fromKey)) {
            long ts = prefs.getLong(fromKey, 0L);
            prefs.edit()
                    .remove(fromKey)
                    .putLong(KEY_PREFIX + to.getAbsolutePath(), ts)
                    .apply();
        }
    }

    // -------------------- internals --------------------

    private long inferCreated(File file) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(
                    file.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            FileTime ct = attrs.creationTime();
            if (ct != null) {
                long t = ct.toMillis();
                if (t > 0L) return t;
            }
        } catch (Throwable ignored) {
            // NIO not available / unsupported FS / API level — fall through
        }
        long lm = file.lastModified();
        return (lm > 0L) ? lm : System.currentTimeMillis();
    }
}
