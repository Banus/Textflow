package com.example.googlemap;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

class CalendarUtility {

    static ArrayList<HashMap<String, String>> readCalendarEvent(
            Context context, Date start, Date end) {
        Uri CALENDAR_URI = CalendarContract.Events.CONTENT_URI;
        ArrayList<HashMap<String,String>> result = new ArrayList<>();

        ArrayList<String> selections = new ArrayList<>();
        if (start != null) {
            selections.add("dtstart > " + start.getTime());
        }
        if (end != null) {
            selections.add("dtstart < " + end.getTime());
        }
        String selection = (selections.size() > 0) ? TextUtils.join(" AND ", selections) : null;

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            return result;
        }
        Cursor cursor = context.getContentResolver()
                .query(CALENDAR_URI, new String[]{"calendar_id", "title", "description",
                                "dtstart", "dtend", "eventLocation"},
                        selection, null, "dtstart");
        if (cursor == null) {
            return new ArrayList<>();
        }
        cursor.moveToFirst();
        // fetching calendars name
        int n_names = cursor.getCount();

        // fetching calendars id
        for (int i = 0; i < n_names; i++) {
            if (cursor.getInt(0) != 2) {    // Holidays in United States
                HashMap<String, String> event = new HashMap<>();
                event.put("calendar_id", cursor.getString(0));
                event.put("name", cursor.getString(1));
                event.put("description", cursor.getString(2));
                event.put("start", getDate(Long.parseLong(cursor.getString(3))));
                event.put("end", getDate(Long.parseLong(cursor.getString(4))));
                event.put("location", cursor.getString(5));
                result.add(event);
            }
            cursor.moveToNext();
        }
        cursor.close();
        return result;
    }

    private static String getDate(long milliSeconds) {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "MM/dd/yyyy hh:mm:ss a");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }
}