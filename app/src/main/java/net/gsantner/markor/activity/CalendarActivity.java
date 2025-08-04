package net.gsantner.markor.activity;

import android.app.Activity;
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
import android.widget.Toast;

import androidx.core.content.FileProvider;

import net.gsantner.markor.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.os.StrictMode;


public class CalendarActivity extends Activity {

    private EditText inputNoteTitle;
    private Button btnReset, btnSubmit;
    private Spinner spinnerCategory;
    private ImageButton btnBrowser;
    private CalendarView calendarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // 1. Initialize views
        inputNoteTitle = findViewById(R.id.input_note_title);
        btnReset = findViewById(R.id.btn_reset);
        btnSubmit = findViewById(R.id.btn_submit);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnBrowser = findViewById(R.id.btn_browser);
        calendarView = findViewById(R.id.calendarView);

        // 2. Set up spinner with predefined categories
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.category_list,
                R.layout.spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setSelection(0); // Default to "General"

        // 3. Reset button: clear input, reset spinner and calendar
        btnReset.setOnClickListener(v -> {
            inputNoteTitle.setText("");  // Clear input field
            spinnerCategory.setSelection(0);  // Reset spinner
            calendarView.setDate(System.currentTimeMillis(), false, true);  // Reset calendar view
            Toast.makeText(CalendarActivity.this, "Reset", Toast.LENGTH_SHORT).show();
        });

        // 4. Submit button: triggers note creation
        btnSubmit.setOnClickListener(v -> createNoteFileAndSeedContent());

        // 5. File browser button: stub for now
        btnBrowser.setOnClickListener(v ->
                Toast.makeText(CalendarActivity.this, "File browser not yet implemented", Toast.LENGTH_SHORT).show()
        );
    }

    // Method to create folder structure and new note file, and seed date stamp
    private void createNoteFileAndSeedContent() {
        String title = inputNoteTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a note title.", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = spinnerCategory.getSelectedItem().toString();

        // 📅 Use selected date from calendar
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.setTimeInMillis(calendarView.getDate());

        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM_MMMM", Locale.getDefault());
        SimpleDateFormat timestampFormat = new SimpleDateFormat("EEE. MMMM dd yyyy | h:mm a", Locale.getDefault());

        String year = yearFormat.format(selectedDate.getTime());
        String month = monthFormat.format(selectedDate.getTime());
        String timestamp = timestampFormat.format(selectedDate.getTime());

        // 📂 Target root directory: /Documents/markor default/
        File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "markor default");
        File yearDir = new File(root, year);
        File monthDir = new File(yearDir, month);
        File categoryDir = new File(monthDir, category);

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

        try (FileWriter writer = new FileWriter(noteFile)) {
            writer.write(timestamp + "\n\n");
        } catch (IOException e) {
            Toast.makeText(this, "Failed to write note.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }

        Toast.makeText(this, "Note created: " + noteFile.getName(), Toast.LENGTH_SHORT).show();

        // 🚀 Open directly in Markor (editing mode)
        try {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(noteFile), "text/plain");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage("net.gsantner.markor");

            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open note in Markor.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

}
