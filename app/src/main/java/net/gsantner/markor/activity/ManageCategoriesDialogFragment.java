package net.gsantner.markor.activity;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.gsantner.markor.R;
import net.gsantner.markor.util.CategoryManager;

import java.util.ArrayList;
import java.util.List;

public class ManageCategoriesDialogFragment extends DialogFragment {

    public static ManageCategoriesDialogFragment newInstance() {
        return new ManageCategoriesDialogFragment();
    }

    private Spinner actionSpinner;
    private Spinner fromSpinner;
    private Spinner targetSpinner;
    private EditText inputNewName;
    private TextView previewOutput;
    private CheckBox chkForceMove;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();
        View root = LayoutInflater.from(ctx).inflate(R.layout.dialog_manage_categories, null, false);

        actionSpinner = root.findViewById(R.id.mc_action);
        fromSpinner = root.findViewById(R.id.mc_from);
        targetSpinner = root.findViewById(R.id.mc_target);
        inputNewName = root.findViewById(R.id.mc_new_name);
        previewOutput = root.findViewById(R.id.mc_preview_output);
        chkForceMove = root.findViewById(R.id.mc_force_uncategorized);

        // Populate action list
        ArrayAdapter<String> actions = new ArrayAdapter<>(
                ctx, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Add", "Rename", "Merge", "Delete", "Undo last"});
        actionSpinner.setAdapter(actions);

        // Load categories for from/target spinners
        List<String> cats = new ArrayList<>(CategoryManager.listCategories(ctx).changedPaths);
        if (cats.isEmpty()) { cats.add("Uncategorized"); }
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, cats);
        fromSpinner.setAdapter(catAdapter);
        targetSpinner.setAdapter(catAdapter);

        // Input type for new name
        inputNewName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        // Toggle fields based on action
        actionSpinner.setOnItemSelectedListener(new SimpleOnItemSelected(() -> {
            String act = currentAction();
            inputNewName.setVisibility(("Add".equals(act) || "Rename".equals(act)) ? View.VISIBLE : View.GONE);
            fromSpinner.setVisibility(("Rename".equals(act) || "Merge".equals(act) || "Delete".equals(act)) ? View.VISIBLE : View.GONE);
            targetSpinner.setVisibility(("Rename".equals(act) || "Merge".equals(act)) ? View.VISIBLE : View.GONE);
            chkForceMove.setVisibility("Delete".equals(act) ? View.VISIBLE : View.GONE);
            previewOutput.setText("");
        }));

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.manage_categories)
                .setView(root)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.preview, (d, w) -> doPreview())
                .setPositiveButton(R.string.confirm, (d, w) -> doConfirm());

        return b.create();
    }

    private String currentAction() {
        return (String) actionSpinner.getSelectedItem();
    }

    private void doPreview() {
        Context ctx = requireContext();
        String act = currentAction();
        CategoryManager.Result r;
        switch (act) {
            case "Add": {
                String name = safeText(inputNewName);
                r = CategoryManager.addCategory(ctx, name); // add is cheap; show result
                break;
            }
            case "Rename": {
                String from = (String) fromSpinner.getSelectedItem();
                String to = safeText(inputNewName);
                r = CategoryManager.renameCategory(ctx, from, to, true);
                break;
            }
            case "Merge": {
                String from = (String) fromSpinner.getSelectedItem();
                String to = (String) targetSpinner.getSelectedItem();
                r = CategoryManager.mergeCategories(ctx, from, to, true);
                break;
            }
            case "Delete": {
                String name = (String) fromSpinner.getSelectedItem();
                boolean force = chkForceMove.isChecked();
                r = CategoryManager.deleteCategory(ctx, name, true, force);
                break;
            }
            case "Undo last": {
                // Just tell the user what will happen
                previewOutput.setText(getString(R.string.undo_will_revert_last_op));
                return;
            }
            default:
                previewOutput.setText(getString(R.string.invalid_action));
                return;
        }
        showResult(r);
    }

    private void doConfirm() {
        Context ctx = requireContext();
        String act = currentAction();
        CategoryManager.Result r;
        switch (act) {
            case "Add": {
                String name = safeText(inputNewName);
                r = CategoryManager.addCategory(ctx, name);
                break;
            }
            case "Rename": {
                String from = (String) fromSpinner.getSelectedItem();
                String to = safeText(inputNewName);
                r = CategoryManager.renameCategory(ctx, from, to, false);
                break;
            }
            case "Merge": {
                String from = (String) fromSpinner.getSelectedItem();
                String to = (String) targetSpinner.getSelectedItem();
                r = CategoryManager.mergeCategories(ctx, from, to, false);
                break;
            }
            case "Delete": {
                String name = (String) fromSpinner.getSelectedItem();
                boolean force = chkForceMove.isChecked();
                r = CategoryManager.deleteCategory(ctx, name, false, force);
                break;
            }
            case "Undo last": {
                r = CategoryManager.undoLast(ctx);
                break;
            }
            default:
                previewOutput.setText(getString(R.string.invalid_action));
                return;
        }
        showResult(r);
        // Optionally refresh the spinners with the latest categories
        if (r.ok) dismiss();
    }

    private void showResult(CategoryManager.Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.ok ? "✅ " : "❌ ").append(r.message);
        if (r.previewCount > 0 && r.changedPaths != null && !r.changedPaths.isEmpty()) {
            sb.append("\n\n").append(getString(R.string.items)).append(": ").append(r.previewCount);
            for (String p : r.changedPaths) {
                sb.append("\n• ").append(p);
            }
        }
        previewOutput.setText(sb.toString());
    }

    private static String safeText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    // Minimal helper to avoid verbose listener code
    private static class SimpleOnItemSelected implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable run;
        SimpleOnItemSelected(Runnable r) { run = r; }
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { run.run(); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
    }
}
