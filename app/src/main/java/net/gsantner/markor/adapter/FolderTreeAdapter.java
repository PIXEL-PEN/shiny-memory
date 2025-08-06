package net.gsantner.markor.adapter;

import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import net.gsantner.markor.R;
import net.gsantner.markor.model.FileNode;
import net.gsantner.markor.model.FolderNode;

import java.util.ArrayList;
import java.util.List;

public class FolderTreeAdapter extends RecyclerView.Adapter<FolderTreeAdapter.NodeViewHolder> {
    private final List<Object> visibleItems = new ArrayList<>();
    private final FolderNode rootNode;

    public FolderTreeAdapter(FolderNode rootNode) {
        this.rootNode = rootNode;
        rebuildVisibleItems(); // Initial build
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
            holder.name.setText(folder.name);

            int depth = folder.depth;
            int padding = 20 * depth;
            holder.itemView.setPadding(padding, holder.itemView.getPaddingTop(), 20, holder.itemView.getPaddingBottom());

            // Styling by folder depth
            if (depth == 2) {
                holder.name.setTextSize(19); // Month
                holder.name.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            } else if (depth == 3) {
                holder.name.setTextSize(17); // Category
                holder.name.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                holder.name.setTextSize(16);
                holder.name.setTypeface(Typeface.DEFAULT);
            }

            holder.itemView.setOnClickListener(v -> toggleExpanded(folder));

        } else if (item instanceof FileNode) {
            FileNode file = (FileNode) item;
            holder.icon.setText("📄");
            holder.name.setText(file.name);
            holder.name.setTextSize(16);
            holder.name.setTypeface(Typeface.DEFAULT);

            int padding = 20 * file.depth;
            holder.itemView.setPadding(padding, holder.itemView.getPaddingTop(), 20, holder.itemView.getPaddingBottom());
        }
    }

    @Override
    public int getItemCount() {
        Log.d("FolderTree", "🧮 Total visible nodes: " + visibleItems.size());
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

    // ✅ Toggle logic and rebuilding
    private void toggleExpanded(FolderNode node) {
        node.expanded = !node.expanded;
        rebuildVisibleItems();
        notifyDataSetChanged();
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
}
