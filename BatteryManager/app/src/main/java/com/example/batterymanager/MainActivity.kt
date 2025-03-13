package com.example.batterymanager;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView batteryPercentage = findViewById(R.id.batteryPercentage);
        TextView batteryStatus = findViewById(R.id.batteryStatus);
        TextView remainingTime = findViewById(R.id.remainingTime);
        SeekBar batteryThreshold = findViewById(R.id.batteryThreshold);
        Button selectSound = findViewById(R.id.selectSound);
    }
}