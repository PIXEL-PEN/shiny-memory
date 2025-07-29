package net.gsantner.markor.activity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import net.gsantner.markor.R;

public class CalendarActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        Toast.makeText(this, "Calendar Activity Launched", Toast.LENGTH_SHORT).show();
    }
}
