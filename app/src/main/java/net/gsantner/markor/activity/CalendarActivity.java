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

public class CalendarActivity extends Activity {

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

        // Spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.category_list,
                R.layout.spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setSelection(0); // Default to "General"

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

        // Browser button opens folder tree with selected values
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

    // (Unchanged) createNoteFileAndSeedContent() remains the same
}
