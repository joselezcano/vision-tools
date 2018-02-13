package com.example.visiontools;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button textDetectionButton = findViewById(R.id.textDetectionButton);
        textDetectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), TextDetectionActivity.class);
                startActivity(intent);
            }
        });

        Button labelDetectionButton = findViewById(R.id.labelDetectionButton);
        labelDetectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LabelDetectionActivity.class);
                startActivity(intent);
            }
        });

        Button faceDetectionButton = findViewById(R.id.faceDetectionButton);
        faceDetectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), FaceDetectionActivity.class);
                startActivity(intent);
            }
        });
    }
}
