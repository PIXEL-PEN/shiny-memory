package net.gsantner.markor.adapter;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.R;
import net.gsantner.markor.model.FileNode;
import net.gsantner.markor.model.FolderNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderTreeAdapter extends RecyclerView.Adapter<FolderTreeAdapter.NodeViewHolder> {
    private final List<Object> visibleItems = new ArrayList<>();
    private final FolderNode rootNode;

    public FolderTreeAdapter(FolderNode rootNode) {
        this.rootNode = rootNode;
        rebuildVisibleItems();
    }

    @Override
    public NodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.folder_tree_item, parent, false);
        return new NodeViewHolder(view);
    }
    @Override
    public void onBindViewHolder(NodeViewHolder holder, int position) {
        Object item = visibleItems.get(position);

        if (item instanceof FolderNode) {
            FolderNode folder = (FolderNode) item;
            holder.icon.setText(folder.expanded ? "▾" : "▸");

            // Remove number prefix from month folders (e.g., 01_January → January)
            String displayName = folder.name;
            if (folder.depth == 2 && folder.name.contains("_")) {
                displayName = folder.name.substring(folder.name.indexOf("_") + 1);
            }
            holder.name.setText(displayName);

            int depth = folder.depth;
            int padding = 20 * depth;
            holder.itemView.setPadding(
                    padding,
                    holder.itemView.getPaddingTop(),
                    20,
                    holder.itemView.getPaddingBottom()
            );

            if (depth == 2) {
                holder.name.setTextSize(19);
                holder.name.setTypeface(android.graphics.Typeface.DEFAULT);
                holder.name.setTextColor(android.graphics.Color.DKGRAY);
            } else if (depth == 3) {
                holder.name.setTextSize(17);
                holder.name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                holder.name.setTextColor(android.graphics.Color.BLACK);
            } else {
                holder.name.setTextSize(16);
                holder.name.setTypeface(android.graphics.Typeface.DEFAULT);
                holder.name.setTextColor(android.graphics.Color.BLACK);
            }

            holder.itemView.setOnClickListener(v -> {
                folder.expanded = !folder.expanded;
                rebuildVisibleItems();
                notifyDataSetChanged();
            });

        } else if (item instanceof FileNode) {
            FileNode file = (FileNode) item;
            holder.icon.setText("\uD83D\uDCC4");

            // Format "31 Aug" for modified date
            String modifiedText = "";
            if (file.file != null && file.file.exists()) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault());
                modifiedText = " • (" + sdf.format(new java.util.Date(file.file.lastModified())) + ")";
            }

            holder.name.setText(file.name + modifiedText);
            holder.name.setTextSize(16);
            holder.name.setTypeface(android.graphics.Typeface.DEFAULT);
            holder.name.setTextColor(android.graphics.Color.BLACK);

            int padding = 20 * file.depth;
            holder.itemView.setPadding(
                    padding,
                    holder.itemView.getPaddingTop(),
                    20,
                    holder.itemView.getPaddingBottom()
            );

            holder.itemView.setOnClickListener(v -> {
                java.io.File noteFile = file.file;
                // TODO: handle file open
            });
        }
    }

    @Override
    public int getItemCount() {
        return visibleItems.size();
    }

    static class NodeViewHolder extends RecyclerView.ViewHolder {
        TextView icon;
        TextView name;

        NodeViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.item_icon);
            name = view.findViewById(R.id.item_name);
        }
    }

    private void rebuildVisibleItems() {
        visibleItems.clear();
        buildVisibleItemsRecursive(rootNode);
    }

    private void buildVisibleItemsRecursive(FolderNode node) {
        visibleItems.add(node);
        if (node.expanded) {
            for (FolderNode sub : node.subfolders) {
                buildVisibleItemsRecursive(sub);
            }
            visibleItems.addAll(node.files);
        }
    }

    // Helper: find best filename match when FS truncated a very long title
    private static java.io.File findBestPrefixMatch(java.io.File parent, String displayName) {
        if (displayName == null || displayName.isEmpty()) return null;

        // stem of the displayed name (without extension)
        String base = displayName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);

        // limit the matching prefix (avoid extremely long probes)
        int max = Math.min(200, base.length());
        String probe = base.substring(0, max);

        java.io.File[] kids = parent.listFiles();
        if (kids == null || kids.length == 0) return null;

        java.io.File best = null;
        int bestLen = -1;

        for (java.io.File k : kids) {
            if (!k.isFile()) continue;
            String name = k.getName();
            String stem = name;
            int d = stem.lastIndexOf('.');
            if (d > 0) stem = stem.substring(0, d);

            if (stem.startsWith(probe)) {
                if (name.length() > bestLen) {
                    best = k;
                    bestLen = name.length();
                }
            }
        }
        return best;
    }
}
