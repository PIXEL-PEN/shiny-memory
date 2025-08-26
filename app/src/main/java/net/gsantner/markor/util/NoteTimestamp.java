package net.gsantner.markor.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class NoteTimestamp {
    private NoteTimestamp() {}

    // Use this if you have epoch millis from CalendarView.getDate()
    public static String fromSelectedEpochMillis(long selectedDayUtcMillis) {
        TimeZone tz = TimeZone.getDefault();

        // Break out the picked local Y/M/D
        Calendar picked = Calendar.getInstance(tz);
        picked.setTimeInMillis(selectedDayUtcMillis);
        int y = picked.get(Calendar.YEAR);
        int m = picked.get(Calendar.MONTH); // 0-based
        int d = picked.get(Calendar.DAY_OF_MONTH);

        // Current local time (HH:mm), seconds=0
        Calendar now = Calendar.getInstance(tz);

        // Combine picked date + current time (local)
        Calendar combined = Calendar.getInstance(tz);
        combined.clear();
        combined.set(y, m, d, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), 0);

        return format(combined, tz);
    }

    // Or use this if you keep a Calendar for the picked day already
    public static String fromCalendar(Calendar cal) {
        TimeZone tz = TimeZone.getDefault();
        Calendar now = Calendar.getInstance(tz);

        Calendar combined = Calendar.getInstance(tz);
        combined.clear();
        combined.set(cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                0);

        return format(combined, tz);
    }

    private static String format(Calendar c, TimeZone tz) {
        SimpleDateFormat dfDate = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        SimpleDateFormat dfTime = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
        dfDate.setTimeZone(tz);
        dfTime.setTimeZone(tz);
        String dow = abbrevDOW(c.get(Calendar.DAY_OF_WEEK));
        return dow + " " + dfDate.format(c.getTime()) + " | " + dfTime.format(c.getTime()).toLowerCase(Locale.ENGLISH);
    }

    private static String abbrevDOW(int dow) {
        switch (dow) {
            case Calendar.MONDAY: return "Mon.";
            case Calendar.TUESDAY: return "Tues.";
            case Calendar.WEDNESDAY: return "Wed.";
            case Calendar.THURSDAY: return "Thurs.";
            case Calendar.FRIDAY: return "Fri.";
            case Calendar.SATURDAY: return "Sat.";
            case Calendar.SUNDAY: return "Sun.";
            default: return "";
        }
    }
}
