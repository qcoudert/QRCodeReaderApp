package com.ubiquid.ubiquidtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Activity used to display the results from the evaluation mode obtained from the main activity
 */
public class EvaluationDisplayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evaluation_display);

        ListView listView = (ListView) findViewById(R.id.codeValuesListView);
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getIntent().getStringArrayExtra(MainActivity.CODE_CONTENT)));

        TextView tv = (TextView) findViewById(R.id.qrCodeNumberTextView);
        tv.setText(Integer.toString(getIntent().getIntExtra(MainActivity.CODE_QR_NUMBER, 0)));

        tv = (TextView) findViewById(R.id.codeNumberTextView);
        tv.setText(Integer.toString(getIntent().getStringArrayExtra(MainActivity.CODE_CONTENT).length));

        tv = (TextView) findViewById(R.id.dataMatrixNumber);
        tv.setText(Integer.toString(getIntent().getIntExtra(MainActivity.CODE_DATA_MATRIX_NUMBER, 0)));
    }
}