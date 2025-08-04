package net.gsantner.markor.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import java.util.Locale;

public class CalendarActivity extends Activity {

    private EditText inputNoteTitle;
    private Button btnReset, btnSubmit;
    private Spinner spinnerCategory;
    private ImageButton btnBrowser;
    private CalendarView calendarView;
    private Calendar selectedCalendarDate = Calendar.getInstance();  // Track selected calendar date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        inputNoteTitle = findViewById(R.id.input_note_title);
        btnReset = findViewById(R.id.btn_reset);
        btnSubmit = findViewById(R.id.btn_submit);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnBrowser = findViewById(R.id.btn_browser);
        calendarView = findViewById(R.id.calendarView);

        // Spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.category_list,
                R.layout.spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setSelection(0);

        // Calendar selection listener
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedCalendarDate.set(Calendar.YEAR, year);
            selectedCalendarDate.set(Calendar.MONTH, month);
            selectedCalendarDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        });

        // Reset button logic
        btnReset.setOnClickListener(v -> {
            inputNoteTitle.setText("");
            spinnerCategory.setSelection(0);
            calendarView.setDate(System.currentTimeMillis(), false, true);
            selectedCalendarDate.setTimeInMillis(System.currentTimeMillis());
            Toast.makeText(CalendarActivity.this, "Reset", Toast.LENGTH_SHORT).show();
        });

        // Submit button logic
        btnSubmit.setOnClickListener(v -> createNoteFileAndSeedContent());

        // Browser icon (not implemented)
        btnBrowser.setOnClickListener(v -> Toast.makeText(CalendarActivity.this, "File browser not yet implemented", Toast.LENGTH_SHORT).show());
    }

    private void createNoteFileAndSeedContent() {
        String title = inputNoteTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a note title.", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = spinnerCategory.getSelectedItem().toString();

        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM_MMMM", Locale.getDefault());
        SimpleDateFormat timestampFormat = new SimpleDateFormat("EEE. MMMM dd yyyy | h:mm a", Locale.getDefault());

        String year = yearFormat.format(selectedCalendarDate.getTime());
        String month = monthFormat.format(selectedCalendarDate.getTime());
        String timestamp = timestampFormat.format(selectedCalendarDate.getTime());

        File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "markor default");
        File yearDir = new File(root, year);
        File monthDir = new File(yearDir, month);
        File categoryDir = new File(monthDir, category);

        if (!categoryDir.exists()) {
            boolean created = categoryDir.mkdirs();
            if (!created) {
                Toast.makeText(this, "Failed to create folders.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String safeTitle = title.replaceAll("[\\/:*?\"<>|]", "_");
        File noteFile = new File(categoryDir, safeTitle + ".md");

        if (noteFile.exists()) {
            Toast.makeText(this, "Note already exists.", Toast.LENGTH_SHORT).show();
            return;
        }

        try (FileWriter writer = new FileWriter(noteFile)) {
            writer.write(timestamp + "\n\n");
            Toast.makeText(this, "Note created: " + noteFile.getName(), Toast.LENGTH_SHORT).show();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", noteFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "text/markdown");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivity(intent);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to create note.", Toast.LENGTH_SHORT).show();
        }
    }
}
