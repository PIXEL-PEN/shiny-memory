package net.gsantner.markor.frontend.filesearch;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import net.gsantner.markor.R;
import net.gsantner.opoc.util.GsCollectionUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;

@SuppressWarnings("WeakerAccess")

public class FileSearchEngine {
    public static final AtomicBoolean isSearchExecuting = new AtomicBoolean(false);
    public static final AtomicReference<WeakReference<Activity>> activity = new AtomicReference<>();

    private static final List<String> defaultIgnoredDirs = Arrays.asList("^\\.git$", "^\\.tmp$", ".*[Tt]humb.*");
    private static final int maxPreviewLength = 100;
    public static final int maxQueryHistoryCount = 20;
    public static final LinkedList<String> queryHistory = new LinkedList<>();

    public static void addToHistory(String query) {
        queryHistory.remove(query);

        if (queryHistory.size() == maxQueryHistoryCount) {
            queryHistory.removeLast();
        }
        queryHistory.addFirst(query);
    }

    public static class SearchOptions {
        public File rootSearchDir;
        public String query;

        public boolean isRegexQuery;
        public boolean isCaseSensitiveQuery;
        public boolean isSearchInContent;
        public boolean isOnlyFirstContentMatch;

        public int maxSearchDepth;
        public List<String> ignoredDirectories;
        public boolean isShowMatchPreview = true;
        public char[] password = new char[0];
        public int message = 0;
    }

    public static class FitFile {
        public final File file;
        public final String relPath;
        public final boolean isDirectory;
        public final @NonNull List<Pair<String, Integer>> children;

        public FitFile(
                final File file,
                final String relPath,
                final boolean isDirectory,
                final @Nullable List<Pair<String, Integer>> lineNumbers
        ) {
            this.file = file.getAbsoluteFile();
            this.relPath = relPath;
            this.isDirectory = isDirectory;
            this.children = Collections.unmodifiableList(lineNumbers != null ? lineNumbers : Collections.emptyList());
        }

        @NonNull
        @Override
        public String toString() {
            return (!children.isEmpty() ? String.format("(%s) ", children.size()) : "") + relPath;
        }
    }

    public static FileSearchEngine.QueueSearchFilesTask queueFileSearch(
            @NonNull final Activity activity,
            final SearchOptions config,
            final GsCallback.a1<List<FitFile>> callback
    ) {
        FileSearchEngine.activity.set(new WeakReference<>(activity));
        FileSearchEngine.isSearchExecuting.set(true);
        FileSearchEngine.addToHistory(config.query);
        FileSearchEngine.QueueSearchFilesTask task = new FileSearchEngine.QueueSearchFilesTask(config, callback);
        task.execute();

        return task;
    }

    public static class QueueSearchFilesTask extends AsyncTask<Void, Integer, List<FitFile>> {
        private final SearchOptions _config;
        private final GsCallback.a1<List<FitFile>> _callback;

        // _matcher.reset() is _not_ thread safe. Will need alternate approach when we make search parallel
        private final Matcher _matcher;

        private Snackbar _snackBar;
        private Integer _countCheckedFiles = 0;
        private final List<FitFile> _result = new ArrayList<>();
        private final Set<Matcher> _ignoredRegexDirs = new HashSet<>();
        private final Set<String> _ignoredExactDirs = new HashSet<>();

        public QueueSearchFilesTask(final SearchOptions config, final GsCallback.a1<List<FitFile>> callback) {
            _config = config;
            _callback = callback;

            _config.query = _config.isCaseSensitiveQuery ? _config.query : _config.query.toLowerCase();
            splitRegexExactFiles(config.ignoredDirectories, _ignoredExactDirs, _ignoredRegexDirs);
            splitRegexExactFiles(FileSearchEngine.defaultIgnoredDirs, _ignoredExactDirs, _ignoredRegexDirs);

            Pattern pattern = null;
            if (_config.isRegexQuery) {
                try {
                    _config.query = _config.query.replaceAll("(?<![.])[*]", ".*");
                    pattern = Pattern.compile(_config.query);
                } catch (Exception ex) {
                    final Activity a = activity.get().get();
                    if (a != null) {
                        final String errorMessage = a.getString(R.string.regex_can_not_be_compiled) + ": " + _config.query;
                        Toast.makeText(a, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            }
            _matcher = pattern != null ? pattern.matcher("") : null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (_config.isRegexQuery && _matcher == null) {
                cancel(true);
                return;
            }
            bindSnackBar(_config.query);
        }

        public void bindSnackBar(String text) {
            if (!FileSearchEngine.isSearchExecuting.get()) {
                return;
            }

            try {
                final View view = activity.get().get().findViewById(android.R.id.content);
                _snackBar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE);
                _snackBar.addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                if (FileSearchEngine.isSearchExecuting.get()) {
                                    bindSnackBar(text);
                                }
                            }
                        })
                        .setAction(android.R.string.cancel, (v) -> {
                            _snackBar.dismiss();
                            cancel(true);
                        })
                        .show();
            } catch (Exception ignored) {
                cancel(true);
            }
        }

        @Override
        protected List<FitFile> doInBackground(final Void... ignored) {
            final ArrayDeque<Pair<File, Integer>> stack = new ArrayDeque<>();
            stack.add(Pair.create(_config.rootSearchDir, 0));
            final int trimLength = _config.rootSearchDir.getAbsolutePath().length() + 1;

            Pair<File, Integer> pair;
            while ((pair = stack.pollLast()) != null && !isCancelled()) {
                final int depth = pair.second;
                final File dir = pair.first;

                if (depth < _config.maxSearchDepth && dir.canRead()) {
                    handleDirectory(dir, trimLength, depth, stack::addLast);
                    publishProgress(stack.size(), depth, _result.size(), _countCheckedFiles);
                }
            }

            GsCollectionUtils.keySort(_result, f -> f.relPath.toLowerCase());

            return _result;
        }

        private void handleDirectory(
                final File dir,
                final int trimSize,
                final int depth,
                final GsCallback.a1<Pair<File, Integer>> pushToStack
        ) {

            final File[] files = dir.listFiles();

            if (files == null) {
                return;
            }

            _countCheckedFiles += files.length;

            for (final File file : files) {

                if (isCancelled()) {
                    return;
                }

                final String name = _config.isCaseSensitiveQuery ? file.getName() : file.getName().toLowerCase();

                if (!isIgnored(name)) {

                    final boolean isDir = file.isDirectory();
                    final String relPath = file.getAbsolutePath().substring(trimSize);

                    final int beforeContentCount = _result.size();
                    if (_config.isSearchInContent && !isDir && file.canRead() && GsFileUtils.isTextFile(file)) {
                        getContentMatches(file, relPath, _config.isOnlyFirstContentMatch);
                    }

                    // Search name if directory or not already included due to content
                    if (isDir || _result.size() == beforeContentCount) {
                        if (_config.isRegexQuery ? _matcher.reset(name).matches() : name.contains(_config.query)) {
                            _result.add(new FitFile(file, relPath, isDir, null));
                        }
                    }

                    // Only check for symbolic link directories
                    if (isDir && depth < _config.maxSearchDepth && !GsFileUtils.isSymbolicLink(file)) {
                        pushToStack.callback(Pair.create(file, depth + 1));
                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (_snackBar != null) {
                // _currentQueueLength, _currentSearchDepth, _result.size(), _countCheckedFiles
                _snackBar.setText("⭕" + values[2] + " || \uD83D\uDD0D" + values[0] + " || ⬇️ " + values[1] + " || \uD83D\uDC41️" + values[3] + "\n" + _config.query);
            }
        }

        @Override
        protected void onPostExecute(List<FitFile> ret) {
            super.onPostExecute(ret);
            FileSearchEngine.isSearchExecuting.set(false);
            if (_snackBar != null) {
                _snackBar.dismiss();
            }
            if (!isCancelled() && _callback != null) {
                try {
                    _callback.callback(ret);
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            FileSearchEngine.isSearchExecuting.set(false);
        }

        private void splitRegexExactFiles(final List<String> list, final Set<String> exactList, final Set<Matcher> regexList) {
            for (String pattern : (list != null ? list : new ArrayList<String>())) {
                if (pattern.isEmpty()) {
                    continue;
                }
                if (!_config.isCaseSensitiveQuery) {
                    pattern = pattern.toLowerCase();
                }

                if (pattern.startsWith("\"")) {
                    pattern = pattern.replace("\"", "");
                    if (pattern.isEmpty()) {
                        continue;
                    }
                    exactList.add(pattern);
                } else {
                    pattern = pattern.replaceAll("(?<![.])[*]", ".*");
                    try {
                        regexList.add(Pattern.compile(pattern).matcher(""));
                    } catch (Exception ex) {
                        final Activity a = activity.get().get();
                        if (a != null) {
                            final String errorMessage = a.getString(R.string.regex_can_not_be_compiled) + ": " + pattern;
                            Toast.makeText(a, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }

        // Match line and return preview string. Preview will be null if no match found
        private String matchLine(final String line) {
            final String preparedLine = _config.isCaseSensitiveQuery ? line : line.toLowerCase();

            int start = -1, end = -1;
            if (_config.isRegexQuery) {
                if (_matcher.reset(preparedLine).find()) {
                    start = _matcher.start();
                    end = _matcher.end();
                }
            } else {
                start = preparedLine.indexOf(_config.query);
                if (start >= 0) {
                    end = start + _config.query.length();
                }
            }

            // Preview is based on original line
            if (start >= 0 && end <= line.length()) {
                if (!_config.isShowMatchPreview) {
                    return "";
                }
                if (line.length() < maxPreviewLength) {
                    return line;
                } else {
                    int offset = (maxPreviewLength - (end - start)) / 2;
                    int subStart = Math.max(start - offset, 0);
                    int subEnd = Math.min(end + offset, line.length());
                    return String.format("… %s …", line.substring(subStart, subEnd));
                }
            }
            return null;
        }

        private void getContentMatches(final File file, final String relPath, final boolean isFirstMatchOnly) {
            List<Pair<String, Integer>> contentMatches = null;

            try (final BufferedReader br = new BufferedReader(new InputStreamReader(getInputStream(file)))) {
                int lineNumber = 0;
                for (String line; (line = br.readLine()) != null; ) {
                    if (isCancelled()) {
                        break;
                    }
                    line = matchLine(line);
                    if (line != null) {

                        // We lazily create the match list
                        // And therefore avoid creating it for _every_ file
                        if (contentMatches == null) {
                            contentMatches = new ArrayList<>();
                            _result.add(new FitFile(file, relPath, false, contentMatches));
                        }

                        // Note that content matches is only created on the first find
                        contentMatches.add(new Pair<>(line, lineNumber));

                        if (isFirstMatchOnly) {
                            break;
                        }
                    }
                    lineNumber++;
                }
            } catch (Exception ignored) {
            }
        }

        private boolean isIgnored(final String dirName) {
            for (final String pattern : _ignoredExactDirs) {
                if (dirName.equals(pattern)) {
                    return true;
                }
            }

            for (final Matcher matcher : _ignoredRegexDirs) {
                if (matcher.reset(dirName).matches()) {
                    return true;
                }
            }
            return false;
        }

        private InputStream getInputStream(File file) throws FileNotFoundException {
            if (isEncryptedFile(file)) {
                final byte[] encryptedContext = GsFileUtils.readCloseStreamWithSize(new FileInputStream(file), (int) file.length());
                return new ByteArrayInputStream(JavaPasswordbasedCryption.getDecryptedText(encryptedContext, _config.password.clone()).getBytes(StandardCharsets.UTF_8));
            } else {
                return new FileInputStream(file);
            }
        }
    }

    private static boolean isEncryptedFile(File file) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && file.getName().endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
    }
}
