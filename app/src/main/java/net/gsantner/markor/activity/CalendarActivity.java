package net.gsantner.markor.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;

import net.gsantner.markor.R;
import net.gsantner.markor.model.CreationIndex;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private EditText inputNoteTitle;
    private Button btnReset, btnSubmit;
    private Spinner spinnerCategory;
    private ImageButton btnBrowser;
    private CalendarView calendarView;
    private Calendar selectedCalendarDate = Calendar.getInstance(); // Default to today

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // Initialize views
        inputNoteTitle = findViewById(R.id.input_note_title);
        btnReset = findViewById(R.id.btn_reset);
        btnSubmit = findViewById(R.id.btn_submit);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnBrowser = findViewById(R.id.btn_browser);
        calendarView = findViewById(R.id.calendarView);

        // Header/back wiring (static header)
        View headerTap = findViewById(R.id.header_click_target);
        if (headerTap != null) headerTap.setOnClickListener(v -> handleBackFromCalendar());
        ImageButton backBtnHeader = findViewById(R.id.btn_back);
        if (backBtnHeader != null) backBtnHeader.setOnClickListener(v -> handleBackFromCalendar());
        TextView title = findViewById(R.id.title_text);
        if (title != null) title.setText("› Calendar");

        // NEW: Gear button in the top header opens Category Editor
        ImageButton gear = findViewById(R.id.btn_manage_categories_toolbar);
        if (gear != null) {
            gear.setOnClickListener(v -> openManageCategoriesDialog());
        }

        // Spinner setup — keep it pure and stable (no FS scan here)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.category_list, R.layout.spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setSelection(0); // Default to first entry

        // Optional: long-press Browser button as a hidden shortcut to the Category Editor
        btnBrowser.setOnLongClickListener(v -> {
            openManageCategoriesDialog();
            Toast.makeText(this, "Manage categories…", Toast.LENGTH_SHORT).show();
            return true;
        });

        // Calendar listener
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedCalendarDate.set(Calendar.YEAR, year);
            selectedCalendarDate.set(Calendar.MONTH, month);
            selectedCalendarDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        });

        // Reset button
        btnReset.setOnClickListener(v -> {
            inputNoteTitle.setText("");
            spinnerCategory.setSelection(0);
            calendarView.setDate(System.currentTimeMillis(), false, true);
            selectedCalendarDate.setTimeInMillis(System.currentTimeMillis());
            Toast.makeText(CalendarActivity.this, "Reset", Toast.LENGTH_SHORT).show();
        });

        // Submit button
        btnSubmit.setOnClickListener(v -> createNoteFileAndSeedContent());

        // Browser button opens folder tree with selected values (short press)
        btnBrowser.setOnClickListener(v -> {
            String selectedCategory = spinnerCategory.getSelectedItem().toString();
            // Get selected month as "MM_MMMM" (e.g. "08_August")
            SimpleDateFormat monthFormat = new SimpleDateFormat("MM_MMMM", Locale.getDefault());
            String selectedMonth = monthFormat.format(selectedCalendarDate.getTime());

            Intent intent = new Intent(CalendarActivity.this, FolderBrowserActivity.class);
            intent.putExtra("selectedCategory", selectedCategory);
            intent.putExtra("selectedMonth", selectedMonth);
            startActivity(intent);
        });
    }

    // Smart back:
    // - If Calendar sits on a back stack (opened from inside Markor), finish().
    // - If it's task root (e.g., launched fresh), go to MainActivity.
    private void handleBackFromCalendar() {
        if (!isTaskRoot()) {
            finish();
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("open_tab", "files"); // harmless hint
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        handleBackFromCalendar();
    }

    private void openManageCategoriesDialog() {
        ManageCategoriesDialogFragment.newInstance()
                .show(getSupportFragmentManager(), "manage_categories");
    }

    private void createNoteFileAndSeedContent() {
        String title = inputNoteTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a note title.", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = spinnerCategory.getSelectedItem().toString();

        // Use the selected date
        Calendar calendar = selectedCalendarDate;
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM_MMMM", Locale.getDefault());
        SimpleDateFormat timestampFormat = new SimpleDateFormat("EEE. MMMM dd yyyy | h:mm a", Locale.getDefault());

        String year = yearFormat.format(calendar.getTime());
        String month = monthFormat.format(calendar.getTime());
        String timestamp = timestampFormat.format(calendar.getTime());

        File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "markor default");
        File categoryDir = new File(new File(root, year + "/" + month), category);

        if (!categoryDir.exists() && !categoryDir.mkdirs()) {
            Toast.makeText(this, "Failed to create folders.", Toast.LENGTH_SHORT).show();
            return;
        }

        String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
        File noteFile = new File(categoryDir, safeTitle + ".md");

        if (noteFile.exists()) {
            Toast.makeText(this, "Note already exists.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Persist true creation time in the index (epoch millis)
        long createdMillis = calendar.getTimeInMillis();
        CreationIndex cidx = new CreationIndex(this);
        cidx.put(noteFile, createdMillis);
        cidx.save();

        try (FileWriter writer = new FileWriter(noteFile)) {
            writer.write(timestamp + "\n\n"); // human-readable first line
            Toast.makeText(this, "Note created: " + noteFile.getName(), Toast.LENGTH_SHORT).show();

            // Open in Markor via FileProvider
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", noteFile);
            Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setDataAndType(uri, "text/markdown");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.setPackage("net.gsantner.markor");

            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to open note in Markor editor.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to create note.", Toast.LENGTH_SHORT).show();
        }
    }
}
