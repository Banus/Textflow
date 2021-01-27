package com.example.googlemap;

import android.os.Bundle;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

public class MainActivity extends FragmentActivity {
    TapController tap;
    Talker talker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        talker = new Talker(this);
        tap = new TapController(this, talker);

        ((Switch) findViewById(R.id.fullflow)).setOnCheckedChangeListener(
                (buttonView, isChecked) -> talker.USE_FLOW = isChecked);

        findViewById(R.id.sc1_t1).setOnClickListener(
                v -> talker.run_textflow("scenario 1", 0));
        findViewById(R.id.sc1_t2).setOnClickListener(
                v -> talker.run_textflow("scenario 1", 1));
        findViewById(R.id.sc1_t3).setOnClickListener(
                v -> talker.run_textflow("scenario 1", 2));
        findViewById(R.id.sc1_t4).setOnClickListener(
                v -> talker.run_textflow("scenario 1", 3));
        findViewById(R.id.sc1_t5).setOnClickListener(
                v -> talker.run_textflow("scenario 1", 4));
        findViewById(R.id.sc2_t1).setOnClickListener(
                v -> talker.run_textflow("scenario 2", 0));
        findViewById(R.id.sc2_t2).setOnClickListener(
                v -> talker.run_textflow("scenario 2", 1));
        findViewById(R.id.sc2_t3).setOnClickListener(
                v -> talker.run_textflow("scenario 2", 2));
        findViewById(R.id.sc2_t4).setOnClickListener(
                v -> talker.run_textflow("scenario 2", 3));
        findViewById(R.id.sc2_t5).setOnClickListener(
                v -> talker.run_textflow("scenario 2", 4));
        findViewById(R.id.sc3_t1).setOnClickListener(
                v -> talker.run_textflow("scenario 3", 0));
        findViewById(R.id.sc3_t2).setOnClickListener(
                v -> talker.run_textflow("scenario 3", 1));
        findViewById(R.id.sc3_t3).setOnClickListener(
                v -> talker.run_textflow("scenario 3", 2));
        findViewById(R.id.sc3_t4).setOnClickListener(
                v -> talker.run_textflow("scenario 3", 3));
        findViewById(R.id.sc3_t5).setOnClickListener(
                v -> talker.run_textflow("scenario 3", 4));
    }
}
