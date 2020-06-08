package com.ubiquid.ubiquidtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

/**
 * Activty used to display the results from the normal mode obtained from the main activity
 */
public class DisplayCodeValuesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_code_values);

        String code_content = getIntent().getStringExtra(MainActivity.CODE_CONTENT);
        TextView tv = (TextView) findViewById(R.id.textViewValue);
        tv.setText(code_content);
    }
}